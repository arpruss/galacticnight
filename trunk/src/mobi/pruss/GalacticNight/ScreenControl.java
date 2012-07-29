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
import android.os.Environment;
import android.provider.Settings.SettingNotFoundException;

public class ScreenControl {
	Context context;
	boolean valid;
	String systemDevice;
	static final int NORMAL = 0;
	static final int RED = 1;
	static final int GREEN = 2;
	static final int BW = 3;
	static final int REVERSED_RED = 4;
	static final int REVERSED_GREEN = 5;
	static final int REVERSED_BW = 6;
	
	String tmpSuffix = ".GalacticNight.tmp";
	private String backupDir;
	static final String selectorFile = "/sys/devices/virtual/mdnieset_ui/switch_mdnieset_ui/mdnieset_user_select_file_cmd";
	static final String mdnieDir = "/system/etc/";
	static final String[] modes = {"movie", "standard", "dynamic"};
	static final int DYNAMIC = 0;
	static final int STANDARD = 1;
	static final int MOVIE = 2;

	static final int UI = 0;
	static final int ADJ = 1;
	static final String[][] files = { {"mdnie_tune_ui_dynamic_mode", "mdnie_tune_dynamic_mode"},
		{"mdnie_tune_ui_standard_mode", "mdnie_tune_standard_mode"},
		{null, "mdnie_tune_movie_mode"} };
	static final String[][] checksums = 
		{{ "e2276e0176e17890788ccbf4909e97c4", "87fc844c950c1e842f264720f9d4b76c"},
		{ "5775097528aaf50efe1f58d2b3e577ee", "23af263efb2f7f94586c71af256f86ec"},
		{ null, "7d643e4da5235ea3606b4badf770e27b"}};
	

	static final int[] normal_dynamic 
	   = { 0x0000, 0x0000, 0xffff, 0xffff, // R
		   0x0000, 0xffff, 0x0000, 0xffff, //G
		   0x00ff, 0x00ff, 0x00ff, 0x00ff  // B
	};
	static final int[] normal_standard 
	   = { 0x0000, 0x0000, 0xffff, 0xffff, // R
		   0x0000, 0xffff, 0x0000, 0xffff, //G
		   0x00ff, 0x00ff, 0x00ff, 0x00ff  // B
	};
	static final int[] normal_movie 
	   = { 0x0000, 0x6000, 0xf0f0, 0xf0ff, // R
		   0x0050, 0xd8f0, 0x2500, 0xf0fb, //G
		   0x00f0, 0x20f0, 0x28f0, 0x00f0  // B
	};
	static final int[] red
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
	static final int[] green
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
	static final int[] bw
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
	
	public ScreenControl(Context context) {
		this.context = context;
		
		File backupDirFile = new File(Environment.getExternalStorageDirectory().getPath()+"/GalacticNight_backup");
		backupDirFile.mkdir();
		backupDir = backupDirFile.getPath()+"/";
		
		if (!unlock()) {
			valid = false;
		}
		else {
			valid = true;
		}
	}
	
	public boolean unlock() {
		systemDevice = getSystemDevice();
		
		if (systemDevice == null) {
			GalacticNight.log("cannot find /system");
			return false;
		}
		
		GalacticNight.log("system device "+systemDevice);
		
		String cmd = "mount -o remount,rw "+systemDevice+" /system; "+
				"chmod 666 "+getFileList();
		if (!Root.runOne(cmd)) {
			GalacticNight.log("Error in "+cmd);
			lock();
			return false;
		}
		else {
			GalacticNight.log("Successful run");
			if (checkValidity() && makeBackups()) {
				return checksum(); // TODO: allow override
			}
			else {
				GalacticNight.log("Failed validity check or backup");
				lock();
				return false;
			}
		}
	}
	
	public void lock() {
		if (systemDevice != null)
			Root.runOne("mount -o remount,ro "+systemDevice+" /system; "+
					"chmod 644 "+getFileList());
	}
	
	private String getFileList() {
		String list = selectorFile;
		for (int i = 0; i < modes.length ; i++) {
			if (files[i][UI] != null)
				list += " "+mdnieDir + files[i][UI];
			list += " " + mdnieDir + files[i][ADJ];
		}

		return list;
	}

