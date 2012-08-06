package mobi.pruss.GalacticNight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class GalacticNight extends Activity {
	private ScreenControl screenControl;
	private LinearLayout main;
	private boolean installed;
	public static final boolean DEBUG = true;

	private void message(String title, String msg, final boolean finish) {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();

		Log.e("GalacticNight", title);

		alertDialog.setTitle(title);
		alertDialog.setMessage(msg);
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				"OK", 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {if (finish) finish();} });
		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {if (finish) finish();} });
		alertDialog.show();

	}
	
	private void setInstalled(boolean value) {
		installed = value;
		
		Button install = (Button)findViewById(R.id.install); 
		
		if (screenControl.isAlwaysInstalled())
			install.setVisibility(View.GONE);

		if (installed) {
			install.setText("Uninstall");
			show(R.id.color_modes);
		}
		else {
			install.setText("Install");
			hide(R.id.color_modes);
		}
		
		if (!screenControl.supportsOutdoor())
			findViewById(R.id.outdoor).setVisibility(View.GONE);
		if (!screenControl.supportNatural())
			findViewById(R.id.natural).setVisibility(View.GONE);
		
	}
	
	private void show(int id) {
		findViewById(id).setVisibility(View.VISIBLE);
	}

	private void hide(int id) {
		findViewById(id).setVisibility(View.INVISIBLE);
	}


	public void mode(View v) {
		int id = v.getId();
		
		if (id == R.id.install) {
			if (installed) {
				screenControl.uninstall();
				if (screenControl.deemInstalled()) {
					message("Failure uninstalling",
							"This shouldn't have happened but it did: we were unable to "+
							"uninstall GalacticNight control.  You can try again later, "+
							"or you can attempt to restore the /system/etc/tune_ui_dynamic_mode "+
							"and /system/etc/tune_ui_dynamic_mode files from their *.orig copies.",
							true);					
				}
				else {
					setInstalled(false);
				}
			}
			else {
				if (!screenControl.install()) {
					message("Failure installing",
							"It seems that your device is not supported.  You may want to "+
							"install catlog from Android Market, try installing GalacticNight again "+
							"and if it fails again, go immediately to catlog and send your log and device information "+
							"to arpruss@gmail.com",
							true);
				}
				else {
					setInstalled(true);
				}
			}

			return;
		}

		for (int i=0; i<ScreenControl.NUM_MODES; i++) {
			if (id == ScreenControl.ids[i]) {
				screenControl.set(i);
				break;
			}
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		main = (LinearLayout)getLayoutInflater().inflate(R.layout.main, null);
		setContentView(main);
		
		screenControl = ScreenControl.getScreenControl(this);
		if (screenControl == null || !screenControl.valid) {
			message("Failure starting",
					"It seems that your device is not rooted or not supported.  You may want to "+
					"install catlog from Android Market, try installing GalacticNight again "+
					"and if it fails again, go immediately to catlog and send your log and device information "+
					"to arpruss@gmail.com", true);
		}
		else {
			setInstalled(screenControl.deemInstalled());
		}
	}
	
    void resize() {
    	LinearLayout ll = main;
    	FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)ll.getLayoutParams();

    	int h = getWindowManager().getDefaultDisplay().getHeight();
    	int w = getWindowManager().getDefaultDisplay().getWidth();
    	
    	int min = h<w ? h : w;
    	
    	lp.width = min*95/100;
		ll.setLayoutParams(lp);
    }


	@Override
	public void onResume() {
		super.onResume();
		
		resize();
		if (screenControl != null)
			screenControl.updateService();
		
		GalacticNight.log("resume");
	}	
	
	static public void log(String s) {
		if (DEBUG) {
			Log.v("GalacticNight", s);
		}
	}
}
