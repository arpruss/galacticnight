package mobi.pruss.GalacticNight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;

public class ScreenControlICS4212 extends ScreenControlICS {
	
	public ScreenControlICS4212(Context context) {
		super(context);
		
		exynos4212 = true;
	}
	
	static public boolean detect(String cpu) {
		if (!FORCE_ICS_S2 && FORCE_S3)
			return true;

		if (!cpu.endsWith("12"))
			return false;
		
		return detectICS(); // && (new Device().is(Device.GALAXYS3)); 
	}

	int[][] prefix = {
	{0x0000, 0x0000}, 
	{0x0008, 0x00a0}, 
	{0x0030, 0x0000}, 
	{0x0092, 0x0020}, 
	{0x0093, 0x0020}, 
	{0x0094, 0x0020}, 
	{0x0095, 0x0020}, 
	{0x0096, 0x0020}, 
	{0x0097, 0x0020}, 
	{0x0098, 0x1000}, 
	{0x0099, 0x0100}, 
	};
	
	int[][] suffix = {
	{0x0000, 0x0001},
	{0x001f, 0x0080},
	{0x0020, 0x0000},
	{0x0021, 0x0290},
	{0x0022, 0x20a0},
	{0x0023, 0x30b0},
	{0x0024, 0x40c0},
	{0x0025, 0x50d0},
	{0x0026, 0x60e0},
	{0x0027, 0x70f0},
	{0x0028, 0x80ff},
	{0x00ff, 0x0000},
	};


	int[] mapFrom4212To4210 = {
		4, 3,   //RrCr
		12, 11, //RgCg
		20, 19, //RbCb
		2,   5, //GrMr
		10, 13, //GgMg
		18, 21, //GbMb
		1, 6,   //BrYr
		9, 14,  //BgYg
		17, 22, //BbYb
		0, 7,   //KrWr
		8, 15,  //KgWg
		16, 23  //KbWb		
	};
	
	private
	int[][] convertTweak(int[][] oldTweak) {
		int[][] newTweak = new int[SCR_COUNT][2];
		
		for (int i=0; i<SCR_COUNT; i++) {
			newTweak[i][0] = 0xE1 + i;
			int pos = mapFrom4212To4210[2 * i];
			
			int value = lookupTweak(0xC8 + pos/2, -1, oldTweak);
			if(0<=value) {
				if (pos % 2 == 0) {
					value >>= 8;
				}
				else {
					value &= 0xFF;
				}
				newTweak[i][1] = value << 8;
			}
			pos = mapFrom4212To4210[2 * i + 1];
			value = lookupTweak(0xC8 + pos/2, -1, oldTweak);
			if(0<=value) {
				if (pos % 2 == 0) {
					value >>= 8;
				}
				else {
					value &= 0xFF;
				}
				newTweak[i][1] |= value;
			}
		}			
		
		return newTweak;
	}
	
	@Override
	protected
	int[][] getTweak(int setting) {
		int[][] oldTweak;
		
		if (setting == REVERSE) {
			return getReverseTweak(new File(workingColorPath));
		}
		
		oldTweak = super.getTweak(setting);
		if (oldTweak == null)
			return null;
		
		return convertTweak(oldTweak);
	}
	
	@Override
	protected int[][] getReverseTweak(File f) {
		int[][] newTweak = new int[SCR_COUNT][2];		

		try {
			BufferedReader reader = new BufferedReader( new FileReader(f));
			String line;
			Pattern pat = Pattern.compile("\\s*0[xX]([a-fA-F0-9]+)\\s*,0[xX]([a-fA-F0-9]+).*");
			while(null != (line = reader.readLine())) {
				Matcher m = pat.matcher(line);
				if (m.find()) {
					int register = Integer.parseInt(m.group(1),16);
					int value = Integer.parseInt(m.group(2),16);

					if (0xE1 <= register && register < 0xE1 + SCR_COUNT) {
						newTweak[register-0xE1][0] = register;
						newTweak[register-0xE1][1] = ((value & 0xFF) << 8) | (value >> 8);
					}
				}
			}
		} catch (IOException e) {
			return convertTweak(getTweak(negative));
		}
		
		return newTweak;
	}
	
	@Override
	public void set(int setting) {
		int[][] tweak = getTweak(setting);
		
		if (tweak != null) {
			GalacticNight.log("writing tweak");
			writeTweak(prefix, tweak, suffix);
		}
		else if (setting == STANDARD || setting == MOVIE || setting == DYNAMIC
				|| setting == NATURAL) {
			tuningControlWrite("0");
			int mode;
			if (setting == MOVIE) {
				mode = 3;
			}
			else if (setting == NATURAL) {
				mode = 2;
			}
			else {
				mode = setting;
			}
			GalacticNight.log("Setting to "+mode+" ("+setting+")");
			
			saveMode(mode); 
			selectMode(mode);
			(new File(workingColorPath)).delete();
		}
	}
	
	@Override
	public boolean supportNatural() {
		return true;
	}
}
