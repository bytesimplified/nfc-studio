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
import static com.nfcstudio.app.data.JsonValues.*;
final class ActionExecutor {
    private final Activity activity;
    ActionExecutor(Activity activity) {
        this.activity=activity;
    }
    AudioManager audio() {
        return (AudioManager)activity.getSystemService(Context.AUDIO_SERVICE);
    }
    NotificationManager notifications() {
        return (NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE);
    }
    void execute(String t,String v)throws Exception {
        switch(t) {
            case "Open app":Intent app=activity.getPackageManager().getLaunchIntentForPackage(v);
            if(app==null)throw new Exception("App not installed or launchable");
            activity.startActivity(app);
            break;
            case "Open link":activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(v)));
            break;
            case "Media volume":audio().setStreamVolume(AudioManager.STREAM_MUSIC,Math.round(audio().getStreamMaxVolume(AudioManager.STREAM_MUSIC)*Integer.parseInt(v)/100f),0);
            break;
            case "Ring mode":audio().setRingerMode(v.equals("normal")?AudioManager.RINGER_MODE_NORMAL:v.equals("silent")?AudioManager.RINGER_MODE_SILENT:AudioManager.RINGER_MODE_VIBRATE);
            break;
            case "Do Not Disturb":if(!notifications().isNotificationPolicyAccessGranted())throw new Exception("Grant DND access in settings");
            notifications().setInterruptionFilter(v.equals("on")?NotificationManager.INTERRUPTION_FILTER_PRIORITY:NotificationManager.INTERRUPTION_FILTER_ALL);
            break;
            case "Brightness":if(!Settings.System.canWrite(activity))throw new Exception("Grant Modify system settings");
            Settings.System.putInt(activity.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            if(!Settings.System.putInt(activity.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,Math.max(1,Math.round(Integer.parseInt(v)*255/100f))))throw new Exception("Brightness write rejected");
            break;
            case "Rotation":if(!Settings.System.canWrite(activity)||!Settings.System.putInt(activity.getContentResolver(),Settings.System.ACCELEROMETER_ROTATION,v.equals("auto")?1:0))throw new Exception("Grant Modify system settings");
            break;
            case "Flashlight":if(activity.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)throw new Exception("Grant camera permission in settings");
            CameraManager cm=(CameraManager)activity.getSystemService(Context.CAMERA_SERVICE);
            String flash=null;
            for(String cam:cm.getCameraIdList())if(Boolean.TRUE.equals(cm.getCameraCharacteristics(cam).get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) {
                flash=cam;
                break;
            }
            if(flash==null)throw new Exception("No flashlight");
            cm.setTorchMode(flash,v.equals("on"));
            break;
            case "Vibrate":((Vibrator)activity.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(Integer.parseInt(v),VibrationEffect.DEFAULT_AMPLITUDE));
            break;
            case "Show message":notifyUser(v);
            break;
            case "Set alarm":String[] hm=v.split(":");
            activity.startActivity(new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR,Integer.parseInt(hm[0])).putExtra(AlarmClock.EXTRA_MINUTES,Integer.parseInt(hm[1])).putExtra(AlarmClock.EXTRA_SKIP_UI,true));
            break;
            case "Set timer":activity.startActivity(new Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH,Integer.parseInt(v)).putExtra(AlarmClock.EXTRA_SKIP_UI,true));
            break;
            case "Wi-Fi controls":activity.startActivity(new Intent(Settings.Panel.ACTION_WIFI));
            break;
            case "Bluetooth controls":activity.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            break;
            default:throw new Exception("Action unavailable");
        }
    }
    private void notifyUser(String message) {
        Toast.makeText(activity,message,Toast.LENGTH_LONG).show();
    }
}
