package mobi.pruss.GalacticNight;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;

public class ScreenControlICS4210 extends ScreenControlICS {
	private static final int[][] activater ={ 
		{ 0x0001, 0x0040 } };
	
	public ScreenControlICS4210(Context context) {
		super(context);
	}
	
	static public boolean detect(String cpu) {
		if (FORCE_ICS_S2 && !FORCE_S3)
			return true;
		
		if (!cpu.endsWith("210"))
			return false;
		
		return detectICS() && !(new Device().is(Device.GALAXYS3)); 
	}

	@Override
	public void set(int setting) {
		int[][] tweak = getTweak(setting);
		
		if (tweak != null) {
			writeLine(OUTDOOR,"0");
			writeTweakICS(activater, tweak, null);
		}
		else if (setting == STANDARD || setting == MOVIE || setting == DYNAMIC) {
			setOSMode(setting);
		}
		else if (setting == OUTDOOR_ICS) {
			setOutdoor();
		}
	}
	

}
