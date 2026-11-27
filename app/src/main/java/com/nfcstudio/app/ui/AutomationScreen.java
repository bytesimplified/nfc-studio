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
final class AutomationScreen {
    private final StudioUi ui;
    private final Activity activity;
    private final ProfileStore store;
    private final AutomationEngine engine;
    AutomationScreen(StudioUi ui) {
        this.ui=ui;
        this.activity=ui.activity;
        this.store=ui.store;
        this.engine=ui.engine;
    }
    void listProfiles() {
        ui.page("Automations","Select a saved profile to edit, run or bind to a tag.");
        Iterator<String> keys=store.profiles().keys();
        while(keys.hasNext()) {
            String id=keys.next();
            JSONObject p=store.profiles().optJSONObject(id);
            if(p!=null)ui.button(p.optString("name")+" • "+p.optJSONArray("actions").length()+" actions",()->profileMenu(id));
        }
        ui.button("New automation",()->editor(null));
        ui.button("Home",ui.home::home);
    }
    void profileMenu(String id) {
        JSONObject p=store.profiles().optJSONObject(id);
        if(p==null) {
            ui.home.home();
            return;
        }
        ui.page(p.optString("name"),"Profile actions");
        JSONArray a=p.optJSONArray("actions");
        for(int i=0; i<a.length(); i++) {
            JSONObject x=a.optJSONObject(i);
            ui.label((i+1)+". "+x.optString("type")+"  "+x.optString("value"));
        }
        ui.button("Run now",()->engine.runProfile(id));
        ui.button("Write to NFC tag",()-> {
            NdefMessage msg=new NdefMessage(NdefRecord.createMime(MIME,id.getBytes(StandardCharsets.UTF_8)));
            ui.confirm("Program NFC tag?","Writes a local automation ID ("+msg.toByteArray().length+" bytes). The actions remain on this phone.",()->ui.tags.waitFor("write",msg,id,"Tap your new or reusable tag."));
        });
        ui.button("Edit / reassign actions",()->editor(id));
        ui.button("Delete profile",()->ui.confirm("Delete profile?","Tags referring to it will no longer run.",()-> {
            store.profiles().remove(id);
            ArrayList<String> stale=new ArrayList<>();
            Iterator<String> keys=store.bindings().keys();
            while(keys.hasNext()) {
                String uid=keys.next();
                if(id.equals(store.bindings().optString(uid)))stale.add(uid);
            }
            for(String uid:stale)store.bindings().remove(uid);
            store.preferences().edit().remove("snapshot_"+id).apply();
            store.save();
            listProfiles();
        }));
        ui.button("Back",this::listProfiles);
    }
    static class Row {
        Spinner type;
        EditText value;
        LinearLayout box;
    }
    void editor(String id) {
        JSONObject old=id==null?null:store.profiles().optJSONObject(id);
        ui.page("Create automation","Actions run in order. Put app launches and settings panels last.");
        EditText name=ui.edit("Profile name",old==null?"":old.optString("name"));
        CheckBox ask=new CheckBox(activity);
        ask.setText("Confirm before running from NFC");
        ask.setChecked(old!=null&&old.optBoolean("confirm"));
        ui.root.addView(ask);
        CheckBox toggle=new CheckBox(activity);
        toggle.setText("Second tap restores supported settings");
        toggle.setChecked(old!=null&&old.optBoolean("toggle"));
        ui.root.addView(toggle);
        LinearLayout area=new LinearLayout(activity);
        area.setOrientation(1);
        ui.root.addView(area);
        ArrayList<Row> rows=new ArrayList<>();
        JSONArray a=old==null?new JSONArray():old.optJSONArray("actions");
        for(int i=0; i<a.length(); i++) {
            JSONObject x=a.optJSONObject(i);
            addRow(area,rows,x.optString("type"),x.optString("value"));
        }
        if(rows.isEmpty())addRow(area,rows,"Open app","");
        ui.button("+ Add action",()->addRow(area,rows,"Open app",""));
        ui.button("Save automation",()-> {
            try {
                String n=name.getText().toString().trim();
                if(n.isEmpty()||rows.isEmpty())throw new Exception("Name and at least one action required");
                JSONArray actions=new JSONArray();
                for(Row r:rows) {
                    String t=(String)r.type.getSelectedItem(),v=r.value.getText().toString().trim();
                    validate(t,v);
                    JSONObject x=new JSONObject();
                    put(x,"type",t);
                    put(x,"value",v);
                    actions.put(x);
                }
                JSONObject p=new JSONObject();
                put(p,"name",n);
                put(p,"actions",actions);
                put(p,"confirm",ask.isChecked());
                put(p,"toggle",toggle.isChecked());
                String key=id==null?UUID.randomUUID().toString().replace("-","").substring(0,16):id;
                put(store.profiles(),key,p);
                store.save();
                profileMenu(key);
            } catch(Exception ex) {
                ui.toast(ex.getMessage());
            }
        });
        ui.button("Cancel",ui.home::home);
    }
    void addRow(LinearLayout area,ArrayList<Row> rows,String t,String v) {
        Row r=new Row();
        r.box=new LinearLayout(activity);
        r.box.setOrientation(1);
        r.box.setPadding(ui.dp(8),ui.dp(10),ui.dp(8),ui.dp(10));
        r.box.setBackgroundColor(PANEL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,ui.dp(5),0,ui.dp(5));
        area.addView(r.box,lp);
        r.type=new Spinner(activity);
        r.type.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,TYPES));
        int ix=Math.max(0,Arrays.asList(TYPES).indexOf(t));
        r.type.setSelection(ix);
        r.box.addView(r.type);
        r.value=new EditText(activity);
        r.value.setSingleLine(true);
        r.value.setText(v);
        r.value.setHint(HINTS[ix]);
        r.box.addView(r.value);
        Button pick=new Button(activity);
        pick.setText("Choose installed app");
        r.box.addView(pick);
        pick.setOnClickListener(w->ui.pickApp(r.value));
        r.type.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p,View view,int i,long l) {
                r.value.setHint(HINTS[i]);
                pick.setVisibility(i==0?View.VISIBLE:View.GONE);
            }
            public void onNothingSelected(AdapterView<?> p) {
            }
        });
        Button remove=new Button(activity);
        remove.setText("Remove action");
        r.box.addView(remove);
        remove.setOnClickListener(w-> {
            rows.remove(r);
            area.removeView(r.box);
        });
        rows.add(r);
    }
}
