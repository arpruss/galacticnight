package mobi.pruss.GalacticNight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Environment;
import android.provider.Settings.SettingNotFoundException;

public class ScreenControlGB extends ScreenControl {
	public static final String SELECTOR = "/sys/devices/virtual/mdnieset_ui/switch_mdnieset_ui/mdnieset_user_select_file_cmd";
	String systemDevice;
	
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
	private String unlockPartition;
	private String lockPartition;
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
	static final String LINK_ADJ_CHECKSUM_NOTE = "67ecdcc627d91564ad938106d334b401";
	static final String LINK_UI_CHECKSUM_NOTE = "63c08567bbc0acce68b771ac8a3a9333";
	
	public ScreenControlGB(Context context) {
		super(context);

		this.selectorPath = SELECTOR; 
		File gnDirFile = new File(Environment.getExternalStorageDirectory().getPath()+"/GalacticNight");
		gnDirFile.mkdir();
		gnDir = gnDirFile.getPath() + "/";
		workingColorPath = gnDir + SUBST_ADJ;
		
		valid = false;
		
		if (Root.runOne("chmod 666 "+selectorPath)) {
			systemDevice = getSystemDevice();
			
			if (systemDevice != null) {
				unlockPartition = "mount -o remount,rw "+systemDevice+" /system";
				lockPartition = "mount -o remount,ro "+systemDevice+" /system";
				valid = true;
			}
		}
		
	}

	@Override
	public void lock() {
		Root.runOne("chmod 664 "+selectorPath);
	}
	
	public static boolean detect(String cpu) {
		if (!cpu.endsWith("210"))
			return false;
		
		return (new File(SELECTOR).exists());
	}
	
	private boolean existsInMdnieDir(String name) {
		return new File(mdnieDir + name).exists();
	}
	
	@Override
	public boolean isAlwaysInstalled() {
		return false;
	}
	
	@Override
	public boolean deemInstalled() {
		return existsInMdnieDir(LINK_UI + origSuffix) || 
				existsInMdnieDir(LINK_ADJ + origSuffix);
	}
	
	@Override
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
	
	@Override
	public void uninstall() {
		selectMode(STANDARD);
		saveMode(STANDARD);
		
		Root.runOne(
				unlockPartition + " ; " +
				"mv "+mdnieDir+LINK_ADJ+origSuffix+" "+mdnieDir+LINK_ADJ+" ; "+
				"mv "+mdnieDir+LINK_UI+origSuffix+" "+mdnieDir+LINK_UI+" ; "+
				lockPartition);				
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
	
	@Override
	public void set(int setting) {
		int[][] tweak = getTweak(setting);
		
		if (tweak != null) {			
			saveMode(STANDARD);

			if (!tweak(mdnieDir+STANDARD_ADJ, gnDir+SUBST_ADJ, getTweak(setting))) {
				copyFile(mdnieDir+STANDARD_ADJ+origSuffix,gnDir+SUBST_UI);
				selectMode(STANDARD);
			}
			else {
				if (!tweak(mdnieDir+STANDARD_UI, gnDir+SUBST_UI, ACTIVATE_SCR)) {
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
	
	private boolean tweak(String source, String dest, int[][] tweaks) {
		File tmp = new File(dest+tmpSuffix);
		BufferedWriter writer = null;
		
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
						writer.write(String.format("0x%04x,0x%04x%s\n",
								reg,
								lookupTweak(reg, value, tweaks),
								m.group(3)));
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

	public void activate() {
		String value;
		
		try {
			value = ""+
				android.provider.Settings.System.getInt(context.getContentResolver(),
					"screen_mode_setting");
		} catch (SettingNotFoundException e) {
			value = "1";
		}
		
		File out = new File(selectorPath);
		try {
			FileOutputStream stream = new FileOutputStream(out);
			stream.write(value.getBytes());
			stream.close();
		} catch (FileNotFoundException e) {
			GalacticNight.log("error "+selectorPath+" "+e);
		} catch (IOException e) {
			GalacticNight.log("error "+selectorPath+" "+e);
		}
		
	}
	
	public boolean checksum() {
		String sum = checksum(mdnieDir+LINK_ADJ);
		if (sum == null || (0 != sum.compareTo(LINK_ADJ_CHECKSUM) &&
				0 != sum.compareTo(LINK_ADJ_CHECKSUM_NOTE)))
			return false;
		sum = checksum(mdnieDir+LINK_UI);
		return sum != null && (0 == sum.compareTo(LINK_UI_CHECKSUM)||
				0 == sum.compareTo(LINK_UI_CHECKSUM_NOTE));
	}
	
	@Override
	public boolean isGB() {
		return true;
	}
}
