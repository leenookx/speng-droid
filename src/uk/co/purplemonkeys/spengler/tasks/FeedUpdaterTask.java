package uk.co.purplemonkeys.spengler.tasks;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import uk.co.purplemonkeys.spengler.providers.Article.Articles;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class FeedUpdaterTask extends AsyncTask<Void, Void, Object> 
{
	private final static String TAG = "ProjectGrabberTask";
    private Context _appContext;
    
    public FeedUpdaterTask(Context c)
    {
    	_appContext = c;
    }
   
    @Override
    public Object doInBackground(Void... unused_params) 
    {
    	Object result = new Object();
    	
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( _appContext );
    	String url = prefs.getString("pref_url", "http://localhost:3000");
    	String auth_code = prefs.getString("pref_auth_code", "abc123");
    	
    	String rss = url + "/user/feed/" + auth_code;
    	
    	try
    	{
    		// Clear out all of the existing articles.
    		_appContext.getContentResolver().delete(Articles.CONTENT_URI, null, null);
    		
    		URL feedUrl = new URL(rss);
    		SyndFeedInput input = new SyndFeedInput();
    		SyndFeed feed = input.build(new XmlReader(feedUrl));
    		List<SyndEntry> entries = feed.getEntries();
    		Iterator<SyndEntry> iterator = entries.listIterator();
    		int i = 0;
    		while (iterator.hasNext())
    		{
    			SyndEntry entry = (SyndEntry)iterator.next();
    			
				ContentValues cv = new ContentValues();
				cv.put(Articles._ID, i++);
				cv.put(Articles.TITLE, entry.getTitle());
				cv.put(Articles.PAGE_URL, entry.getLink());
				
				_appContext.getContentResolver().insert(Articles.CONTENT_URI, cv);
    		}
    	}
    	catch (MalformedURLException e)
    	{
    		result = null;
    		Log.e(TAG, e.toString());
    	}
    	catch (IllegalArgumentException e)
    	{
    		result = null;
    		Log.e(TAG, e.toString());
    	}
    	catch (FeedException e)
    	{
    		result = null;
    		Log.e(TAG, e.toString());
    	}
    	catch (IOException e)
    	{
    		result = null;
    		Log.e(TAG, e.toString());
    	}
    	
    	return result;
    }
    
    @Override
    public void onPreExecute() 
    {
    }
   
    @Override
    public void onPostExecute(Object result) 
    {
    	if (result != null)
    	{
    	}
    	else
    	{
    	}
    }        
}
