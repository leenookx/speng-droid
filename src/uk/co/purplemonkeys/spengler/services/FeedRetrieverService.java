package uk.co.purplemonkeys.spengler.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;

import uk.co.purplemonkeys.spengler.R;
import uk.co.purplemonkeys.spengler.feeds.Feed;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.util.Log;

public class FeedRetrieverService extends Service {
	private BlockingQueue<Feed> queue;
	private NotificationManager notificationManager;
	private Thread thread;
	
	private final IFeedRetrieverService.Stub binder = new IFeedRetrieverService.Stub(){
		public void retrieveURI(String uri) throws DeadObjectException {
			Feed feed = new Feed(getContentResolver());
			try {
				feed.setUri(new URI(uri));
			} catch (URISyntaxException e) {
				Log.e("AFR", "IFeedRetrieverService.Stub.retrieveFeed(String): failed to parse '" + uri + "'", e);
				displayErrorNotification(R.string.feed_retriever_err_unspecified, 
						feed.getName() != null ? feed.getName() : feed.getUri().toString());
				return;
			}
			
			queue.add(feed);
		}
		
		public void retrieveFeed(long id) throws DeadObjectException {
			Feed feed = new Feed(getContentResolver(), id);
			if (!feed.load()) {
				Log.e("AFR", "IFeedRetrieverService.Stub.retrieveFeed(long): failed to load feed with id=" + id);
				displayErrorNotification(R.string.feed_retriever_err_unspecified_no_details);
				return;
			}
			
			queue.add(feed);
		}
		
		public void retrieveAllFeeds() throws DeadObjectException {
			Cursor c = getContentResolver().query(Afr.Feeds.CONTENT_URI, 
					new String[] { Afr.Feeds._ID }, 
					null, null, 
					Afr.Feeds.DEFAULT_SORT_ORDER); 
			while (c.next()) {
				retrieveFeed(c.getLong(0));
			}
			c.close();
		}
	};
	
	private final Runnable retriever = new Runnable() {		
		Feed feed;
		File file;
		String contentType;
		SyndFeed parsedFeed;
		
		public void run() {
			try {
				while (true) {
					feed = queue.take();
					
					// show a notification
					Notification notification = displayStatusNotification(feed);
					
					if (!download(notification) || 
						!parse(notification)) {
						notificationManager.cancel(R.string.feed_retriever_notification);
						continue;
					}
					
					save(notification);
					notificationManager.cancel(R.string.feed_retriever_notification);
				}
			} catch (InterruptedException e) {
				// if we are interrupted, we are going away, so do nothing
			}
		}
		
		boolean download(Notification notification) {
			// open a HTTP connection and get the headers
			System.setProperty("httpclient.useragent", getString(R.string.app_user_agent_string));
			HttpClient client = new HttpClient();
			// set a 10 second timeout
			client.getParams().setParameter("http.socket.timeout", new Integer(10000));
			GetMethod method = new GetMethod(feed.getUri().toString());
	        
			Log.i("AFR", "FeedRetrieverService.retriever.download(): retrieving feed from: " + feed.getUri().toString());
			
			try {
				method.setFollowRedirects(true);
				method.addRequestHeader("Accept-Encoding", "gzip");
				
				// if we have the data for conditional GET, use it
				if (feed.getEtag() != null) {
					method.addRequestHeader("If-None-Match", feed.getEtag());
				}
				if (feed.getLastModified() != null) {
					method.addRequestHeader("If-Modified-Since", feed.getLastModified());
				}
				
				// make the GET request
				if (!handleHttpStatus(client.executeMethod(method))) {
					return false;
				}
				
				// get the headers we are interested in
		        Header header = method.getResponseHeader("Last-Modified");
		        if (header != null) {
		        	feed.setLastModified(header.getValue());
		        }
		        header = method.getResponseHeader("ETag");
		        if (header != null) {
		        	feed.setEtag(header.getValue());
		        }
		        header = method.getResponseHeader("Content-Type");
				if (header != null) {
					contentType = header.getValue();
				}
				
				// get the response stream
				InputStream in = null;
				if (method.getResponseHeader("Content-Encoding") != null && 
					"gzip".equalsIgnoreCase(method.getResponseHeader("Content-Encoding").getValue())) {		
				    in = new GZIPInputStream(method.getResponseBodyAsStream());
				} else {
				    in = method.getResponseBodyAsStream();
				}
				
				// download the feed
				file = File.createTempFile("afr-feed-", ".xml");
				FileOutputStream out = new FileOutputStream(file);
		        
				long length = method.getResponseContentLength();

				if (length != -1) {
					String details = getText(R.string.feed_retriever_notification_dl).toString();
					Log.i("AFR", "FeedRetrieverService.retriever.download(): Content-Length = " + length);
					
					// use a 1 KiB buffer so we can update progress
					byte[] buffer = new byte[1024];
					float total = (float) length, readSoFar = 0f;
					for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
						out.write(buffer, 0, read);
						readSoFar += read;
						notification.statusBarBalloonText = String.format(details, 
								feed.getName() != null ? feed.getName() : feed.getUri().toString(),
								readSoFar/total * 100f).toString();
					}
				} else {
					Log.i("AFR", "FeedRetrieverService.retriever.download(): Content-Length not given");
					// use a 8 KiB buffer
					byte[] buffer = new byte[1024*8];
					for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
						out.write(buffer, 0, read);
					}
				}
				
				in.close();
				out.close();
			} catch (UnknownHostException e) {
				Log.e("AFR", "FeedRetrieverService.retriever.download(): failed to retrieve feed: host not found", e);
				String detail = feed.getUri().getScheme() + "://" + feed.getUri().getHost();
				displayErrorNotification(R.string.feed_retriever_err_unknown_host, detail);
				return false;
			} catch (SocketTimeoutException e) {
				Log.e("AFR", "FeedRetrieverService.retriever.download(): failed to retrieve feed: timed out", e);
				String detail = feed.getUri().getScheme() + "://" + feed.getUri().getHost();
				displayErrorNotification(R.string.feed_retriever_err_timed_out, detail);
				return false;
			} catch (IOException e) {
				Log.e("AFR", "FeedRetrieverService.retriever.download(): failed to retrieve feed: I/O error", e);
				displayErrorNotification(R.string.feed_retriever_err_unspecified, 
						feed.getName() != null ? feed.getName() : feed.getUri().toString());
				return false;
			} finally {				
				method.releaseConnection();
			}
			
