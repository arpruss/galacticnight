package mobi.pruss.GalacticNight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;

public class ScreenControlICSMDP extends ScreenControl {
	protected static final String OUTDOOR_CONTROL = "/sys/class/mdnie/mdnie/outdoor";
	protected static final String SCENARIO_CONTROL = "/sys/class/mdnie/mdnie/scenario";
	protected static final String TUNING_CONTROL = "/sys/class/mdnie/mdnie/tuning"; // [sic]
//	protected static final String OUTDOOR_CONTROL = "/sdcard/mdnie/outdoor";
//	protected static final String SCENARIO_CONTROL = "/sdcard/mdnie/scenario";
//	protected static final String TUNING_CONTROL = "/sdcard/mdnie/tuning"; // [sic]
	
	protected static final String SUBST = "GalacticNightTuning";

	public ScreenControlICSMDP(Context context) {
		super(context);

		(new File("/sdcard/tuning")).mkdir();
		gnDir = "/sdcard/tuning/";
		workingColorPath = gnDir + SUBST;
		
		valid = false;		
		
		if (unlockICSMDP()) {
			valid = true;
		}
		else {
			GalacticNight.log("Failing unlocking");
		}
	}
	
	private static boolean unlockICSMDP() {
		return Root.runOne("chmod 666 "+SCENARIO_CONTROL+" "+TUNING_CONTROL+" "+OUTDOOR_CONTROL);		
	}
	
	@Override
	public void set(int setting) {
		switch(setting) {
		case STANDARD:
			new File(workingColorPath).delete();
			writeLine(OUTDOOR_CONTROL, "0");
			writeLine(SCENARIO_CONTROL, "0");
			break;
		case OUTDOOR:
			new File(workingColorPath).delete();
			writeLine(SCENARIO_CONTROL, "0");
			writeLine(OUTDOOR_CONTROL, "1");
		case RED:
			tune(0xFF,0x00,0x00);
			break;
		case GREEN:
			tune(0x00,0xFF,0x00);
			break;
		case BLUE:
			tune(0x00,0x00,0xFF);
			break;
		case NOBLUE:
			tune(0xFF,0xFF,0);
			break;
		case SEPIA:
			tune(0xE9,0xD8,0xBA);
			break;
		case REVERSE:
			reverse();
			break;
		}
	}
	
	private void reverseColor(int[][] rgb, int c) {
		for (int j=0; j<128; j++) {
			int t = rgb[j][c];
			rgb[j][c] = rgb[255-j][c];
			rgb[255-j][c] = t;
		}
	}
	
	private void reverse() {
		int[][] rgb = new int[256][3];

		try {
			BufferedReader reader = new BufferedReader( new FileReader(new File(workingColorPath)));
			String line;
			int i = 0;
			Pattern pat = Pattern.compile("\\s*0[xX]([a-fA-F0-9]+)\\s*,\\s*0[xX]([a-fA-F0-9]+),\\s*0[xX]([a-fA-F0-9]+)");
			while(null != (line = reader.readLine()) && i < 256) {
				Matcher m = pat.matcher(line);
				if (m.find()) {
					rgb[i][0] = Integer.parseInt(m.group(1),16);
					rgb[i][1] = Integer.parseInt(m.group(2),16);
					rgb[i][2] = Integer.parseInt(m.group(3),16);
					i++;
				}
			}
			if (i<255) {
				throw(new IOException());
			}
			reverseColor(rgb, 0);
			reverseColor(rgb, 1);
			reverseColor(rgb, 2);
		} catch (IOException e) {
			for (int i=0; i<256; i++) 
				rgb[i][0] = rgb[i][1] = rgb[i][2] = i;			
		}
		
		String data = "0";
		for (int i=0; i<256; i++) {
			data += String.format("\n0x%02x, 0x%02x, 0x%02x",
					rgb[i][0], rgb[i][1], rgb[i][2]);;
		}

		if (writeLine(workingColorPath, data)) 
			writeLine(TUNING_CONTROL, SUBST);
		else
			new File(workingColorPath).delete();
	}
	
	private void tune(int r, int g, int b) {
		String data = "0";
		for (int i=0; i<256; i++) {
			data += String.format("\n0x%02x, 0x%02x, 0x%02x",
					(r * i + 127) / 255,
					(g * i + 127) / 255,
					(b * i + 127) / 255);
		}
		
		if (writeLine(workingColorPath, data)) 
			writeLine(TUNING_CONTROL, SUBST);
		else
			new File(workingColorPath).delete();
	}
	
	@Override
	public void lock() {
		Root.runOne("chmod 664 "+SCENARIO_CONTROL+" "+TUNING_CONTROL+" "+OUTDOOR_CONTROL);
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
			unlockICSMDP();
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
		
		return success;
	}
	
	protected void setOutdoor() {
		writeLine(SCENARIO_CONTROL, "0");
		writeLine(OUTDOOR_CONTROL, "1");
	}
	
	@Override
	public boolean isICS42xx() {
		return false;
	}
	
	@Override
	public boolean support(int setting) {
		switch(setting) {
		case STANDARD:
		case RED:
		case GREEN:
		case REVERSE:
		case SEPIA:
		case OUTDOOR:
		case NOBLUE:
		case BLUE:
			return true;
		default:
			return false;
		}
	}

	static public boolean detect(String cpu) {
		return new File(SCENARIO_CONTROL).exists() &&
			new File(TUNING_CONTROL).exists() &&
			new File(OUTDOOR_CONTROL).exists() &&
			(cpu.contains("quincy") || cpu.contains("SAMSUNG M2_"));
	}
}
