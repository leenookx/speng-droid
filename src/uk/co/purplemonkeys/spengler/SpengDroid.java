package uk.co.purplemonkeys.spengler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import uk.co.purplemonkeys.common.Common;
import uk.co.purplemonkeys.spengler.articlefeed.ArticlesList;
import uk.co.purplemonkeys.spengler.tasks.FeedUpdaterTask;
import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;

public class SpengDroid extends TabActivity 
{
	private final static String TAG = "SpengDroid";
	private SharedPreferences preferences;
	private String version_info;
	private ScheduledExecutorService ex = null;
	
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
		
	    Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    // Create an Intent to launch an Activity for the tabs
	    intent = new Intent().setClass(this, ArticlesList.class);

	    // Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("Feed")
	    			.setIndicator("Feed", res.getDrawable(android.R.drawable.ic_search_category_default))
	    				.setContent(intent);
	    tabHost.addTab(spec);

	    tabHost.setCurrentTab(1);
	    
	    // Update the RSS feed when we start the application (assuming
	    // the current update preference setting is not 'Never'.
	    int RSSUpdateRate = preferences.getInt("rss_update_rate", 0);
	    if (RSSUpdateRate > 0)
	    {
	    	new FeedUpdaterTask( this ).execute();
	    }
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
    			new FeedUpdaterTask( this ).execute();
    			return true;
			default:
				break;
    	}

    	return super.onOptionsItemSelected(item);
    }
}