/*
 * $Id: Entry.java 26 2007-12-06 03:06:54Z azurite@telusplanet.net $
 *
 * Copyright (C) 2007 James Gilbertson <azurite@telusplanet.net>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */
package uk.co.purplemonkeys.spengler.feeds;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Entry extends DAO implements Afr.EntriesColumns, Parcelable {
	protected long feed = -1;
	protected String uri;
	protected String title;
	protected String author;
	protected Date date;
	protected URI link;
	protected String content;
	protected String type;
	protected boolean read;
	
	public static final Parcelable.Creator<Entry> CREATOR = new Parcelable.Creator<Entry>() {
		public Entry createFromParcel(Parcel source) {
			Entry item = new Entry(null, source.readLong());
			try {
				item.feed = source.readLong();
				item.uri = source.readString();
				item.title = source.readString();
				item.author = source.readString();
				item.date = new Date(source.readLong());
				item.link = new URI(source.readString());
				item.content = source.readString();
				item.type = source.readString();
				item.read = source.readByte() == 1;
			} catch (URISyntaxException e) {
				// this should never happen
				Log.e("AFR", "Item.createFromParcel: error parsing the link or URI from parcel", e);
			}
			return item;
		}
		
		public Entry[] newArray(int size) {
			return new Entry[size];
		}
	};
	
	public Entry(ContentResolver contentResolver) {
		super(contentResolver);
	}
	
	public Entry(ContentResolver contentResolver, long id) {
		super(contentResolver, id);
	}
	
	public void writeToParcel(Parcel parcel) {
		parcel.writeLong(id);
		parcel.writeString(uri.toString());
		parcel.writeString(title);
		parcel.writeString(author);
		parcel.writeLong(date.getTime());
		parcel.writeString(link.toString());
		parcel.writeString(content);
		parcel.writeString(type);
		parcel.writeByte(read ? (byte) 1 : (byte) 0);
	}
	
	@Override
	protected ContentURI getContentURI() {
		return Afr.Entries.CONTENT_URI;
	}
	
	public boolean loadByUri() {
		if (uri != null) {
			String path = null;
			try {
				path = URLEncoder.encode(uri, "utf-8");
			} catch (UnsupportedEncodingException e) {
				// should never happen
			}
			
			return load(Afr.Entries.CONTENT_FILTER_URI_URI.addPath(path));
		}
		
		return false;
	}
	
	@Override
	protected void doLoad(Cursor cursor, boolean subset) {
		try {
			if (subset) {
				if (cursor.getColumnIndex(FEED) != -1) {
					feed = cursor.getLong(cursor.getColumnIndex(FEED));
				}
				if (cursor.getColumnIndex(URI) != -1) {
					uri = cursor.getString(cursor.getColumnIndex(URI));
				}
				if (cursor.getColumnIndex(TITLE) != -1) {
					title = cursor.getString(cursor.getColumnIndex(TITLE));
				}
				if (cursor.getColumnIndex(AUTHOR) != -1) {
					author = cursor.getString(cursor.getColumnIndex(AUTHOR));
				}
				if (cursor.getColumnIndex(DATE) != -1) {
					date = new Date(cursor.getLong(cursor.getColumnIndex(DATE)));
				}
				if (cursor.getColumnIndex(LINK) != -1) {
					link = new URI(cursor.getString(cursor.getColumnIndex(LINK)));
				}
				if (cursor.getColumnIndex(CONTENT) != -1) {
					content = cursor.getString(cursor.getColumnIndex(CONTENT));
				}
				if (cursor.getColumnIndex(TYPE) != -1) {
					type = cursor.getString(cursor.getColumnIndex(TYPE));
				}
				if (cursor.getColumnIndex(READ) != -1) {
					read = cursor.getInt(cursor.getColumnIndex(READ)) == 1;
				}
			} else {
				feed = cursor.getLong(1);
				uri = cursor.getString(2);
				title = cursor.getString(3);
				author = cursor.getString(4);
				date = new Date(cursor.getLong(5));
				link = new URI(cursor.getString(6));
				content = cursor.getString(7);
				type = cursor.getString(8);
				read = cursor.getInt(9) == 1;
			}
		} catch (URISyntaxException e) {
			// this should never happen, since they are validated before they get into the DB
			Log.e("AFR", "Item.doLoad: error loading the link or URI field from database", e);
		}
	}
	
	@Override
	protected boolean doSaveOrUpdate(ContentValues values) {
		if (feed == -1 || 
			uri == null ||
			author == null || title == null || date == null || 
			content == null || type == null) {
			return false;
		}
		
		values.put(FEED, feed);
		values.put(URI, uri.toString());
		values.put(TITLE, title);
		values.put(AUTHOR, author);
		values.put(DATE, date.getTime());
		values.put(LINK, link.toString());
		values.put(CONTENT, content);
		values.put(TYPE, type);
		values.put(READ, read);
		
		return true;
	}

	public Feed getFeed() {
		Feed feed = new Feed(contentResolver);
		feed.setId(this.feed);
		feed.load();
		return feed;
	}
	
	public void setFeed(Feed feed) {
		this.feed = feed.getId();
	}
	
	public long getFeedId() {
		return feed;
	}

	public void setFeedId(long feed) {
		this.feed = feed;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public URI getLink() {
		return link;
	}

	public void setLink(URI link) {
		this.link = link;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}
}
