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
import static com.nfcstudio.app.ui.StudioUi.*;
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
    void showHistory() {
        ui.page("History","Most recent 150 events. Stored on this device.");
        for(int i=0; i<store.history().length(); i++)ui.label(store.history().optString(i));
        ui.button("Clear history",()->ui.confirm("Clear history?","This does not affect profiles.",()-> {
            store.clearHistory();
            showHistory();
        }));
        ui.button("Home",ui.home::home);
    }
    void permissions() {
        ui.page("Permissions","Android requires explicit access for some actions.");
        ui.button("Do Not Disturb access",()->ui.launch(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)));
        ui.button("Modify system settings (brightness / rotation)",()->ui.launch(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+activity.getPackageName()))));
        ui.button("Camera / flashlight permission",()->activity.requestPermissions(new String[] {
            Manifest.permission.CAMERA
        },50));
        ui.button("NFC settings",()->ui.launch(new Intent(Settings.ACTION_NFC_SETTINGS)));
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
