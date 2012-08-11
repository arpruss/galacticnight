package mobi.pruss.GalacticNight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.provider.Settings.SettingNotFoundException;

abstract public class ScreenControl {
	public boolean valid;
	public static final int DYNAMIC = 0;
	public static final int STANDARD = 1;
	public static final int MOVIE = 2;
	public static final int NATURAL = 3;
	public static final int RED = 4;
	public static final int GREEN = 5;
	public static final int BW = 6;
	public static final int REVERSE = 7;
	public static final int SEPIA = 8;
	public static final int OUTDOOR = 9;
	public static final int NOBLUE = 10;
	public static final int BLUE = 11;
	public static final int OUTDOOR_ICS = 12;
	public static final int NUM_MODES = 13;
	
	public static final int[] ids = {
		R.id.dynamic, R.id.normal, R.id.movie, R.id.natural, R.id.red,
		R.id.green, R.id.bw, R.id.reverse, R.id.sepia, R.id.outdoor,
		R.id.noblue, R.id.blue, R.id.outdoor_ics
	};
	public static final String[] prefs = {
		"dynamic", "standard", "movie", "natural", "red",
		"green", "bw", "invert", "sepia", "outdoor", "noBlue", "blue",
		"outdoor"
	};
	
	protected static final int[] sepia 
	  = { 0x0000, //r
		  0x0000,
		  0xffff,
		  0xffe9,
		  0x0000, //g
		  0xffff,
		  0x0000,
		  0xffd8,
		  0x00ff, // b
		  0x00ff,
		  0x00ff,
		  0x00ba
	};
	
	protected static final int[] noblue = {
		0x001D,
		0x001d,
		0xe2ff,
		0xe2ff,
		0x001D,
		0xe2ff,
		0x001d,
		0xe2ff,
		0x0000,
		0x0000,
		0x0000,
		0x0000
	};
	
	protected static final int[] red
	   = { 0x001D,
		0x96B3,
		0x4C69,
		0xe2ff,
		0x0000,
		0x0000,
		0x0000,
		0x0000,
		0x0000,
		0x0000,
		0x0000,
		0x0000
	};
	protected static final int[] blue
	   = { 
		0x0000,
		0x0000,
		0x0000,
		0x0000,
		0x0000,
		0x0000,
		0x0000,
		0x0000,
		0x001D,
		0x96B3,
		0x4C69,
		0xe2ff,
	};
	protected static final int[] green
	   = {
		0x0000,
		0x0000,
		0x0000,
		0x0000,
		0x001d,
		0x96b3,
		0x4c69,
		0xe2ff,
		0x0000,
		0x0000,
		0x0000,
		0x0000
	};
	protected static final int[] bw
	   = {
		0x001D,
		0x96B3,
		0x4C69,
		0xe2ff,
		0x001d,
		0x96b3,
		0x4c69,
		0xe2ff,
		0x001d,
		0x96b3,
		0x4c69,
		0xe2ff
	};
	protected static final int[] negative = {
		0xffff,
		0xffff,
		0x0000,
		0x0000,
		0xffff,
		0x0000,
		0xffff,
		0x0000,
		0xff00,
		0xff00,
		0xff00,
		0xff00		
	};
	
	public static int[][] colors = {
		null, null, null, null,
		red, green, bw,
		null, sepia, null,
		noblue, blue, null
	};
	
	protected int SCR_COUNT = 12;

	protected Context context;
	protected String gnDir;
	protected String selectorPath;
	protected String workingColorPath;
	protected boolean exynos4212;

	protected static final int[][] ACTIVATE_SCR = {{0x0001, 0x0040 }};
	
	public ScreenControl(Context context) {
		this.context = context;
	}		

	protected void writeLine(String filename, String data) {		
		File out = new File(filename);
		try {
			FileOutputStream stream = new FileOutputStream(out);
			stream.write((data+"\n").getBytes());
			stream.close();
			GalacticNight.log("Wrote "+data+" to "+filename);
		} catch (FileNotFoundException e) {
			GalacticNight.log("error "+filename+" "+e);
		} catch (IOException e) {
			GalacticNight.log("error "+filename+" "+e);
		}
	}
	
	protected void selectMode(int mode) {
		writeLine(selectorPath, ""+mode);
	}
	
	protected void saveMode(int mode) {
		android.provider.Settings.System.putInt(context.getContentResolver(),
				"screen_mode_setting", mode);
	}
	
	public static String getSystemDevice() {
		try {
			Process p;

			p = Runtime.getRuntime().exec("mount");
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line;
			Pattern pat = Pattern.compile("^([^\\s]+)\\s+(on\\s+)?\\/system\\s");
			while(null != (line = reader.readLine())) {
				Matcher m = pat.matcher(line);
				if (m.find()) {
					p.destroy();
					return m.group(1);
				}
			}
		} catch (IOException e) {}
	
		return null;
	}
	
	protected int[][] getTweak(int setting) {
		if (colors[setting] != null)
			return getTweak(colors[setting]);
		else if (setting == REVERSE)
			return getReverseTweak(new File(workingColorPath));
		else
			return null;
	}
	
	private int getInvertedPosition(int pos) {
		return pos / 8 * 8 + (7 - (pos % 8));
	}
	