			return true;
		}
		
		boolean handleHttpStatus(int statusCode) {
			String detail = feed.getName() != null ? feed.getName() : feed.getUri().toString();
			int error = 0;
			
			if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
				Log.i("AFR", "FeedRetrieverService.retriever.download(): [" + statusCode + "] feed unchanged");
				// update the feed and quit
				feed.setLastChecked(new Date(System.currentTimeMillis()));
				feed.update();
				return false;
			} else if (statusCode == HttpStatus.SC_FORBIDDEN) {
				Log.e("AFR", "FeedRetrieverService.retriever.download(): [" + statusCode + "] authorization required");
				error = R.string.feed_retriever_err_auth_req;
				// TODO: figure out a way to ask for credentials & retry
			} else if (statusCode == HttpStatus.SC_NOT_FOUND || statusCode == HttpStatus.SC_GONE) {
				Log.e("AFR", "FeedRetrieverService.retriever.download(): [" + statusCode + "] not found");
				error = R.string.feed_retriever_err_not_found;
			} else if (statusCode >= 400 && statusCode < 500) {
				Log.e("AFR", "FeedRetrieverService.retriever.download(): [" + statusCode + "] client error");
				error = R.string.feed_retriever_err_client;
			} else if (statusCode >= 500 && statusCode < 600) {
				Log.e("AFR", "FeedRetrieverService.retriever.download(): [" + statusCode + "] server error");
				detail = feed.getUri().getScheme() + "://" + feed.getUri().getHost();
				error = R.string.feed_retriever_err_server;
			}
			
			if (error != 0) {
				displayErrorNotification(error, detail);
				return false;
			}
			
			Log.i("AFR", "FeedRetrieverService.retriever.download(): [" + statusCode + "] continuing");
			return true;
		}
		
		boolean parse(Notification notification) {		
			try {
				FileInputStream stream = new FileInputStream(file);
				parsedFeed = new SyndFeedInput().build(new XmlReader(stream, contentType, true));
			} catch (Exception e) {
				Log.i("AFR", "FeedRetrieverService.retriever.parse(): error parsing feed", e);
				displayErrorNotification(R.string.feed_retriever_err_parse, feed.getName() != null ? feed.getName() : feed.getUri());
				return false;
			} finally {
				file.delete();
			}
			
			return true;
		}
		
		@SuppressWarnings("unchecked")
		void save(Notification notification) {
			feed.setName(parsedFeed.getTitle());
			try {
				feed.setLink(new URI(parsedFeed.getLink()));
			} catch (URISyntaxException e) {
				Log.e("AFR", "FeedRetrieverService.retriever.save(): failed to parse '" + parsedFeed.getLink() + "'", e);
				displayErrorNotification(R.string.feed_retriever_err_unspecified, 
						feed.getName() != null ? feed.getName() : feed.getUri().toString());
				return;
			}
			try {
				/* if the link and uri are the same, don't update the uri, because it
				 * might or might not be the URI we use to retrieve the feed. 
				 */
				if (parsedFeed.getUri() != null && !parsedFeed.getUri().equals(parsedFeed.getLink())) {
					feed.setUri(new URI(parsedFeed.getUri()));
				}
			} catch (URISyntaxException e) {
				Log.e("AFR", "FeedRetrieverService.retriever.save(): failed to parse '" + parsedFeed.getUri() + "'", e);
				displayErrorNotification(R.string.feed_retriever_err_unspecified, 
						feed.getName() != null ? feed.getName() : feed.getUri().toString());
				return;
			}
			feed.setLastChecked(new Date(System.currentTimeMillis()));
			
			feed.saveOrUpdate();

			ArrayList<ContentValues> entries = new ArrayList<ContentValues>(parsedFeed.getEntries().size());
			for (SyndEntry entry : (List<SyndEntry>) parsedFeed.getEntries()) {
				ContentValues values = processEntry(entry);
				if (values != null) {
					entries.add(values);
				}
			}
			
			ContentValues[] bulkValues = entries.toArray(new ContentValues[entries.size()]);
			getContentResolver().bulkInsert(Afr.Entries.CONTENT_URI, bulkValues);
		}
		
		@SuppressWarnings("unchecked")
		ContentValues processEntry(SyndEntry parsedEntry) {
			Entry entry = new Entry(getContentResolver());

			// check if this item has already been retrieved (and stop if it has)
			entry.setUri(parsedEntry.getUri());
			if (entry.loadByUri()) {
				return null;
			}
			
			entry.setFeed(feed);
			// get the author (item author > feed author > item contributers > feed contributers > feed title)
			String author = parsedEntry.getAuthor();
			if (author == null || author.length() == 0) {
				if (parsedFeed.getAuthor() != null && parsedFeed.getAuthor().length() != 0) {
					author = parsedFeed.getAuthor();
				} else if (parsedEntry.getContributors() != null && !parsedEntry.getContributors().isEmpty()) {
					author = ((SyndPerson) parsedEntry.getContributors().get(0)).getName();
				} else if (parsedFeed.getContributors() != null && !parsedFeed.getContributors().isEmpty()) {
					author = ((SyndPerson) parsedFeed.getContributors().get(0)).getName();
				} else {
					author = parsedFeed.getTitle();
				}
			}
			entry.setAuthor(author);
			entry.setTitle(parsedEntry.getTitle());
			if (parsedEntry.getPublishedDate() != null) {
				entry.setDate(parsedEntry.getPublishedDate());
			} else {
				entry.setDate(new Date(System.currentTimeMillis()));
			}
			try {
				entry.setLink(new URI(parsedEntry.getLink()));
			} catch (URISyntaxException e) {
				Log.e("AFR", "FeedRetrieverService.retriever.saveItem(): failed to parse '" + parsedEntry.getLink() + "'", e);
				displayErrorNotification(R.string.feed_retriever_err_unspecified, 
						feed.getName() != null ? feed.getName() : feed.getUri().toString());
				return null;
			}
			
			// get the content (prefer HTML over plain text)
			SyndContent bestContent = null;
			for (SyndContent content : (List<SyndContent>) parsedEntry.getContents()) {
				if (bestContent == null) {
					bestContent = content;
					continue;
				}
				
				if (content.getType() != null && bestContent.getType() == null) {
					bestContent = content;
					break;
				}
			}
			
			if (bestContent == null) {
				// no content found? get it from the description then
				bestContent = parsedEntry.getDescription();
			}
			
			if (bestContent == null) {
				// we're screwed. let's move on
				Log.e("AFR", "FeedRetrieverService.retriever.saveItem(): no item content found");
				displayErrorNotification(R.string.feed_retriever_err_unspecified, 
						feed.getName() != null ? feed.getName() : feed.getUri().toString());
				return null;
			}
			entry.setContent(bestContent.getValue());
			
			if (bestContent.getType() == "html") {
				entry.setType("text/html");
			} else {
				entry.setType("text/plain");
			}

			return entry.getContentValues();
		}
	};
	
	@Override
	public IBinder getBinder() {
		return binder;
	}
	
	@Override
	protected void onCreate() {
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		queue = new LinkedBlockingQueue<Feed>();
		
		thread = new Thread(null, retriever, "FeedRetrieverService worker");
		thread.setDaemon(true);
		thread.start();
		
		super.onCreate();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	Notification displayStatusNotification(Feed feed) {
		String text = String.format(getString(R.string.feed_retriever_notification), 
				feed.getName() != null ? feed.getName() : feed.getUri()).toString();
		Notification notification = new Notification(R.drawable.notification_afr, text, null, text, null);
		notificationManager.notify(R.string.feed_retriever_notification, notification);
		return notification;
	}
	
	void displayErrorNotification(int textResId, Object... params) {
		notificationManager.notifyWithText(
				textResId, 
				String.format(getString(textResId), params).toString(), 
				NotificationManager.LENGTH_SHORT, null);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
}