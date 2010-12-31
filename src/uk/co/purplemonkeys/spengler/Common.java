package uk.co.purplemonkeys.spengler;

import android.content.Context;
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
}
