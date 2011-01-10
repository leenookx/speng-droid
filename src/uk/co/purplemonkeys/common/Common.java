package uk.co.purplemonkeys.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

public class Common 
{
	public static void ShowErrorToast(Context context, String error, int duration) 
	{
		Toast toast = Toast.makeText(context, error, duration);
		toast.setDuration( duration );
		toast.show();
	}
	
	public static void ShowAlertMessage(Context c, String message)
	{
        // Create the alert box
        AlertDialog.Builder alertbox = new AlertDialog.Builder(c);

        // Set the message to display
        alertbox.setMessage( message );

        // Add a neutral button to the alert box and assign a click listener
        alertbox.setNeutralButton("Ok", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// We don't really care about this here...
			}
        });

         // show the alert box
        alertbox.show();
	}
}
