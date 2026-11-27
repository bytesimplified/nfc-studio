package com.nfcstudio.app.automation;
import java.util.Arrays;
public final class ActionValidator {
    private ActionValidator() {
    }
    public static final String[] TYPES= {
        "Open app","Open link","Media volume","Ring mode","Do Not Disturb","Brightness","Rotation","Flashlight","Vibrate","Show message","Set alarm","Set timer","Wi-Fi controls","Bluetooth controls","HTTPS webhook","Delay"
    };
    public static final String[] HINTS= {
        "Choose an installed app","https://… or geo:…","0–100","normal / silent / vibrate","on / off","1–100","auto / locked","on / off","milliseconds (1–3000)","Message","HH:mm (24-hour)","Seconds (1–86400)","No value needed","No value needed","https://… (POST)","Seconds (0–30)"
    };
    public static int number(String v,int lo,int hi)throws Exception {
        int n=Integer.parseInt(v);
        if(n<lo||n>hi)throw new Exception("Value must be "+lo+"–"+hi);
        return n;
    }
    static void oneOf(String v,String...opts)throws Exception {
        if(!Arrays.asList(opts).contains(v))throw new Exception("Use "+String.join(" / ",opts));
    }
    public static void validate(String t,String v)throws Exception {
        switch(t) {
            case "Media volume":number(v,0,100);
            break;
            case "Brightness":number(v,1,100);
            break;
            case "Vibrate":number(v,1,3000);
            break;
            case "Set timer":number(v,1,86400);
            break;
            case "Delay":number(v,0,30);
            break;
            case "Ring mode":oneOf(v,"normal","silent","vibrate");
            break;
            case "Do Not Disturb":case "Flashlight":oneOf(v,"on","off");
            break;
            case "Rotation":oneOf(v,"auto","locked");
            break;
            case "Set alarm":if(!v.matches("([01][0-9]|2[0-3]):[0-5][0-9]"))throw new Exception("Alarm must be HH:mm");
            break;
            case "Open link":if(!Arrays.asList("http","https","geo","tel","mailto","sms","spotify","whatsapp").contains(java.net.URI.create(v).getScheme()))throw new Exception("Use a supported URI scheme");
            break;
            case "HTTPS webhook":if(!"https".equals(java.net.URI.create(v).getScheme())||java.net.URI.create(v).getHost()==null)throw new Exception("Use an HTTPS URL");
            break;
            case "Wi-Fi controls":case "Bluetooth controls":break;
            case "Open app":case "Show message":if(v.isEmpty())throw new Exception("Missing value for "+t);
            break;
            default:throw new Exception("Unknown action");
        }
    }
}
