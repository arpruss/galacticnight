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
	public static final boolean DEBUG = true;

	private void message(String title, String msg) {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();

		alertDialog.setTitle(title);
		alertDialog.setMessage(msg);
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				"OK", 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {} });
		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {} });
		alertDialog.show();

	}
	

	private void fatalError(String title, String msg) {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();

		Log.e("GalacticNight fatalError", title);

		alertDialog.setTitle(title);
		alertDialog.setMessage(msg);
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				"OK", 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {finish();} });
		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {finish();} });
		alertDialog.show();		
	}
	
	public void mode(View v) {
		switch(v.getId()) {
		case R.id.normal:
			screenControl.set(ScreenControl.NORMAL);
			break;
		case R.id.red:
			screenControl.set(ScreenControl.RED);
			break;
		case R.id.green:
			screenControl.set(ScreenControl.GREEN);
			break;
		case R.id.bw:
			screenControl.set(ScreenControl.BW);
			break;
		case R.id.reversed_red:
			screenControl.set(ScreenControl.REVERSED_RED);
			break;
		case R.id.reversed_green:
			screenControl.set(ScreenControl.REVERSED_GREEN);
			break;
		case R.id.reversed_bw:
			screenControl.set(ScreenControl.REVERSED_BW);
			break;
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
//		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		main = (LinearLayout)getLayoutInflater().inflate(R.layout.main, null);
		setContentView(main);
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
		
		GalacticNight.log("resume");
		
		screenControl = new ScreenControl(this);
		
		if (!screenControl.valid) {
			fatalError("Device not supported",
					"Your device is either not supported or not rooted.");
		}
	}	
	
	@Override
	public void onPause() {
		super.onPause();
		
		if (screenControl.valid)
			screenControl.lock();
	}
	
	static public void log(String s) {
		if (DEBUG) {
			Log.v("GalacticNight", s);
		}
	}
}
