package uk.co.purplemonkeys.spengler;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class Common 
{
	static void showErrorToast(String error, int duration, Context context) 
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		Toast t = new Toast(context);
		t.setDuration(duration);
		View v = inflater.inflate(R.layout.error_toast, null);
		TextView errorMessage = (TextView) v.findViewById(R.id.errorMessage);
		errorMessage.setText(error);
		t.setView(v);
		t.show();
	}
	
	static void showAlertMessage(Context c, String message)
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
