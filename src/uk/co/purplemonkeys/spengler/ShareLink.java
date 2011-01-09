package uk.co.purplemonkeys.spengler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import uk.co.purplemonkeys.common.Common;
import uk.co.purplemonkeys.common.XmlResponseParser;
import uk.co.purplemonkeys.common.http.HttpCommon;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ShareLink extends Activity 
{
	private final String TAG = "ShareLink";
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.share_link);
		
		String intentAction = getIntent().getAction();
        if (Intent.ACTION_SEND.equals(intentAction)) 
        {
        	// Share
        	Bundle extras = getIntent().getExtras();
        	if (extras != null) 
        	{
        		String url = extras.getString(Intent.EXTRA_TEXT);
        		final EditText submitLinkUrl = (EditText) findViewById(R.id.submit_link_url);
        		submitLinkUrl.setText(url);
        		final EditText submitLinkTitle = (EditText) findViewById(R.id.submit_link_title);
        		submitLinkTitle.setText( fetchHtmlTitle(url) );
        	}
        }
	
	    final Button submitLinkButton = (Button) findViewById(R.id.submit_link_button);
	    submitLinkButton.setOnClickListener(new OnClickListener() 
	    {
	    	public void onClick(View v) 
	    	{
	    		if (validateLinkForm()) 
	    		{
					final EditText submitLinkUrl = (EditText) findViewById(R.id.submit_link_url);
					final EditText submitLinkTitle = (EditText) findViewById(R.id.submit_link_title);
					final EditText submitLinkDescription = (EditText) findViewById(R.id.submit_link_description);
					final EditText submitLinkKeywords = (EditText) findViewById(R.id.submit_link_keywords);
					new ShareLinkTask(submitLinkUrl.getText().toString(),
										submitLinkDescription.getText().toString(),
										submitLinkKeywords.getText().toString(),
										submitLinkTitle.getText().toString()
									).execute();
	    		}
	    	}
	    });
	}

    private boolean validateLinkForm() 
    {
    	final EditText urlText = (EditText) findViewById(R.id.submit_link_url);
    	if ("".equals(urlText.getText())) 
    	{
    		Common.ShowErrorToast("Please provide a URL.", Toast.LENGTH_LONG, this);
    		return false;
    	}
    	
    	return true;
    }
    
	public final String fetchHtmlTitle(String url) 
	{
		HttpGet get = new HttpGet(url);
		DefaultHttpClient mHttpClient = HttpCommon.createGzipHttpClient();	
		HttpEntity e;
		try 
		{
			HttpResponse resp = mHttpClient.execute(get);
			e = resp.getEntity();
		} 
		catch (Throwable t) {
		    Log.d(TAG, "HTTP request failed", t);
			return null;
		}
		
		XmlResponseParser<String> titleParser = new XmlResponseParser<String>(e) 
		{
			@Override
			public boolean onXmlEvent(XmlPullParser parser, int eventType) throws XmlPullParserException, IOException {
				switch(eventType) {
				case XmlPullParser.START_TAG:
				    String tagName = parser.getName().toLowerCase();
					if ("title".equals(tagName)) 
					{
						// Handle HTML
						setResult(parser.nextText());
						return false;
					} 
					else if ("card".equals(tagName)) 
					{
						// Handle WML - XXX - check that this is proper semantics
						setResult(parser.getAttributeValue(null, "title"));
						return false;
					}
					break;
				}
				
				return true;
			}
		};
		
		return titleParser.parse();
	}
    
    private class ShareLinkTask extends AsyncTask<Void, Void, Object> 
    {
    	private final static String TAG = "SharedLinkTask";
    	
        private String _url;
        private String _description;
        private String _keywords;
        private String _title;
        private DefaultHttpClient mClient = HttpCommon.createGzipHttpClient();
        
        ShareLinkTask(String url, String description, String keywords, String title)
        {
        	_url = url;
        	_description = description;
        	_keywords = keywords;
        	_title = title;
        }
       
        @Override
        public Object doInBackground(Void... unused_params) 
        {
        	HttpEntity entity = null;
        	
            try 
            {
            	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            	String url = prefs.getString("pref_url", "http://localhost:3000");
            	String auth_code = prefs.getString("pref_auth_code", "abc123");

            	// Construct data
            	JSONObject post_params = new JSONObject();
            	post_params.put("auth_code", auth_code);
            	
            	JSONObject sub = new JSONObject();
            	sub.put("url", _url);
            	sub.put("description", _description);
            	sub.put("keywords", _keywords);
            	sub.put("title", _title);
            	
            	post_params.put("links", sub);
            	
            	HttpPost httppost = new HttpPost(url + "/links");
            	
            	// The progress dialog is non-cancelable, so set a shorter timeout than system's
            	HttpParams params = httppost.getParams();
            	HttpConnectionParams.setConnectionTimeout(params, 30000);
            	HttpConnectionParams.setSoTimeout(params, 30000);
       
            	StringEntity s = new StringEntity(post_params.toString());
            	s.setContentEncoding("UTF-8");
            	s.setContentType("application/json");
            	params.setParameter("authentication-token", auth_code);
            	httppost.setEntity(s);
                
            	// Perform the HTTP POST request
            	HttpResponse response = mClient.execute(httppost);
            	String status = response.getStatusLine().toString();
                if (!status.contains("OK"))
                {
                	throw new HttpException(status);
                }
                
                entity = response.getEntity();

                BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
                String line = in.readLine();
                in.close();
                if (line == null || "".equals(line)) 
                {
                	throw new HttpException("No content returned from reply POST");
                }

                entity.consumeContent();
                
                Common.ShowAlertMessage(getBaseContext(), "Success");
                
                // TODO: Return something more meaningful later.
                return new Object();
            }
            catch (Exception e) 
            {
            	if (entity != null) 
            	{
            		try 
            		{
            			entity.consumeContent();
            		} 
            		catch (Exception e2) {
            			Log.e(TAG, "entity.consumeContent()", e2);
            		}
            	}
            	
            	Log.e(TAG, "ShareLinkTask", e);
            }
            
            return null;
        }
        
        @Override
        public void onPreExecute() 
        {
        }
       
        @Override
        public void onPostExecute(Object result) 
        {
        	finish();
        }        
    }
}