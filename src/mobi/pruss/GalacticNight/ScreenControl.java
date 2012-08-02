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
	static final int DYNAMIC = 0;
	static final int STANDARD = 1;
	static final int MOVIE = 2;
	static final int RED = 3;
	static final int GREEN = 4;
	static final int BW = 5;
	static final int REVERSE = 6;
	static final int SEPIA = 7;
	static final int OUTDOOR = 8;
	static final int NOBLUE = 9;
	
	static final String origSuffix = ".orig";
	static final String[] srcUI = { "mdnie_tune_ui_dynamic_mode"+origSuffix,
		"mdnie_tune_ui_standard_mode",
		"mdnie_tune_ui_movie_mode"
	};
	static final String[] srcAdj = { "mdnie_tune_dynamic_mode"+origSuffix,
		"mdnie_tune_standard_mode",
		"mdnie_tune_movie_mode"
	};
	static final String tmpSuffix = ".tmp";
	private String gnDir;
	private String unlockPartition;
	private String lockPartition;
	static final String selectorFile = "/sys/devices/virtual/mdnieset_ui/switch_mdnieset_ui/mdnieset_user_select_file_cmd";
	static final String mdnieDir = "/system/etc/";
	static final String[] builtinModes = { "movie", "standard", "dynamic"};
	static final String OUTDOOR_UI = "mdnie_tune_outdoor_mode"; 

	static final int LINK_MODE = 0;
	static final String LINK_UI = "mdnie_tune_ui_dynamic_mode";
	static final String LINK_ADJ = "mdnie_tune_dynamic_mode";
	static final String DYNAMIC_UI = "mdnie_tune_ui_dynamic_mode";
	static final String DYNAMIC_ADJ = "mdnie_tune_dynamic_mode";
	static final String STANDARD_UI = "mdnie_tune_ui_standard_mode";
	static final String STANDARD_ADJ = "mdnie_tune_standard_mode";
	static final String MOVIE_UI = "mdnie_tune_ui_standard_mode";
	static final String MOVIE_ADJ = "mdnie_tune_standard_mode";
	static final String SUBST_UI = "mdnie_tune_ui";
	static final String SUBST_ADJ = "mdnie_tune";
	
	static final String LINK_UI_CHECKSUM = "e2276e0176e17890788ccbf4909e97c4";
	static final String LINK_ADJ_CHECKSUM = "87fc844c950c1e842f264720f9d4b76c";
	
	static final int[] sepia 
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
	
	static final int[] noblue = {
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
	static final int[] blue
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
		
		File gnDirFile = new File(Environment.getExternalStorageDirectory().getPath()+"/GalacticNight");
		gnDirFile.mkdir();
		gnDir = gnDirFile.getPath() + "/";
		
		valid = false;
		
		if (Root.runOne("chmod 666 "+selectorFile)) {
			systemDevice = getSystemDevice();
			
			if (systemDevice != null) {
				unlockPartition = "mount -o remount,rw "+systemDevice+" /system";
				lockPartition = "mount -o remount,ro "+systemDevice+" /system";
				valid = true;
			}
		}
		
	}
	
	private boolean existsInMdnieDir(String name) {
		return new File(mdnieDir + name).exists();
	}
	
	public boolean canUninstall() {
		return existsInMdnieDir(LINK_UI + origSuffix) || 
				existsInMdnieDir(LINK_ADJ + origSuffix);
	}
	
	public boolean install() {
		if (!checksum()) {
			GalacticNight.log("dynamic files fail checksum");
			return false;
		}
		
		try {
			copyFile(new File(mdnieDir+STANDARD_ADJ), new File(gnDir+SUBST_ADJ));
			copyFile(new File(mdnieDir+STANDARD_UI), new File(gnDir+SUBST_UI));
		} catch (IOException e) {
			GalacticNight.log("cannot copy dynamic tuning file: "+e);
		}
		
		selectMode(STANDARD);
		saveMode(STANDARD);

		if (! Root.runOne(
				unlockPartition + " && " +
				"mv "+mdnieDir+LINK_ADJ+" "+mdnieDir+LINK_ADJ+origSuffix +" && "+
				"mv "+mdnieDir+LINK_UI+" "+mdnieDir+LINK_UI+origSuffix + " && "+
				"ln -s \""+gnDir+SUBST_ADJ+"\" "+mdnieDir+LINK_ADJ + " && " +
				"ln -s \""+gnDir+SUBST_UI+"\" "+mdnieDir+LINK_UI + " && "+
				lockPartition)) {
			uninstall();
			return false;
		}
		
		return true;				
	}
	
	public void uninstall() {
		selectMode(STANDARD);
		saveMode(STANDARD);
		
		Root.runOne(
				unlockPartition + " ; " +
				"mv "+mdnieDir+LINK_ADJ+origSuffix+" "+mdnieDir+LINK_ADJ+" ; "+
				"mv "+mdnieDir+LINK_UI+origSuffix+" "+mdnieDir+LINK_UI+" ; "+
				lockPartition);				
	}
	
	private void selectMode(int mode) {
		File out = new File(selectorFile);
		try {
			FileOutputStream stream = new FileOutputStream(out);
			stream.write((""+mode).getBytes());
			stream.close();
		} catch (FileNotFoundException e) {
			GalacticNight.log("error "+selectorFile+" "+e);
		} catch (IOException e) {
			GalacticNight.log("error "+selectorFile+" "+e);
		}
	}
	
	private void saveMode(int mode) {
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
	
	public void set(int setting) {
		int[][] tweak = getTweak(setting);
		
		if (tweak != null) {			
			saveMode(STANDARD);

			if (!tweak(mdnieDir+STANDARD_ADJ, gnDir+SUBST_ADJ, getTweak(setting), false)) {
				copyFile(mdnieDir+STANDARD_ADJ+origSuffix,gnDir+SUBST_UI);
				selectMode(STANDARD);
			}
			else {
				if (!tweak(mdnieDir+STANDARD_UI, gnDir+SUBST_UI, new int[][] {{0x0001, 0x0040 }}, true)) {
					copyFile(mdnieDir+STANDARD_UI+origSuffix,gnDir+SUBST_UI);
					copyFile(mdnieDir+STANDARD_ADJ+origSuffix,gnDir+SUBST_ADJ);
					selectMode(STANDARD);
				}
				else {
					selectMode(LINK_MODE);					
				}
			}
		}
		else if (setting == STANDARD || setting == MOVIE || setting == DYNAMIC) {
			saveMode(STANDARD);

			if (copyFile(mdnieDir+srcAdj[setting],
					gnDir+SUBST_ADJ) && 
				copyFile(mdnieDir+srcUI[setting],
						gnDir+SUBST_UI)) {
				saveMode(setting);
				selectMode(setting);
			}
			else {
				selectMode(STANDARD);
			}					
		}
		else if (setting == OUTDOOR) {
			saveMode(STANDARD);
			
			if (copyFile(mdnieDir+srcAdj[STANDARD], gnDir+SUBST_ADJ) &&
					copyFile(mdnieDir+OUTDOOR_UI, gnDir+SUBST_UI)) {
				selectMode(DYNAMIC);
			}
			else {
				set(STANDARD);
			}
		}
	}
	
	private int[][] getTweak(int setting) {
		if (setting == RED)
			return getTweak(red);
		else if (setting == GREEN)
			return getTweak(green);
		else if (setting == BW)
			return getTweak(bw);
		else if (setting == SEPIA)
			return getTweak(sepia);
		else if (setting == NOBLUE)
			return getTweak(noblue);
		else if (setting == REVERSE)
			return getReverseTweak(new File(gnDir + SUBST_ADJ));
		else
			return null;
	}
	
	private int getInvertedPosition(int pos) {
		return pos / 8 * 8 + (7 - (pos % 8));
	}
	
	private int[][] getReverseTweak(File f) {
		int[]c = new int[12];		

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
		} catch (NumberFormatException e) {
			return null;
		} catch (IOException e) {
			GalacticNight.log("ReverseTweak "+e);
			return null;
		}
		
		return getTweak(c);
	}

	private int[][] getTweak(int[] c) {
		int[][] tweak = new int[c.length][2];
		for (int i = 0; i < c.length ; i++) {
			tweak[i][0] = 0xc8 + i;
			tweak[i][1] = c[i];
		}
		return tweak;
	}

	private boolean copyFile(String src, String dest) {
		try {
			copyFile(new File(src), new File(dest));
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}
	
	private void copyFile(File src, File dest) throws IOException {
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
		
	private boolean tweak(String source, String dest, int[][] tweaks, boolean ui) {
		File tmp = new File(dest+tmpSuffix);
		BufferedWriter writer = null;
		
//		if (!ui) {
//			try {
//				tmp.createNewFile();
//				writer = new BufferedWriter( new FileWriter(tmp));
//				
//				for (int i = 0; i<tweaks.length; i++) {
//					writer.write(String.format("0x%04x,0x%04x,\n", tweaks[i][0], tweaks[i][1]));
//				}
//				writer.close();
//				writer = null;
//			}
//			catch (IOException e) {
//
//				if (writer != null) {
//					try {
//						writer.close();
//					} catch (IOException e1) {
//					}
//				}
//
//				tmp.delete();
//				GalacticNight.log("error writing tmp "+e);
//				return false;
//			}
//		}
//		else 
		{
			File src = new File(source);
			GalacticNight.log("tweaking to "+tmp);
			BufferedReader reader = null;
			try {
				reader = new BufferedReader( new FileReader(src));
				tmp.createNewFile();
				writer = new BufferedWriter( new FileWriter(tmp));
				
				Pattern pat = Pattern.compile("\\s*0[xX]([a-fA-F0-9]+)\\s*,0[xX]([a-fA-F0-9]+)(.*)");
				String line;
				while(null != (line = reader.readLine())) {
					Matcher m = pat.matcher(line);
					if (m.find()) {
						int reg = Integer.parseInt(m.group(1),16);
						int value = Integer.parseInt(m.group(2),16);
						if (true /*reg <= 0xd3 || ui*/) {
							writer.write(String.format("0x%04x,0x%04x%s\n",
									reg,
									lookupTweak(reg, value, tweaks),
									m.group(3)));
						}
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
		}
		
		return tmp.renameTo(new File(dest));
	}

	private int lookupTweak(int reg, int original, int[][] tweaks) {
		for (int i=0; i<tweaks.length; i++) {
			if (tweaks[i][0] == reg)
				return tweaks[i][1];
		}
		return original;
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
	
	private String checksum(String filename) {
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
	
	public boolean checksum() {
		String sum = checksum(mdnieDir+LINK_ADJ);
		if (sum == null || 0 != sum.compareTo(LINK_ADJ_CHECKSUM))
			return false;
		sum = checksum(mdnieDir+LINK_UI);
		return sum != null && 0 == sum.compareTo(LINK_UI_CHECKSUM);
	}
}
