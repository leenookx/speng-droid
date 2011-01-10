package uk.co.purplemonkeys.spengler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import uk.co.purplemonkeys.common.Common;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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
import android.widget.Toast;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class SpengDroid extends Activity 
{
	private SharedPreferences preferences;
	private String version_info;
	private ArrayAdapter<String> adapter = null;
	private ListView listView;
	private Button goButton;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
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
		
		listView = (ListView) this.findViewById(R.id.ListView);
		adapter = new ArrayAdapter<String>(this, R.layout.main, R.id.ListView);
		listView.setAdapter(adapter);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
    	String rss = "http://192.168.230.183:3000/user/feed/abc123";
    	
    	Common.ShowErrorToast(getApplicationContext(), "Retrieving user feed", Toast.LENGTH_SHORT);
    	URL feedUrl;
    	
    	try
    	{
    		Log.d("DEBUG", "Entered:" + rss);
    		feedUrl = new URL(rss);
    		SyndFeedInput input = new SyndFeedInput();
    		SyndFeed feed = input.build(new XmlReader(feedUrl));
    		List entries = feed.getEntries();
    		Toast.makeText(this, "#Feeds retrieved: " + entries.size(), Toast.LENGTH_SHORT);
    		Iterator iterator = entries.listIterator();
    		while (iterator.hasNext())
    		{
    			SyndEntry ent = (SyndEntry) iterator.next();
    			String title = ent.getTitle();
    			adapter.add(title);
    		}
    		adapter.notifyDataSetChanged();
    	}
    	catch (MalformedURLException e)
    	{
    		Toast.makeText(this, "MalformedURLException", Toast.LENGTH_SHORT);
    		e.printStackTrace();
    	}
    	catch (IllegalArgumentException e)
    	{
    		Toast.makeText(this, "IllegalArgumentException", Toast.LENGTH_SHORT);
    		e.printStackTrace();
    	}
    	catch (FeedException e)
    	{
    		Toast.makeText(this, "FeedException", Toast.LENGTH_SHORT);
    		e.printStackTrace();
    	}
    	catch (IOException e)
    	{
    		Toast.makeText(this, "IOException", Toast.LENGTH_SHORT);
    		e.printStackTrace();
    	}
    }
}