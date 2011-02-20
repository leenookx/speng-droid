package uk.co.purplemonkeys.spengler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import uk.co.purplemonkeys.common.Common;
import uk.co.purplemonkeys.spengler.providers.Article.Articles;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class SpengDroid extends Activity 
{
	private final static String TAG = "SpengDroid";
	private SharedPreferences preferences;
	private String version_info;
	private ArrayAdapter<String> adapter = null;
	private ListView listView;
	private Button goButton;
	private String[] PROJECTION = new String[] 
	                                         {
	                                             Articles._ID, 
	                                             Articles.TITLE,
	                                             Articles.URL
	                                         };
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Initialise preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		try
		{
			PackageManager pm = getPackageManager();
			PackageInfo pi = pm.getPackageInfo("uk.co.purplemonkeys.spengler", 0);
			version_info = "SpengDroid " + pi.versionName;
		}
		catch (NameNotFoundException e)
		{
			version_info = "Couldn't determine version info.";
		}
		
        goButton = (Button) this.findViewById(R.id.goButton);
        goButton.setOnClickListener(new OnClickListener()
        {
        	@Override
        	public void onClick(View v)
        	{
        		getRSS();
        	}
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.spengdroid, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	switch (item.getItemId()) 
    	{
    		case R.id.preferences_menu_id:
    			startActivity(new Intent(this, Preferences.class));
    			return true;
    		case R.id.about_menu_id:
    			Common.ShowAlertMessage(this, version_info);
    			return true;
    		case R.id.refresh_feed_menu_id:
    			getRSS();
    			return true;
			default:
				break;
    	}

    	return super.onOptionsItemSelected(item);
    }
    
    private void getRSS()
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	String url = prefs.getString("pref_url", "http://localhost:3000");
    	String auth_code = prefs.getString("pref_auth_code", "abc123");
    	
    	String rss = url + "/user/feed/" + auth_code;
    	
    	Common.ShowErrorToast(getApplicationContext(), "Retrieving user feed", Toast.LENGTH_SHORT);
    	URL feedUrl;
    	
    	Context _appContext = getApplicationContext();
    	
    	try
    	{
    		// Clear out all of the existing articles.
    		_appContext.getContentResolver().delete(Articles.CONTENT_URI, null, null);
    		
    		feedUrl = new URL(rss);
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
				cv.put(Articles.URL, entry.getUri());
				
				_appContext.getContentResolver().insert(Articles.CONTENT_URI, cv);
    		}
    	}
    	catch (MalformedURLException e)
    	{
    		Toast.makeText(this, "MalformedURLException", Toast.LENGTH_SHORT);
    		Log.e(TAG, e.toString());
    	}
    	catch (IllegalArgumentException e)
    	{
    		Toast.makeText(this, "IllegalArgumentException", Toast.LENGTH_SHORT);
    		Log.e(TAG, e.toString());
    	}
    	catch (FeedException e)
    	{
    		Toast.makeText(this, "FeedException", Toast.LENGTH_SHORT);
    		Log.e(TAG, e.toString());
    	}
    	catch (IOException e)
    	{
    		Toast.makeText(this, "IOException", Toast.LENGTH_SHORT);
    		Log.e(TAG, e.toString());
    	}
    }
}