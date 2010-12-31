package uk.co.purplemonkeys.spengler;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ShareLink extends Activity 
{
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
					final EditText submitLinkDescription = (EditText) findViewById(R.id.submit_link_description);
					final EditText submitLinkKeywords = (EditText) findViewById(R.id.submit_link_keywords);
					new ShareLinkTask(
							submitLinkUrl.getText().toString(),
							submitLinkDescription.getText().toString(),
							submitLinkKeywords.getText().toString()).execute();
	    		}
	    	}
	    });
	}

    private boolean validateLinkForm() 
    {
    	final EditText urlText = (EditText) findViewById(R.id.submit_link_url);
    	if ("".equals(urlText.getText())) 
    	{
    		Common.showErrorToast("Please provide a URL.", Toast.LENGTH_LONG, this);
    		return false;
    	}
    	
    	return true;
    }
}