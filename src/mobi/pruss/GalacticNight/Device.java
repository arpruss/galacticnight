package mobi.pruss.GalacticNight;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;

public class Device {
	private int model;
	public static final int GENERIC = 0;
	public static final int KINDLE = 1;
	public static final int GALAXYS2 = 2;
	public static final int A43 = 3; // Archos 43
	public static final int GALAXYS3 = 4;
	public static final int NEXUS7 = 5;
	public static final int GALAXY7 = 6;

	public static final String[] GALAXYS2_MODELS = {
		"GT-I9100", "GT-I9108", "SGH-I777",
		"SGH-I927", "SGH-I727", "SGH-N033", "SGH-N034",
		"SHW-M250K", "SHW-M250L", "SGH-I927R", "SGH-I727R", 
		"SHW-M250S", "SPH-D710", "SGH-I989D", "SGH-T989"
	};
	
	public static final String[] GALAXYS3_MODELS =  {
              "GT-I9300", "SGH-I747", "SGH-I747M", "SHV-E210K", "SHV-E210L", 
              "SGH-T999V", "SC-06D", "SGH-I747R", "SHV-E210S", "SPH-L710", "SGH-T999",
              "SGH-T999D", "SCH-R530", "SCH-I535", "SGH-T999V"
        };

	public Device() {
		if (isKindle()) {
			model = KINDLE;
		}
		else if (in(Build.MODEL, GALAXYS2_MODELS)) {
			model = GALAXYS2;
		}
		else if (in(Build.MODEL, GALAXYS3_MODELS)) {
                        model = GALAXYS3;
                }
		else if (Build.MODEL.equals("A43")) {
			model = A43;
		}
		else if (Build.MODEL.equals("Nexus 7")) {
			model = NEXUS7;
		}
		else if (Build.MODEL.equals("GT-P6800") || Build.MODEL.equals("SCH-I815")) {
			model = GALAXY7;
		}
		else
		{
			model = GENERIC;
		}
		
		if (ScreenControlICS.FORCE_S3)
			model = GALAXYS3;
		else if (ScreenControlICS.FORCE_ICS_S2)
			model = GALAXYS2;
	}
	
	public boolean is(int model) {
		return this.model == model;
	}
	
	private static boolean isKindle() {
		return Build.MODEL.equalsIgnoreCase("Kindle Fire");		
	}
	
	private boolean in(String s, String[] list) {
		for (String a: list) {
			if (s.equalsIgnoreCase(a)) {
				return true;
			}
		}
		return false;
	}
}
