package mobi.pruss.GalacticNight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;

public class ScreenControlICS extends ScreenControl {
	public static final boolean FORCE_S3 = false; // debug
	public static final boolean FORCE_ICS_S2 = false; // debug
	
	protected static final String SELECTOR = "/sys/class/mdnie/mdnie/mode";
	protected static final String TUNING_CONTROL = "/sys/class/mdnie/mdnie/tunning"; // [sic]
	
//	public static final String SELECTOR = "/sdcard/GalacticNight/test_mode";
//	public static final String TUNING_CONTROL = "/sdcard/GalacticNight/test_tunning";
	
	protected static final String SUBST = "GalacticNightTuning"; // must not start with 0 or 1
	protected Device device;

	public ScreenControlICS(Context context) {
		super(context);

		selectorPath = SELECTOR; 
		(new File("/sdcard/mdnie")).mkdir();
		gnDir = "/sdcard/mdnie/";
		workingColorPath = gnDir + SUBST;
		
		valid = false;		
		device = new Device();
		
		if (Root.runOne("chmod 666 "+selectorPath+" "+TUNING_CONTROL)) {
			valid = true;
		}
		else {
			GalacticNight.log("Failing unlocking");
		}
	}
	
	public static boolean detectICS() {
		if (FORCE_S3)
			return true;
		return (new File(SELECTOR).exists()) &&
		   (new File(TUNING_CONTROL).exists());
	}
	
	@Override
	public boolean isAlwaysInstalled() {
		return true;
	}
	
	@Override
	public boolean deemInstalled() {
		return true;
	}
	
	@Override
	public boolean install() {
		return true;				
	}
	
	@Override
	public void uninstall() {
	}
	
	public boolean tuningControlWrite(String s) {
		return tuningControlWrite(this.context, s, true);
	}
	
	public static String tuningControlRead() {
		BufferedReader reader;
		try {
			reader = new BufferedReader( new FileReader(new File(TUNING_CONTROL)));
			String line = reader.readLine();
			return line;
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}

	public static boolean tuningControlWrite(Context context, String s, boolean handleService) {
		FileWriter w = null;
		boolean success = false;
			
		try {
			w = new FileWriter(new File(TUNING_CONTROL));
			w.write(s+"\n");
			w.close();
			GalacticNight.log("Wrote "+s+" to "+TUNING_CONTROL);
			success = true;
		} catch (IOException e) {
			if (w != null) {
				try {
					w.close();
				} catch (IOException e1) {
				}
			}
		}
		
		if (handleService) {
			if (s.equals("0") || !success) {
				GalacticNight.log("stopping service");
				context.stopService(new Intent(context, PowerOnService.class));
			}
			else if (s.equals("1") && success) {
				GalacticNight.log("starting service");
				context.stopService(new Intent(context, PowerOnService.class));
				context.startService(new Intent(context, PowerOnService.class));
			}
		}

		return success;
	}
	
	protected boolean writeTweakICS(int[][] a, int[][] b, int[][] c) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter( new FileWriter(new File(workingColorPath)));

			if (a != null)
				for (int i=0; i<a.length; i++) {
					writer.write(String.format("0x%04x,0x%04x,\n",a[i][0],a[i][1]));
				}

			if (b != null)
				for (int i=0; i<b.length; i++) {
					writer.write(String.format("0x%04x,0x%04x,\n",b[i][0],b[i][1]));
				}

			if (c != null)
				for (int i=0; i<c.length; i++) {
					writer.write(String.format("0x%04x,0x%04x,\n",c[i][0],c[i][1]));
				}
			
			writer.close();
		} catch (IOException e) {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e1) {
				}
			}
			(new File(workingColorPath)).delete();
			tuningControlWrite("0");
			GalacticNight.log("error writing to "+workingColorPath+": "+e);
			return false;
		}
		
		if (tuningControlWrite("1") && tuningControlWrite(SUBST)) {
			GalacticNight.log("successfully wrote to "+workingColorPath);
			return true;
		}
		else {
			tuningControlWrite("0");
			return false;
		}
	}
	
	@Override 
	public boolean supportsOutdoor() {
		return false; // TODO!
	}
	
	@Override
	public void updateService() {
		context.stopService(new Intent(context, PowerOnService.class));

		String file = tuningControlRead();
		if (file != null && file.length()>0) {
			context.startService(new Intent(context, PowerOnService.class));
		}
	}
}
