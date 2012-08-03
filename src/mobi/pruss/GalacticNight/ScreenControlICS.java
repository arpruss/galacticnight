package mobi.pruss.GalacticNight;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;

public class ScreenControlICS extends ScreenControl {
	public static final String SELECTOR = "/sys/class/mdnie/scenario";
	public static final String TUNING_CONTROL = "/sys/class/mdnie/tunning"; // [sic]
	private static final String SUBST = "GalacticNightTuning"; // must not start with 0 or 1
	
	public ScreenControlICS(Context context) {
		super(context);

		selectorPath = SELECTOR; 
		(new File("/sdcard/mdnie")).mkdir();
		gnDir = "/sdcard/mdnie/";
		workingColorPath = gnDir + SUBST;
		
		valid = false;
		
		if (Root.runOne("chmod 666 "+selectorPath+" "+TUNING_CONTROL)) {
			valid = true;
		}
		else {
			GalacticNight.log("Failing unlocking");
		}
	}
	
	public static boolean detect() {
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
	
	@Override
	public void set(int setting) {
		int[][] tweak = getTweak(setting);
		
		if (tweak != null) {
			writeTweak(tweak, true);
		}
		else if (setting == STANDARD || setting == MOVIE || setting == DYNAMIC) {
//			saveMode(setting);			
			selectMode(setting == MOVIE ? 3 : setting);
		}
	}
	
	private boolean writeTweak(int[][] tweak, boolean scr) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter( new FileWriter(new File(workingColorPath)));
			for (int i=0; i<tweak.length; i++) {
				writer.write(String.format("0x%04x,0x%04x,\n",tweak[i][0],tweak[i][1]));
			}
			if(scr)
				writer.write("0x0001,0x0040,\n");
			else
				writer.write("0x0001,0x0000,\n");
			writer.close();
		} catch (IOException e) {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e1) {
				}
			}
			(new File(workingColorPath)).delete();
			tuningControlWrite("0"); // optional?
			GalacticNight.log("error writing to "+workingColorPath+": "+e);
			return false;
		}
		
		if (tuningControlWrite("1") && tuningControlWrite(SUBST)) {
			tuningControlWrite("0");
			return true;
		}
		else {
			tuningControlWrite("0");
			return false;
		}
	}
	
	private boolean tuningControlWrite(String s) {
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
	
	
	@Override 
	public boolean supportsOutdoor() {
		return false; // TODO!
	}
}
