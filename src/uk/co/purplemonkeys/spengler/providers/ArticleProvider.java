package uk.co.purplemonkeys.spengler.providers;

import java.util.HashMap;

import uk.co.purplemonkeys.spengler.providers.Article.Articles;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class ArticleProvider extends ContentProvider 
{
    private static final String TAG = "ProjectProvider";
    private static final String DATABASE_NAME = "spengler.db";
    private static final int DATABASE_VERSION = 1;
    private static final String PROJECT_TABLE_NAME = "articles";
    private static HashMap<String, String> sArticleProjectionMap;
    private static final UriMatcher sUriMatcher;

    private DatabaseHelper mOpenHelper;
    
    private static final int ARTICLES = 1;
    
    static 
    {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Article.AUTHORITY, "Articles", ARTICLES);

        sArticleProjectionMap = new HashMap<String, String>();
        sArticleProjectionMap.put(Articles._ID, Articles._ID);
        sArticleProjectionMap.put(Articles.TITLE, Articles.TITLE);
        sArticleProjectionMap.put(Articles.PAGE_URL, Articles.PAGE_URL);
    }

    @Override
    public boolean onCreate() 
    {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) 
    {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) 
        {
	        case ARTICLES:
	        	qb.setTables(PROJECT_TABLE_NAME);
	            qb.setProjectionMap(sArticleProjectionMap);
	            break;
	        default:
	            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) 
        {
            orderBy = Articles.DEFAULT_SORT_ORDER;
        } 
        else 
        {
            orderBy = sortOrder;
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) 
    {
        switch (sUriMatcher.match(uri)) 
        {
	        case ARTICLES:
	            return Articles.CONTENT_TYPE;
	        default:
	            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues)
    {
        switch (sUriMatcher.match(uri)) 
        {
	        case ARTICLES:
	        	return insertProject(uri, initialValues);
	        default:
	        	throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) 
    {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) 
        {
	        case ARTICLES:
	            count = db.delete(PROJECT_TABLE_NAME, where, whereArgs);
	            break;
	
	        default:
	            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) 
    {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) 
        {
	        case ARTICLES:
	            count = db.update(PROJECT_TABLE_NAME, values, where, whereArgs);
	            break;
	
	        default:
	            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        
        return count;
    }
    
    private Uri insertProject(Uri uri, ContentValues initialValues)
    {
        ContentValues values;
        if (initialValues != null) 
        {
            values = new ContentValues(initialValues);
        } 
        else 
        {
            values = new ContentValues();
        }

        if (values.containsKey(Articles.TITLE) == false) 
        {
            Resources r = Resources.getSystem();
            values.put(Articles.TITLE, r.getString(android.R.string.untitled));
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(PROJECT_TABLE_NAME, Articles.TITLE, values);
        if (rowId >= 0) 
        {
            Uri noteUri = ContentUris.withAppendedId(Articles.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);    	
    }
    
    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper 
    {
        DatabaseHelper(Context context) 
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + PROJECT_TABLE_NAME + " ("
                    + Articles._ID + " INTEGER PRIMARY KEY,"
                    + Articles.TITLE + " TEXT,"
                    + Articles.PAGE_URL + " TEXT"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
        {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " 
            			+ newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + PROJECT_TABLE_NAME);
            onCreate(db);
        }
    }
}
