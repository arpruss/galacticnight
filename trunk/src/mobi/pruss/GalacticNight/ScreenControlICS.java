package mobi.pruss.GalacticNight;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;

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
	

	protected boolean tuningControlWrite(String s) {
		FileWriter w = null; 
			
		try {
			w = new FileWriter(new File(TUNING_CONTROL));
			w.write(s+"\n");
			w.close();
		} catch (IOException e) {
			if (w != null) {
				try {
					w.close();
				} catch (IOException e1) {
				}
			}
			return false;
		}

		return true;
	}
	
	protected boolean writeTweak(int[][] a, int[][] b, int[][] c) {
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
//			tuningControlWrite("0");
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
}
