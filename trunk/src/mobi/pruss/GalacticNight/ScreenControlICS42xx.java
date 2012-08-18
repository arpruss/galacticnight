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

public class ScreenControlICS42xx extends ScreenControl {
	public static final boolean FORCE_S3 = false; // debug
	public static final boolean FORCE_ICS_S2 = false; // debug
	
	protected static final String MODE = "/sys/class/mdnie/mdnie/mode";
	protected static final String OUTDOOR_CONTROL = "/sys/class/mdnie/mdnie/outdoor";
	protected static final String TUNING_CONTROL = "/sys/class/mdnie/mdnie/tunning"; // [sic]
	
//	public static final String MODE = "/sdcard/mdnie/test_mode";
//	protected static final String OUTDOOR_CONTROL = "/sdcard/mdnie/test_outdoor";
//	public static final String TUNING_CONTROL = "/sdcard/mdnie/test_tunning";
	
	protected static final String SUBST = "GalacticNightTuning"; // must not start with 0 or 1

	public ScreenControlICS42xx(Context context) {
		super(context);

		selectorPath = MODE; 
		(new File("/sdcard/mdnie")).mkdir();
		gnDir = "/sdcard/mdnie/";
		workingColorPath = gnDir + SUBST;
		
		valid = false;		
		
		if (unlockICS42xx()) {
			valid = true;
		}
		else {
			GalacticNight.log("Failing unlocking");
		}
	}
	
	private static boolean unlockICS42xx() {
		return Root.runOne("chmod 666 "+MODE+" "+TUNING_CONTROL+" "+OUTDOOR_CONTROL);		
	}
	
	
	
	@Override
	public void lock() {
		Root.runOne("chmod 664 "+selectorPath+" "+TUNING_CONTROL+" "+OUTDOOR_CONTROL);
	}
	
	public static boolean detectICS42xx() {
		if (FORCE_S3)
			return true;
		
		boolean modeExists = new File(MODE).exists();
		boolean tuningExists = new File(TUNING_CONTROL).exists();
		
		GalacticNight.log(""+MODE+" "+modeExists+" "+TUNING_CONTROL+" "+tuningExists);

		return modeExists && tuningExists;
//		return (new File(MODE).exists()) &&
//		   (new File(TUNING_CONTROL).exists());
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
		return readLine(TUNING_CONTROL);
	}

	public static boolean tuningControlWrite(Context context, String s, boolean startServiceIfNeeded) {
		File tuning = new File(TUNING_CONTROL);
		if (!tuning.canWrite()) {
			GalacticNight.log("unlocking");
			unlockICS42xx();
		}
		FileWriter w = null;
		boolean success = false;
			
		try {
			w = new FileWriter(new File(TUNING_CONTROL));
			w.write(s+"\n");
			w.close();
			GalacticNight.log("Wrote "+s+" to "+TUNING_CONTROL);
			success = true;
		} catch (IOException e) {
			GalacticNight.log("Error writing "+e);
			if (w != null) {
				try {
					w.close();
				} catch (IOException e1) {
				}
			}
		}
		
		if (startServiceIfNeeded) {
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
	public void updateService() {
		context.stopService(new Intent(context, PowerOnService.class));

		String file = tuningControlRead();
		if (file != null && file.length()>0) {
			context.startService(new Intent(context, PowerOnService.class));
		}
	}
	
	protected void setOutdoor() {
		tuningControlWrite("0");
		String mode;
		mode = readLine(MODE);
		if (mode == null)
			mode = "" + STANDARD;
		writeLine(MODE, mode);
		writeLine(OUTDOOR_CONTROL, "1");
	}
	
	protected void setOSMode(int mode) {
		tuningControlWrite("0");
		saveMode(mode);
		writeLine(OUTDOOR_CONTROL,"0");
		selectMode(mode);
		(new File(workingColorPath)).delete();
	}
	
	@Override
	public boolean isICS42xx() {
		return true;
	}
}
