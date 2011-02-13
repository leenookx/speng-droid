package uk.co.purplemonkeys.spengler;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey = "dEZjRVlHWUJBQ2tKYXoxZTVTQXRfZUE6MQ",
    				mode = ReportingInteractionMode.NOTIFICATION,
    				resNotifTickerText = R.string.crash_notif_ticker_text,
    				resNotifTitle = R.string.crash_notif_title,
    				resNotifText = R.string.crash_notif_text,
    				resNotifIcon = android.R.drawable.stat_notify_error, // optional. default is a warning sign
    				resDialogText = R.string.crash_dialog_text,
    				resDialogIcon = android.R.drawable.ic_dialog_info, //optional. default is a warning sign
    				resDialogTitle = R.string.crash_dialog_title, // optional. default is your application name
    				resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. when defined, adds a user text field input with this text resource as a label
    				resDialogOkToast = R.string.crash_dialog_ok_toast // optional. displays a Toast message when the user accepts to send a report.
				)

public class SpengApp extends Application
{
    @Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        super.onCreate();
    }
}
