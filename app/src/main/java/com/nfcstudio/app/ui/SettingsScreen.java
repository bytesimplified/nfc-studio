package com.nfcstudio.app.ui;
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
import static com.nfcstudio.app.nfc.RecordCodec.*;
import static com.nfcstudio.app.automation.ActionValidator.*;
final class SettingsScreen {
    private final StudioUi ui;
    private final Activity activity;
    private final ProfileStore store;
    private final AutomationEngine engine;
    SettingsScreen(StudioUi ui) {
        this.ui=ui;
        this.activity=ui.activity;
        this.store=ui.store;
        this.engine=ui.engine;
    }
    void dashboard(){
        ui.page("Settings","Make NFC Studio yours.");
        ui.section("Appearance");String pref=store.preferences().getString("appearance","amoled");
        ui.item("moon","AMOLED black","True black · OLED friendly"+("amoled".equals(pref)?" · Selected":""),ui.palette.lilac,()->appearance("amoled"));
        ui.item("moon","Midnight blue","Soft dark surfaces"+("dark".equals(pref)?" · Selected":""),ui.palette.primary,()->appearance("dark"));
        ui.item("sun","Light","Bright and clean"+("light".equals(pref)?" · Selected":""),ui.palette.amber,()->appearance("light"));
        ui.item("settings","Follow system","Use Android appearance"+("system".equals(pref)?" · Selected":""),ui.palette.success,()->appearance("system"));
        ui.section("Permissions");
        ui.item("settings","Permission center","Review device access",ui.palette.primary,this::permissions);
        ui.section("Data management");
        ui.item("history","Scan / execution history","Local activity log",ui.palette.primary,this::showHistory);
        ui.item("copy","Export configuration","Save a local JSON backup",ui.palette.lilac,()->ui.launch(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"nfc-studio-backup.json"),101));
        ui.item("copy","Import configuration","Restore profiles and bindings",ui.palette.success,()->ui.confirm("Replace local configuration?","Import will replace local profiles and tag bindings. Only import a file you trust.",()->ui.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE),102)));
        ui.section("About");ui.label("NFC Studio 0.3  ·  Native Android  ·  Local-first");
    }
    private void appearance(String value){
        store.preferences().edit().putString("appearance",value).putBoolean("return_to_settings",true).apply();
        activity.recreate();
    }
    void showHistory() {
        ui.page("History","Most recent 150 events. Stored on this device.");
        if(store.history().length()==0)ui.label("No activity yet.");
        for(int i=0;i<store.history().length();i++){LinearLayout card=ui.card();ui.add(card,ui.text(store.history().optString(i),14,ui.palette.text),0);ui.cardToRoot(card);}
        ui.button("Clear history",()->ui.confirm("Clear history?","This does not affect profiles.",()-> {
            store.clearHistory();
            showHistory();
        }));
        ui.button("Back to settings",this::dashboard);
    }
    void permissions() {
        ui.page("Permissions","Android requires explicit access for some actions.");
        ui.item("moon","Do Not Disturb access",((NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE)).isNotificationPolicyAccessGranted()?"Granted":"Not granted",ui.palette.lilac,()->ui.launch(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)));
        ui.item("sun","Modify system settings",Settings.System.canWrite(activity)?"Granted":"Required for brightness / rotation",ui.palette.amber,()->ui.launch(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+activity.getPackageName()))));
        ui.item("sun","Camera / flashlight permission",activity.checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED?"Granted":"Not granted",ui.palette.primary,()->activity.requestPermissions(new String[] {
            Manifest.permission.CAMERA
        },50));
        ui.item("scan","NFC settings",ui.nfc!=null&&ui.nfc.isEnabled()?"Enabled":"Disabled or unavailable",ui.palette.success,()->ui.launch(new Intent(Settings.ACTION_NFC_SETTINGS)));
        ui.button("Home",ui.home::home);
    }
    void onActivityResult(int req,int code,Intent data) {
        if(code!=Activity.RESULT_OK||data==null||data.getData()==null)return;
        try {
            if(req==101) {
                JSONObject b=BackupCodec.encode(store.profiles(),store.bindings());
                try(OutputStream out=activity.getContentResolver().openOutputStream(data.getData())) {
                    if(out==null)throw new IOException("Cannot open destination");
                    out.write(b.toString(2).getBytes(StandardCharsets.UTF_8));
                }
                ui.toast("Backup exported");
            }
            else if(req==102) {
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                try(InputStream in=activity.getContentResolver().openInputStream(data.getData())) {
                    if(in==null)throw new IOException("Cannot open backup");
                    byte[] buf=new byte[4096];
                    int n;
                    while((n=in.read(buf))!=-1) {
                        if(out.size()+n>1000000)throw new IOException("Backup too large");
                        out.write(buf,0,n);
                    }
                }
                JSONObject b=BackupCodec.decode(new String(out.toByteArray(),StandardCharsets.UTF_8));
                store.replace(b.getJSONObject("profiles"),b.getJSONObject("bindings"));
                ui.home.home();
                ui.toast("Backup imported");
            }
        } catch(Exception e) {
            ui.toast("Backup error: "+e.getMessage());
        }
    }
}