	protected int[][] getReverseTweak(File f) {
		int[]c = new int[SCR_COUNT];		

		try {
			BufferedReader reader = new BufferedReader( new FileReader(f));
			String line;
			Pattern pat = Pattern.compile("\\s*0[xX]([a-fA-F0-9]+)\\s*,0[xX]([a-fA-F0-9]+).*");
			while(null != (line = reader.readLine())) {
				Matcher m = pat.matcher(line);
				if (m.find()) {
					int register = Integer.parseInt(m.group(1),16);
					int value = Integer.parseInt(m.group(2),16);

					if (0xC8 <= register && register <= 0xD3) {
						int pos = getInvertedPosition(2*(register-0xC8));
						GalacticNight.log(""+(register-0xc8)+" -> "+pos+" "+String.format("%04x", value));
						if (pos % 2 == 0) {
							c[pos/2] |= (value >> 8) << 8;
						}
						else if (pos % 2 == 1) {
							c[pos/2] |= ( value >> 8 );
						}
						pos = getInvertedPosition(2*(register-0xC8)+1);
						GalacticNight.log(""+(register-0xc8)+" + -> "+pos);
						if (pos % 2 == 0) {
							c[pos/2] |= ( value & 0xFF) << 8;
						}
						else if (pos % 2 == 1) {
							c[pos/2] |= ( value & 0xFF );
						}
					}
				}
			}
		} catch (IOException e) {
			return getTweak(negative);
		}
		
		return getTweak(c);
	}

	protected int[][] getTweak(int[] c) {
		int[][] tweak = new int[c.length][2];
		for (int i = 0; i < c.length ; i++) {
			tweak[i][0] = 0xc8 + i;
			tweak[i][1] = c[i];
		}
		return tweak;
	}

	protected boolean copyFile(String src, String dest) {
		try {
			copyFile(new File(src), new File(dest));
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}
	
	protected void copyFile(File src, File dest) throws IOException {
		GalacticNight.log("Copying "+src+" to "+dest);
		byte[] buffer = new byte[2048]; 
		FileInputStream in = new FileInputStream(src);
		dest.createNewFile();
		FileOutputStream out = new FileOutputStream(dest);
		
		int didRead;
		
		while(0 <= (didRead = in.read(buffer))) {
			out.write(buffer, 0, didRead);
		}
		in.close();
		out.close();
	}
		
	
	protected boolean checksum(File f, MessageDigest md) {
		BufferedReader reader;
		try {
			reader = new BufferedReader( new FileReader(f));
		} catch (FileNotFoundException e1) {
			return false;
		}
		
		Pattern pat = Pattern.compile("\\s*0[xX]([a-fA-F0-9]+)\\s*,0[xX]([a-fA-F0-9]+)");
		String line;
		try {
			while(null != (line = reader.readLine())) {
				Matcher m = pat.matcher(line);
				if (m.find()) {
					int hex1 = Integer.parseInt(m.group(1),16);
					int hex2 = Integer.parseInt(m.group(2),16);
					md.update((byte) (hex1 >> 8));
					md.update((byte) (hex1 & 0xff));
					md.update((byte) (hex2 >> 8));
					md.update((byte) (hex2 & 0xff));
				}
			}
		} catch (NumberFormatException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		
		return true;		
	}
	
	protected String checksum(String filename) {
		File in = new File(filename);
		
		try {
			MessageDigest md = MessageDigest.getInstance("md5");
			
			if (!checksum(in, md)) {
				return null;
			}
			
			byte[] digest = md.digest();
			String s = "";
			for(int i=0; i<digest.length; i++) {
				s += String.format("%02x", (int)(digest[i]&0xFF));
			}
			return s;
		} catch (NoSuchAlgorithmException e) {
			GalacticNight.log(""+e);
			return null;
		}
	}
	
	public void set(int i) {
	}
	
	public boolean deemInstalled() {
		return true;
	}
	
	public boolean isAlwaysInstalled() {
		return true;
	}
	
	public boolean install() {
		return true;
	}
	
	public void uninstall() {
	}
	
	public boolean supportsToggleOutdoor() {
		return false;
	}

	public static ScreenControl getScreenControl(Context c) {
		String cpu = getCPU();
		if (ScreenControlICS4210.detect(cpu)) {
			GalacticNight.log("Detected ICS 4210 mdnie");
			return new ScreenControlICS4210(c);
		}
		else if (ScreenControlICS4212.detect(cpu)) {
			GalacticNight.log("Detected ICS 4212 mdnie");
			return new ScreenControlICS4212(c);
		}
		else if (ScreenControlGB.detect(cpu)) {
			GalacticNight.log("Detected GB mdnie");
			return new ScreenControlGB(c);
		}
		else {
			GalacticNight.log("Failure in detecting");
			return null;
		}
	}
	
	static private String getCPU() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File("/proc/cpuinfo")));

			String line;
			Pattern pat = Pattern.compile("^Hardware\\s*:\\s*(.*)");
			while(null != (line = reader.readLine())) {
				Matcher m = pat.matcher(line);
				if (m.find()) {
					GalacticNight.log("CPU: "+m.group(1));
					return m.group(1);
				}
			}
		} catch (IOException e) {
		}
		
		return null;
	}

	protected int lookupTweak(int reg, int original, int[][] tweaks) {
		for (int i=0; i<tweaks.length; i++) {
			if (tweaks[i][0] == reg)
				return tweaks[i][1];
		}
		return original;
	}

	public boolean supportNatural() {
		return false;
	}

	public void updateService() {
	}

	public static String readLine(String filename) {
		BufferedReader reader;
		try {
			reader = new BufferedReader( new FileReader(new File(filename)));
			String line = reader.readLine();
			return line;
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		}

	}
	
	public boolean isICS() {
		return false;
	}
}
