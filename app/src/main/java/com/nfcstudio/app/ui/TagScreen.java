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
final class TagScreen {
    private final StudioUi ui;
    private final Activity activity;
    private final ProfileStore store;
    private final AutomationEngine engine;
    private final TagIo io=new TagIo();
    String mode="auto",selectedId;
    NdefMessage staged,copied;
    private boolean writing,closed;
    private long lastTap;
    private String lastUid="";
    TagScreen(StudioUi ui) {
        this.ui=ui;
        this.activity=ui.activity;
        this.store=ui.store;
        this.engine=ui.engine;
    }
    void waitFor(String m,NdefMessage data,String id,String prompt) {
        mode=m;
        staged=data;
        selectedId=id;
        ui.page("Ready to scan",prompt);
        ui.status=ui.text("Waiting for NFC tag…",20,ACCENT);
        ui.root.addView(ui.status);
        ui.label("Keep the tag still until the operation completes. Your phone must have NFC enabled.");
        ui.button("Cancel / Home",ui.home::home);
    }
    void writeMenu() {
        ui.page("Write NFC data","Standard NDEF records. Existing content will be replaced.");
        String[] names= {
            "Text","Website / URI","Phone number","Email","SMS","GPS coordinates","Contact (vCard text)","Wi-Fi (WSC, WPA/WPA2)","App shortcut (Android)"
        };
        Spinner sp=new Spinner(activity);
        sp.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,names));
        ui.root.addView(sp);
        EditText e=ui.edit("Value",""), extra=ui.edit("Wi-Fi password only","" );
        extra.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        ui.button("Choose app package",()->ui.pickApp(e));
        ui.button("Prepare write",()-> {
            try {
                String v=e.getText().toString().trim();
                if(v.isEmpty())throw new Exception("Enter a value");
                NdefRecord rec;
                switch(sp.getSelectedItemPosition()) {
                    case 0:rec=NdefRecord.createTextRecord("en",v);
                    break;
                    case 1:rec=NdefRecord.createUri(v);
                    break;
                    case 2:rec=NdefRecord.createUri("tel:"+v);
                    break;
                    case 3:rec=NdefRecord.createUri("mailto:"+v);
                    break;
                    case 4:rec=NdefRecord.createUri("sms:"+v);
                    break;
                    case 5:rec=NdefRecord.createUri("geo:"+v);
                    break;
                    case 6:rec=NdefRecord.createMime("text/vcard",v.getBytes(StandardCharsets.UTF_8));
                    break;
                    case 7:rec=wifi(v,extra.getText().toString());
                    break;
                    default:rec=NdefRecord.createApplicationRecord(v);
                }
                NdefMessage msg=new NdefMessage(rec);
                ui.confirm("Overwrite tag?","Write "+msg.toByteArray().length+" bytes. Anyone can read unprotected tag data.",()->waitFor("write",msg,null,"Tap the writable tag."));
            } catch(Exception x) {
                ui.toast(x.getMessage());
            }
        });
        ui.button("Home",ui.home::home);
    }
    void finishScan(String op,String uid,String id,NdefMessage msg,String details,boolean verified) {
        if(op.equals("write")) {
            if(id!=null) {
                put(store.bindings(),uid,id);
                store.save();
            } else {
                store.bindings().remove(uid);
                store.save();
            }
            ui.home.home();
            store.log("Write "+uid+(verified?" verified":" formatted; rescan to verify"));
            ui.report(verified?"Tag written and verified":"Tag formatted and written; tap Read to verify");
            return;
        }
        if(op.equals("lock")) {
            ui.home.home();
            store.log("Locked "+uid);
            ui.report("Tag is now read-only");
            return;
        }
        if(op.equals("copy")) {
            if(msg==null) {
                ui.report("No NDEF records to copy");
                return;
            }
            copied=msg;
            waitFor("write",copied,null,"Tap the destination tag. Existing content will be overwritten.");
            return;
        }
        String found=null;
        if(msg!=null)for(NdefRecord r:msg.getRecords()) {
            if(r.getTnf()==NdefRecord.TNF_MIME_MEDIA&&MIME.equals(new String(r.getType(),StandardCharsets.US_ASCII))) {
                found=new String(r.getPayload(),StandardCharsets.UTF_8);
                break;
            }
        }
        if(op.equals("auto")&&TagPolicy.canTrigger(found,store.bindings().optString(uid),found!=null&&store.profiles().has(found))) {
            String key=found;
            JSONObject p=store.profiles().optJSONObject(key);
            store.log("Tapped "+uid+" • "+p.optString("name"));
            if(p.optBoolean("confirm"))ui.confirm("Run "+p.optString("name")+"?","Run the saved local automation.",()->engine.runProfile(key));
            else engine.runProfile(key);
            return;
        }
        store.log("Read "+uid);
        ui.page("Tag details",details);
        mode="read";
        ui.status=ui.text("Scan complete",15,ACCENT);
        ui.root.addView(ui.status);
        if(msg!=null) {
            ui.label("NDEF size: "+msg.toByteArray().length+" bytes");
            int i=0;
            for(NdefRecord r:msg.getRecords()) {
                ui.section("Record "+(++i));
                Uri uri=r.toUri();
                ui.label("TNF: "+r.getTnf()+" • Type: "+new String(r.getType(),StandardCharsets.US_ASCII));
                byte[] data=r.getPayload();
                String content;
                if(uri!=null)content=uri.toString();
                else if(r.getTnf()==NdefRecord.TNF_WELL_KNOWN&&Arrays.equals(r.getType(),NdefRecord.RTD_TEXT)&&data.length>0) {
                    int offset=1+(data[0]&63);
                    content=offset<=data.length?new String(data,offset,data.length-offset,(data[0]&128)==0?StandardCharsets.UTF_8:StandardCharsets.UTF_16):TagPolicy.hex(data);
                } else if(MIME.equals(new String(r.getType(),StandardCharsets.US_ASCII))||new String(r.getType(),StandardCharsets.US_ASCII).startsWith("text/"))content=new String(data,StandardCharsets.UTF_8);
                else content=TagPolicy.hex(data);
                TextView val=ui.text(content,14,INK);
                val.setTextIsSelectable(true);
                ui.root.addView(val);
            }
            ui.button("Copy these records to another tag",()->ui.confirm("Overwrite destination?","The next tag will receive a copy of these NDEF records.",()->waitFor("write",msg,null,"Tap the destination tag.")));
        }
        if(found!=null&&store.profiles().has(found)) {
            String key=found;
            ui.button("Register this tag to local profile",()-> {
                put(store.bindings(),uid,key);
                store.save();
                ui.toast("Tag registered");
            });
        }
        ui.button("Home",ui.home::home);
    }
    void listTags() {
        ui.page("My tags","Registered automation tags. Profiles are stored locally.");
        Iterator<String> keys=store.bindings().keys();
        while(keys.hasNext()) {
            String uid=keys.next(),id=store.bindings().optString(uid);
            JSONObject p=store.profiles().optJSONObject(id);
            ui.button((p==null?"Missing profile":p.optString("name"))+"\n"+uid,()->new AlertDialog.Builder(activity).setTitle(uid).setItems(new String[] {
                "Open profile","Unregister"
            },(d,i)-> {
                if(i==0)ui.automations.profileMenu(id);
                else {
                    store.bindings().remove(uid);
                    store.save();
                    listTags();
                }
            }).show());
        }
        ui.button("Home",ui.home::home);
    }
    void close() {
        closed=true;
    }
    void handle(Intent intent) {
        if(closed||intent==null||!Arrays.asList(NfcAdapter.ACTION_NDEF_DISCOVERED,NfcAdapter.ACTION_TECH_DISCOVERED,NfcAdapter.ACTION_TAG_DISCOVERED).contains(intent.getAction()))return;
        Tag tag=intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if(tag==null||writing)return;
        String uid=TagPolicy.hex(tag.getId());
        long now=SystemClock.elapsedRealtime();
        if(TagPolicy.isDuplicate(uid,lastUid,now-lastTap))return;
        lastUid=uid;
        lastTap=now;
        String op=mode;
        String id=selectedId;
        NdefMessage msg=staged;
        writing=true;
        new Thread(()-> {
            try {
                TagIo.Result r=io.scan(tag,uid,op,msg);
                activity.runOnUiThread(()-> {
                    writing=false;
                    if(!closed)finishScan(op,uid,id,r.message,r.details,r.verified);
                });
            } catch(Exception e) {
                activity.runOnUiThread(()-> {
                    writing=false;
                    if(!closed) {
                        store.log("Tag "+uid+": "+e.getMessage());
                        ui.report("NFC error: "+e.getMessage());
                    }
                });
            }
        }).start();
    }
}