	private boolean checkValidity() {
		GalacticNight.log("Checking validity");
		for (int i=0; i<modes.length; i++) {
			if (files[i][UI] != null) 
				if (!checkFileWriteable(mdnieDir+files[i][UI], true))
					return false;
			if (!checkFileWriteable(mdnieDir+files[i][ADJ], true))
				return false;
		}
		return checkFileWriteable(selectorFile, false);
	}
	
	private boolean makeBackups() {		
		for (int i=0; i<modes.length; i++) {
			if (files[i][UI] != null)
				if (! makeBackup(files[i][UI]))
					return false;
			if (! makeBackup(files[i][ADJ]))
				return false;
		}
		
		return true;
	}
	
	private static boolean checkFileWriteable(String filename, boolean tmp) {
		GalacticNight.log("Checking writeability of "+filename);
		File f = new File(filename);
		if (! f.exists() || ! f.canWrite()) {
			GalacticNight.log("Not writeable");
			return false;
		}
		if (tmp) {
			File t = new File(filename+tmp);
			if (t.exists()) {
				if (!t.canWrite()) {
					GalacticNight.log("Tmp not writeable");
					return false;
				}
				else {
					return true;
				}
			}

			try {
				if (!t.createNewFile()) {
					GalacticNight.log("Tmp not creatable");
					return false;
				}
				t.delete();
				return true;
			} catch (IOException e) {
				GalacticNight.log("Tmp not creatable");
				return false;
			}
		}
		else {
			return true;
		}
	}
	
