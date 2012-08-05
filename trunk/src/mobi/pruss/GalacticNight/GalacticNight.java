package mobi.pruss.GalacticNight;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.text.method.NumberKeyListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class GalacticNight extends Activity {
	private ScreenControl screenControl;
	private LinearLayout main;
	private boolean installButton;
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
		
		Button b = (Button)findViewById(R.id.install); 
		
		if (screenControl.isAlwaysInstalled())
			b.setVisibility(View.GONE);

		if (installed) {
			b.setText("Uninstall");
			show(R.id.noblue);
			show(R.id.normal);
			show(R.id.outdoor);
			show(R.id.red);
			show(R.id.green);
			show(R.id.bw);
			show(R.id.sepia);
			show(R.id.movie);
			show(R.id.dynamic);
			show(R.id.reverse);
			show(R.id.natural);
		}
		else {
			b.setText("Install");
			hide(R.id.noblue);
			hide(R.id.normal);
			hide(R.id.outdoor);
			hide(R.id.red);
			hide(R.id.green);
			hide(R.id.bw);
			hide(R.id.sepia);
			hide(R.id.movie);
			hide(R.id.dynamic);
			hide(R.id.reverse);
			hide(R.id.natural);
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
		switch(v.getId()) {
		case R.id.normal:
			screenControl.set(ScreenControl.STANDARD);
			break;
		case R.id.natural:
			screenControl.set(ScreenControl.NATURAL);
			break;
		case R.id.outdoor:
			screenControl.set(ScreenControl.OUTDOOR);
			break;
		case R.id.red:
			screenControl.set(ScreenControl.RED);
			break;
		case R.id.noblue:
			screenControl.set(ScreenControl.NOBLUE);
			break;
		case R.id.green:
			screenControl.set(ScreenControl.GREEN);
			break;
		case R.id.bw:
			screenControl.set(ScreenControl.BW);
			break;
		case R.id.sepia:
			screenControl.set(ScreenControl.SEPIA);
			break;
		case R.id.movie:
			screenControl.set(ScreenControl.MOVIE);
			break;
		case R.id.dynamic:
			screenControl.set(ScreenControl.DYNAMIC);
			break;
		case R.id.reverse:
			screenControl.set(ScreenControl.REVERSE);
			break;
		case R.id.install:
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
			break;
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
    	
    	lp.width = min*9/10;
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
