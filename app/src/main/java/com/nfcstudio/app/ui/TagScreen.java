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
    boolean isBusy(){return writing;}
    void waitFor(String m,NdefMessage data,String id,String prompt) {
        if(writing){ui.toast("NFC operation in progress");return;}
        mode=m;
        staged=data;
        selectedId=id;
        ui.page("Ready to scan",prompt);
        ui.pulse();
        ui.status=ui.bold("Waiting for NFC tag…",23,ui.palette.primary);
        ui.status.setGravity(Gravity.CENTER);ui.add(ui.root,ui.status,8);
        LinearLayout note=ui.card();ui.add(note,ui.bold("Hold steady",16,ui.palette.text),0);ui.add(note,ui.text("Keep the tag near the NFC antenna on the back of the phone until the operation completes.",14,ui.palette.muted),8);if(data!=null)ui.add(note,ui.text("Payload: "+data.toByteArray().length+" bytes",13,ui.palette.primary),8);ui.cardToRoot(note);
        ui.button("Cancel / Home",ui.home::home);
    }
    private static final String[] RECORDS={"Text","Website","Phone","Email","SMS","Location","Contact","Wi-Fi","Application"};
    private static final String[] HINTS={"Enter your text","https://example.com","+91…","name@example.com","Phone number","latitude,longitude","BEGIN:VCARD…","Network name (SSID)","Package name"};
    void writeMenu(){
        ui.page("Write NFC data","Choose what to store on your tag.");
        String[] icons={"text","globe","app","text","text","globe","tags","wifi","app","bolt"};
        String[] names={"Text","Website","Phone","Email","SMS","Location","Contact","Wi-Fi","Application","Automation"};
        String[] desc={"Plain text","Open a URL","Dial number","Compose mail","Compose SMS","Map coordinates","vCard","Network details","Launch an app","Tap to run"};
        int[] colors={ui.palette.primary,ui.palette.lilac,ui.palette.success,ui.palette.primary,ui.palette.amber,ui.palette.lilac,ui.palette.success,ui.palette.primary,ui.palette.lilac,ui.palette.amber};
        Runnable[] actions=new Runnable[names.length];for(int i=0;i<names.length;i++){final int index=i;actions[i]=()->{if(index==9)ui.automations.listProfiles();else recordForm(index);};}
        ui.tileGrid(icons,names,desc,colors,actions);
        ui.label("All records use NDEF. Existing data will be replaced only after confirmation.");
    }
    private NdefMessage message(int index,String v,String password)throws Exception{
        if(v.trim().isEmpty())throw new Exception("Enter a value");
        NdefRecord rec;
        switch(index){
            case 0:rec=NdefRecord.createTextRecord("en",v);break;
            case 1:rec=NdefRecord.createUri(v);break;
            case 2:rec=NdefRecord.createUri("tel:"+v);break;
            case 3:rec=NdefRecord.createUri("mailto:"+v);break;
            case 4:rec=NdefRecord.createUri("sms:"+v);break;
            case 5:rec=NdefRecord.createUri("geo:"+v);break;
            case 6:rec=NdefRecord.createMime("text/vcard",v.getBytes(StandardCharsets.UTF_8));break;
            case 7:rec=wifi(v,password);break;
            default:rec=NdefRecord.createApplicationRecord(v);
        }return new NdefMessage(rec);
    }
    private void recordForm(int index){
        ui.page("New "+RECORDS[index]+" record","Prepare the NDEF payload before scanning.");
        EditText e=ui.edit(HINTS[index],"");
        if(index==0||index==6){e.setMinLines(3);e.setGravity(Gravity.TOP);}
        EditText password=index==7?ui.edit("Wi-Fi password",""):null;
        if(password!=null)password.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        if(index==8)ui.button("Choose app package",()->ui.pickApp(e));
        LinearLayout estimate=ui.card();ui.add(estimate,ui.bold("Payload preview",15,ui.palette.text),0);
        TextView bytes=ui.text("0 bytes · NDEF",22,ui.palette.primary);ui.add(estimate,bytes,8);ui.add(estimate,ui.text("Actual tag capacity is checked before writing.",13,ui.palette.muted),7);ui.cardToRoot(estimate);
        android.text.TextWatcher watcher=new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){try{bytes.setText(message(index,e.getText().toString(),password==null?"":password.getText().toString()).toByteArray().length+" bytes · NDEF");}catch(Exception ex){bytes.setText("Enter valid content");}}
            public void afterTextChanged(android.text.Editable s){}
        };e.addTextChangedListener(watcher);if(password!=null)password.addTextChangedListener(watcher);
        ui.primary("Prepare write",()->{try{
            NdefMessage msg=message(index,e.getText().toString(),password==null?"":password.getText().toString());
            ui.confirm("Overwrite tag?","Write "+msg.toByteArray().length+" bytes. Anyone can read unprotected tag data.",()->waitFor("write",msg,null,"Tap the writable tag."));
        }catch(Exception ex){ui.toast(ex.getMessage());}});
        ui.button("Choose another record",this::writeMenu);
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
        ui.page("Tag details","Inspect the detected NFC tag.");
        LinearLayout info=ui.card();ui.add(info,ui.bold("TAG INFORMATION",13,ui.palette.primary),0);
        for(String line:details.split("\\n"))ui.add(info,ui.text(line,14,ui.palette.text),9);ui.cardToRoot(info);
        mode="read";
        ui.status=ui.bold("Scan complete",15,ui.palette.success);ui.add(ui.root,ui.status,10);
        if(msg!=null) {
            ui.section("NDEF records");ui.label("Payload size: "+msg.toByteArray().length+" bytes");
            int i=0;
            for(NdefRecord r:msg.getRecords()) {
                LinearLayout record=ui.card();ui.add(record,ui.bold("Record "+(++i),16,ui.palette.primary),0);
                Uri uri=r.toUri();
                ui.add(record,ui.text("TNF: "+r.getTnf()+" • Type: "+new String(r.getType(),StandardCharsets.US_ASCII),12,ui.palette.muted),7);
                byte[] data=r.getPayload();
                String content;
                if(uri!=null)content=uri.toString();
                else if(r.getTnf()==NdefRecord.TNF_WELL_KNOWN&&Arrays.equals(r.getType(),NdefRecord.RTD_TEXT)&&data.length>0) {
                    int offset=1+(data[0]&63);
                    content=offset<=data.length?new String(data,offset,data.length-offset,(data[0]&128)==0?StandardCharsets.UTF_8:StandardCharsets.UTF_16):TagPolicy.hex(data);
                } else if(MIME.equals(new String(r.getType(),StandardCharsets.US_ASCII))||new String(r.getType(),StandardCharsets.US_ASCII).startsWith("text/"))content=new String(data,StandardCharsets.UTF_8);
                else content=TagPolicy.hex(data);
                TextView val=ui.text(content,14,ui.palette.text);
                val.setTextIsSelectable(true);
                ui.add(record,val,8);ui.cardToRoot(record);
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
        ui.button("Back to home",ui.home::home);
    }
    void listTags() {
        ui.page("My tags","Your physical shortcuts, organized.");
        ui.metricPair("Registered tags",String.valueOf(store.bindings().length()),"Saved routines",String.valueOf(store.profiles().length()));
        ui.primary("Register / inspect a tag",()->waitFor("read",null,null,"Scan a tag containing a local automation profile."));
        ui.section("Tag library");if(store.bindings().length()==0)ui.label("No registered tags yet. Write an automation to a tag to add it here.");
        Iterator<String> keys=store.bindings().keys();
        while(keys.hasNext()) {
            String uid=keys.next(),id=store.bindings().optString(uid);
            JSONObject p=store.profiles().optJSONObject(id);
            ui.item("tags",p==null?"Missing profile":p.optString("name"),"UID "+uid,ui.palette.primary,()->new AlertDialog.Builder(activity).setTitle(uid).setItems(new String[] {
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
