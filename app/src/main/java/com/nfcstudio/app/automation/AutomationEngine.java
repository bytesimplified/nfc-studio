package com.nfcstudio.app.automation;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Typeface;
import android.hardware.camera2.*;
import android.media.AudioManager;
import android.net.Uri;
import android.nfc.*;
import android.nfc.tech.*;
import android.os.*;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.net.*;
import com.nfcstudio.app.data.*;
import com.nfcstudio.app.nfc.*;
import com.nfcstudio.app.automation.*;
import static com.nfcstudio.app.data.JsonValues.*;
public final class AutomationEngine {
    private final Activity activity;
    private final ProfileStore store;
    private final ActionExecutor executor;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean running,closed;
    public AutomationEngine(Activity activity,ProfileStore store) {
        this.activity=activity;
        this.store=store;
        this.executor=new ActionExecutor(activity);
    }
    public void close() {
        closed=true;
        running=false;
        handler.removeCallbacksAndMessages(null);
    }
    private void notifyUser(String message) {
        if(!closed)Toast.makeText(activity,message,Toast.LENGTH_LONG).show();
    }
    public void runProfile(String id) {
        if(closed)return;
        if(running) {
            notifyUser("An automation is already running");
            return;
        }
        JSONObject p=store.profiles().optJSONObject(id);
        if(p==null)return;
        if(!p.optBoolean("enabled",true)){notifyUser("This routine is disabled");return;}
        String key="snapshot_"+id;
        String prior=store.preferences().getString(key,null);
        if(p.optBoolean("toggle")&&prior!=null) {
            restore(object(prior));
            store.preferences().edit().remove(key).apply();
            store.log("Restored "+p.optString("name"));
            notifyUser("Restore attempted — see history");
            return;
        }
        if(p.optBoolean("toggle")) {
            JSONObject snapshot=new JSONObject();
            JSONArray a=p.optJSONArray("actions");
            for(int i=0; i<a.length(); i++) {
                String type=a.optJSONObject(i).optString("type");
                try {
                    switch(type) {
                        case "Media volume":put(snapshot,"volume",executor.audio().getStreamVolume(AudioManager.STREAM_MUSIC));
                        break;
                        case "Ring mode":put(snapshot,"ring",executor.audio().getRingerMode());
                        break;
                        case "Do Not Disturb":if(executor.notifications().isNotificationPolicyAccessGranted())put(snapshot,"dnd",executor.notifications().getCurrentInterruptionFilter());
                        break;
                        case "Brightness":if(Settings.System.canWrite(activity)) {
                            put(snapshot,"brightness",Settings.System.getInt(activity.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,128));
                            put(snapshot,"brightnessMode",Settings.System.getInt(activity.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,0));
                        }
                        break;
                        case "Rotation":if(Settings.System.canWrite(activity))put(snapshot,"rotation",Settings.System.getInt(activity.getContentResolver(),Settings.System.ACCELEROMETER_ROTATION,1));
                        break;
                    }
                } catch(Exception ignored) {
                }
            }
            store.preferences().edit().putString(key,snapshot.toString()).apply();
        }
        running=true;
        store.log("Started "+p.optString("name"));
        step(p.optJSONArray("actions"),0,p.optString("name"),0);
    }
    void restore(JSONObject s) {
        Iterator<String> keys=s.keys();
        while(keys.hasNext()) {
            String k=keys.next();
            try {
                int n=s.getInt(k);
                switch(k) {
                    case "volume":executor.audio().setStreamVolume(AudioManager.STREAM_MUSIC,n,0);
                    break;
                    case "ring":executor.audio().setRingerMode(n);
                    break;
                    case "dnd":executor.notifications().setInterruptionFilter(n);
                    break;
                    case "brightness":Settings.System.putInt(activity.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,n);
                    break;
                    case "brightnessMode":Settings.System.putInt(activity.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,n);
                    break;
                    case "rotation":Settings.System.putInt(activity.getContentResolver(),Settings.System.ACCELEROMETER_ROTATION,n);
                    break;
                }
            } catch(Exception ex) {
                store.log("Restore failed: "+k+" • "+ex.getMessage());
            }
        }
    }
    void step(JSONArray a,int i,String name,int failures) {
        if(closed)return;
        if(i>=a.length()) {
            running=false;
            store.log("Finished "+name+" • "+failures+" failed");
            notifyUser(name+(failures==0?" complete":" — "+failures+" action(s) failed"));
            return;
        }
        JSONObject x=a.optJSONObject(i);
        String t=x.optString("type"),v=x.optString("value");
        try {
            ActionValidator.validate(t,v);
            if(t.equals("Delay")) {
                handler.postDelayed(()->step(a,i+1,name,failures),Integer.parseInt(v)*1000L);
                return;
            }
            if(t.equals("HTTPS webhook")) {
                new Thread(()-> {
                    String err=null;
                    try {
                        WebhookClient.post(v);
                    } catch(Exception e) {
                        err=e.getMessage();
                    }
                    String error=err;
                    handler.post(()-> {
                        if(closed)return;
                        store.log(t+(error==null?" OK":" failed: "+error));
                        step(a,i+1,name,failures+(error==null?0:1));
                    });
                }).start();
                return;
            }
            executor.execute(t,v);
            store.log(t+" requested");
            handler.postDelayed(()->step(a,i+1,name,failures),100);
        } catch(Exception e) {
            store.log(t+" failed: "+e.getMessage());
            handler.post(()->step(a,i+1,name,failures+1));
        }
    }
}