	public static String getSystemDevice() {
		try {
			Process p;

			p = Runtime.getRuntime().exec("mount");
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line;
			Pattern pat = Pattern.compile("([^\\s]+)\\s+\\/system\\s");
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
	
	final int[] colors(int mode, int setting) {
		if (setting == NORMAL) {
			if (mode == STANDARD)
				return normal_standard;
			else if (mode == DYNAMIC)
				return normal_dynamic;
			else if (mode == MOVIE)
				return normal_movie;
		}
		else if (setting == RED)
			return red;
		else if (setting == GREEN) {
			GalacticNight.log("green");
			return green;
		}
		else if (setting == BW)
			return bw;
		else if (setting == REVERSED_RED)
			return reverse(red, true, false, false);
		else if (setting == REVERSED_GREEN)
			return reverse(green, false, true, false);
		else if (setting == REVERSED_BW)
			return reverse(bw, true, true, true);

		return null;
	}

	private int[] reverse(int[] c, boolean r, boolean g, boolean b) {
		int[] out = new int[red.length];
		for (int i = 0 ; i < 3; i++) {
			for (int j = 0; j<4; j++) {
				if ((i == 0 && r) || (i == 1 && g) || (i == 2 && b)) 
					out[4*i + j] = reverse(c[4*i + j]);
				else
					out[4*i + j] = c[4*i + j];
			}
		}
		return out;
	}

	private int reverse(int v) {
		return (0xFF-(v & 0xFF)) | (0xFF00 - (v & 0xFF00));
	}

	public void set(int setting) {
		if (setting == NORMAL) {
			for (int i=0; i<modes.length; i++) 
				if (!restoreBackup(i)) 
					set(i, colors(i, NORMAL), false);
			activate();
		}
		else {
			for (int i=0; i<modes.length; i++) {
				if (!set(i, colors(i, setting), true))
					restoreBackup(i);
			}
			activate();
		}
	}

	private boolean makeBackup(String file) {
		File out = new File(backupDir + file);
		if (out.exists())
			return true;
		
		GalacticNight.log("Backing up "+file);
		try {
			copyFile(new File(mdnieDir + file), out);
		} catch (IOException e) {
			GalacticNight.log(""+e);
			out.delete();
			return false;
		}
		
		return true;
	}
	
	private void copyFile(File src, File dest) throws IOException {
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
	
	private boolean restoreBackup(int i) {
		boolean success = true;
		if (files[i][UI] != null) {
			success = restoreBackup(files[i][UI]);
		}
		return restoreBackup(files[i][ADJ]) && success;
	}
	
	private boolean restoreBackup(String filename) {
		GalacticNight.log("Restoring "+filename);
		File tmp = new File(mdnieDir + filename + tmpSuffix);
		try {
			copyFile(new File(backupDir + filename), tmp);
		} catch (IOException e) {
			tmp.delete();
			GalacticNight.log("Failing copying "+e);
			return false;
		}
		if (!tmp.renameTo(new File(mdnieDir + filename))) {
			GalacticNight.log("Failing renaming");
			return false;
		}
		else {
			return true;
		}
	}

	private boolean set(int mode, int[] colors, boolean setSCR) {
		GalacticNight.log("set "+mode+ " "+colors[0]);
		
		if (colors == null)
			return false;
		
		int[][] colorTweak = new int[colors.length][2];
		
		for (int i = 0; i<colors.length; i++) {
			colorTweak[i][0] = 0xc8 + i; 
			colorTweak[i][1] = colors[i];
		}
		
		if (!setSCR && files[mode][UI] != null &&
				!tweak(files[mode][UI], new int[][] {{0x0001, 0x0000 }})) 
			return false;
		
		if (!tweak(files[mode][ADJ], colorTweak)) 
			return false;
		
		if (setSCR && files[mode][UI] != null && 
				!tweak(files[mode][UI], new int[][] {{0x0001, 0x0040 }})) 
			return false;
		
		return true;
	}
	
	private boolean tweak(String filename, int[][] tweaks) {
		File official = new File(mdnieDir + filename);
		File tmp = new File(mdnieDir + filename + tmpSuffix);
		GalacticNight.log("tweaking to "+tmp);
		BufferedReader reader = null;
		BufferedWriter writer = null;
		try {
			reader = new BufferedReader( new FileReader(official));
			tmp.createNewFile();
			writer = new BufferedWriter( new FileWriter(tmp));
			
			Pattern pat = Pattern.compile("\\s*0[xX]([a-fA-F0-9]+)\\s*,0[xX][a-fA-F0-9]+(.*)");
			String line;
			while(null != (line = reader.readLine())) {
				Matcher m = pat.matcher(line);
				if (m.find()) {
					int hexIn = Integer.parseInt(m.group(1),16);
					int tweakLine = checkTweak(tweaks, hexIn);
					if (0 <= tweakLine) { 
						writer.write(String.format("0x%04x,0x%04x%s\n",hexIn,
								tweaks[tweakLine][1], m.group(2)));
					}
					else
						writer.write(line+"\n");
				}
				else {
					writer.write(line+"\n");
				}
			}
			writer.close();
			writer = null;
			reader.close();
			reader = null;
			
		} catch (IOException e) {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
			
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e1) {
				}
			}
			
			tmp.delete();
			GalacticNight.log("error writing tmp "+e);
			return false;
		}
		
		return tmp.renameTo(official);
	}

	private int checkTweak(int[][] tweaks, int hexIn) {
		for (int i=0; i<tweaks.length; i++) {
			if (tweaks[i][0] == hexIn)
				return i;
		}
		return -1;
	}

	public void activate() {
		String value;
		
		try {
			value = ""+
				android.provider.Settings.System.getInt(context.getContentResolver(),
					"screen_mode_setting");
		} catch (SettingNotFoundException e) {
			value = "1";
		}
		
		File out = new File(selectorFile);
		try {
			FileOutputStream stream = new FileOutputStream(out);
			stream.write(value.getBytes());
			stream.close();
		} catch (FileNotFoundException e) {
			GalacticNight.log("error "+selectorFile+" "+e);
		} catch (IOException e) {
			GalacticNight.log("error "+selectorFile+" "+e);
		}
		
	}
	
	private boolean checksum(File f, MessageDigest md) {
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
	
	private boolean checksum(int mode, int fType) {
		File in = new File(backupDir + files[mode][fType]);
		
		try {
			MessageDigest md = MessageDigest.getInstance("md5");
			
			if (!checksum(in, md)) {
				return false;
			}
			
			byte[] digest = md.digest();
			String s = "";
			for(int i=0; i<digest.length; i++) {
				s += String.format("%02x", (int)(digest[i]&0xFF));
			}
			if ( s.compareTo(checksums[mode][fType]) != 0) {
				GalacticNight.log("mismatch "+in+" "+s+" "+checksums[mode][fType]);
			}
			return 0 == s.compareTo(checksums[mode][fType]);
		} catch (NoSuchAlgorithmException e) {
			GalacticNight.log(""+e);
			return false;
		}
	}
	
	public boolean checksum() {
		for (int i=0; i<modes.length; i++) {
			GalacticNight.log("checking "+modes[i]);
			if (files[i][UI] != null && !checksum(i,UI)) {
					GalacticNight.log("failed checksum at "+files[i][UI]);
					return false;
			}
			if (!checksum(i,ADJ)) {
				GalacticNight.log("failed checksum at "+files[i][ADJ]);
				return false;
			}
		}
		
		return true;
	}
}
