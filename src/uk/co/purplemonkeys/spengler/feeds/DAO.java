/*
 * $Id: DAO.java 25 2007-12-05 06:16:10Z azurite@telusplanet.net $
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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public abstract class DAO implements BaseColumns {
	protected long id = -1;
	protected ContentResolver contentResolver;
	
	public DAO(ContentResolver contentResolver) {
		this.contentResolver = contentResolver;
	}
	
	public DAO(ContentResolver contentResolver, long id) {
		this.contentResolver = contentResolver;
		this.id = id;
	}
	
	protected abstract Uri getContentURI();
	
	public boolean load() {
		return load((String[]) null);
	}

	public boolean load(String[] columns) {
		if (id == -1) {
			return false;
		}
		
		return load(getContentURI().addId(id), columns);
	}
	
	public boolean load(Cursor cursor) {
		if (cursor.getColumnIndex(_ID) != -1) {
			id = cursor.getLong(cursor.getColumnIndex(_ID));
		}
		
		doLoad(cursor, true);
		return true;
	}
	
	protected abstract void doLoad(Cursor cursor, boolean subset);
	
	protected boolean load(ContentURI uri) {
		return load(uri, null);		
	}
	
	protected boolean load(ContentURI uri, String[] columns) {
		if (contentResolver == null) {
			return false;
		}
		
		Cursor c = contentResolver.query(uri, columns, null, null, null);
		if (c == null || !c.first()) {
			return false;
		}
		
		id = c.getLong(0);
		doLoad(c, columns != null);
		c.close();
		
		return true;		
	}

	public boolean saveOrUpdate() {
		if (id != -1) {
			return update();
		} else {
			return save();
		}
	}
	
	public boolean save() {
		if (contentResolver == null) {
			return false;
		}
		
		ContentValues values = new ContentValues();
		if (!doSaveOrUpdate(values)) {
			return false;
		}
		
		ContentURI uri = contentResolver.insert(getContentURI(), values);
		if (uri == null) {
			return false;
		}
		
		id = uri.getPathLeafId();
		return true;
	}
	
	public boolean update() {
		if (contentResolver == null || id == -1) {
			return false;
		}
		
		ContentValues values = new ContentValues();
		values.put(BaseColumns._ID, id);
		if (!doSaveOrUpdate(values)) {
			return false;
		}
		
		ContentURI uri = getContentURI().addId(id);
		return contentResolver.update(uri, values, null, null) == 1;
	}
	
	public ContentValues getContentValues() {
		ContentValues values = new ContentValues();
		if (id != -1) {
			values.put(BaseColumns._ID, id);
		}
		
		if (!doSaveOrUpdate(values)) {
			return null;
		}
		
		return values;
	}

	protected abstract boolean doSaveOrUpdate(ContentValues values);
	
	public ContentResolver getContentResolver() {
		return contentResolver;
	}

	public void setContentResolver(ContentResolver contentResolver) {
		this.contentResolver = contentResolver;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof DAO) {
			DAO dao = (DAO) o;
			return this.id == dao.id;
		}
		return super.equals(o);
	}
	
	@Override
	public String toString() {
		return getStringBuilder().append('}').toString();
	}
	
	protected StringBuilder getStringBuilder() {
		StringBuilder sb = new StringBuilder("{");
		sb.append("id=").append(id);
		return sb;
	}
}
