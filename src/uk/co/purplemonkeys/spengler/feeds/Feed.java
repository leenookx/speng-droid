/*
 * $Id: Feed.java 4 2007-12-03 04:41:22Z azurite@telusplanet.net $
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Feed extends DAO implements Afr.FeedsColumns, Parcelable {
	protected URI uri;
	protected String name;
	protected URI link;
	protected long expireAfter;
	protected Date lastChecked;
	protected String lastModified;
	protected String etag;
	
	public static final Parcelable.Creator<Feed> CREATOR = new Parcelable.Creator<Feed>() {
		public Feed createFromParcel(Parcel source) {
			Feed feed = new Feed(null, source.readLong());
			try {
				feed.uri = new URI(source.readString());
				feed.name = source.readString();
				feed.link = new URI(source.readString());
				feed.expireAfter = source.readLong();
				feed.lastChecked = new Date(source.readLong());
				feed.lastModified = source.readString();
				feed.etag = source.readString();
			} catch (URISyntaxException e) {
				// this should never happen
				Log.e("AFR", "Feed.createFromParcel: error parsing the link or URI from parcel", e);
			}
			return feed;
		}
		
		public Feed[] newArray(int size) {
			return new Feed[size];
		}
	};
	
	public Feed(ContentResolver contentResolver) {
		super(contentResolver);
	}
	
	public Feed(ContentResolver contentResolver, long id) {
		super(contentResolver, id);
	}
	
	public void writeToParcel(Parcel parcel) {
		parcel.writeLong(id);
		parcel.writeString(uri.toString());
		parcel.writeString(name);
		parcel.writeString(link.toString());
		parcel.writeLong(expireAfter);
		parcel.writeLong(lastChecked.getTime());
		parcel.writeString(lastModified);
		parcel.writeString(etag);
	}
	
	@Override
	protected ContentURI getContentURI() {
		return Afr.Feeds.CONTENT_URI;
	}
	
	@Override
	protected void doLoad(Cursor c, boolean subset) {
		try {
			if (subset) {
				if (c.getColumnIndex(URI) != -1) {
					uri = new URI(c.getString(c.getColumnIndex(URI)));
				}
				if (c.getColumnIndex(NAME) != -1) {
					name = c.getString(c.getColumnIndex(NAME));
				}
				if (c.getColumnIndex(LINK) != -1) {
					link = new URI(c.getString(c.getColumnIndex(LINK)));
				}
				if (c.getColumnIndex(EXPIRE_AFTER) != -1) {
					expireAfter = c.getLong(c.getColumnIndex(EXPIRE_AFTER));
				}
				if (c.getColumnIndex(LAST_CHECKED) != -1) {
					lastChecked = new Date(c.getLong(c.getColumnIndex(LAST_CHECKED)));
				}
				if (c.getColumnIndex(LAST_MODIFIED) != -1) {
					lastModified = c.getString(c.getColumnIndex(LAST_MODIFIED));
				}
				if (c.getColumnIndex(ETAG) != -1) {
					etag = c.getString(c.getColumnIndex(ETAG));
				}
			} else {
				uri = new URI(c.getString(1));
				name = c.getString(2);
				link = new URI(c.getString(3));
				expireAfter = c.getLong(4);
				lastChecked = new Date(c.getLong(5));
				lastModified = c.getString(6);
				etag = c.getString(7);
			}
		} catch (URISyntaxException e) {
			// this should never happen, since they are validated before they get into the DB
			Log.e("AFR", "Feed.doLoad: error loading the link or URI field from database", e);
		}
	}

	@Override
	protected boolean doSaveOrUpdate(ContentValues values) {
		if (uri == null || name == null || link == null) {
			return false;
		}
		
		values.put(URI, uri.toString());
		values.put(NAME, name);
		values.put(LINK, link.toString());
		values.put(EXPIRE_AFTER, expireAfter);
		values.put(LAST_CHECKED, lastChecked.getTime());
		values.put(LAST_MODIFIED, lastModified);
		values.put(ETAG, etag);
		
		return true;
	}
	
	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public URI getLink() {
		return link;
	}

	public void setLink(URI link) {
		this.link = link;
	}

	public long getExpireAfter() {
		return expireAfter;
	}

	public void setExpireAfter(long expireAfter) {
		this.expireAfter = expireAfter;
	}

	public Date getLastChecked() {
		return lastChecked;
	}

	public void setLastChecked(Date lastChecked) {
		this.lastChecked = lastChecked;
	}

	public String getLastModified() {
		return lastModified;
	}

	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}
	
	protected StringBuilder getStringBuilder() {
		StringBuilder sb = super.getStringBuilder();
		sb.append("uri=").append(uri).append(", ");
		sb.append("name=").append(name).append(", ");
		sb.append("link=").append(link).append(", ");
		sb.append("expireAfter=").append(expireAfter).append(", ");
		sb.append("lastChecked=").append(lastChecked).append(", ");
		sb.append("lastModified=").append(lastModified).append(", ");
		sb.append("etag=").append(etag);
		return sb;
	}
}
