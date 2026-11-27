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
        ui.page("Automations","Make everyday tasks one tap away.");
        ui.primary("Create automation",()->editor(null));
        int active=0;
        Iterator<String> keys=store.profiles().keys();
        while(keys.hasNext()){JSONObject p=store.profiles().optJSONObject(keys.next());if(p!=null&&p.optBoolean("enabled",true))active++;}
        ui.metricPair("Active routines",String.valueOf(active),"Total routines",String.valueOf(store.profiles().length()));
        ui.section("Your routines");
        keys=store.profiles().keys();
        while(keys.hasNext()){String id=keys.next();JSONObject p=store.profiles().optJSONObject(id);if(p!=null)profileCard(id,p,true);}
        if(store.profiles().length()==0)ui.label("No routines yet. Create one or start from a template.");
        ui.section("Discover templates");
        ui.item("work","Work mode","Focus • lower media volume",ui.palette.primary,()->template("Work mode",new String[][]{{"Do Not Disturb","on"},{"Media volume","20"}}));
        ui.item("moon","Sleep mode","Quiet • dim display",ui.palette.lilac,()->template("Sleep mode",new String[][]{{"Do Not Disturb","on"},{"Brightness","10"},{"Media volume","0"}}));
        ui.item("car","Driving mode","Sound • Bluetooth settings",ui.palette.amber,()->template("Driving mode",new String[][]{{"Media volume","80"},{"Bluetooth controls",""}}));
        ui.item("home","Home mode","Normal ringer • brightness",ui.palette.success,()->template("Home mode",new String[][]{{"Ring mode","normal"},{"Brightness","60"}}));
        ui.item("bolt","Gym mode","Media • workout timer",ui.palette.primary,()->template("Gym mode",new String[][]{{"Media volume","65"},{"Set timer","1800"}}));
    }
    void profileCard(String id,JSONObject p,boolean toggle){
        String name=p.optString("name");String low=name.toLowerCase(Locale.ROOT);
        String icon=low.contains("work")?"work":low.contains("sleep")?"moon":low.contains("driv")?"car":low.contains("home")?"home":"bolt";
        int color=icon.equals("moon")?ui.palette.lilac:icon.equals("car")?ui.palette.amber:icon.equals("home")?ui.palette.success:ui.palette.primary;
        LinearLayout r=ui.row();ui.pad(r,14);
        r.addView(ui.iconBadge(icon,color,24),new LinearLayout.LayoutParams(ui.dp(46),ui.dp(46)));
        LinearLayout words=ui.column();LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=ui.dp(12);r.addView(words,lp);
        ui.add(words,ui.bold(name,16,ui.palette.text),0);
        int bound=0;Iterator<String> ids=store.bindings().keys();while(ids.hasNext())if(id.equals(store.bindings().optString(ids.next())))bound++;
        ui.add(words,ui.text(p.optJSONArray("actions").length()+" actions · "+bound+" tags",13,ui.palette.muted),5);
        ui.clickable(r,ui.outlined(ui.palette.surface,20),()->profileMenu(id));
        if(toggle){Switch sw=new Switch(activity);sw.setContentDescription("Enable "+name);sw.setChecked(p.optBoolean("enabled",true));sw.setOnCheckedChangeListener((v,on)->{put(p,"enabled",on);store.save();});r.addView(sw);}
        else r.addView(new GlyphView(activity,"arrow",ui.palette.muted),new LinearLayout.LayoutParams(ui.dp(20),ui.dp(20)));
        ui.add(ui.root,r,10);
    }
    private JSONObject draft;
    private void template(String name,String[][] pairs){
        JSONObject p=new JSONObject();JSONArray actions=new JSONArray();
        for(String[] pair:pairs){JSONObject x=new JSONObject();put(x,"type",pair[0]);put(x,"value",pair[1]);actions.put(x);}
        put(p,"name",name);put(p,"actions",actions);put(p,"toggle",true);put(p,"confirm",false);draft=p;editor(null);
    }
    void profileMenu(String id) {
        JSONObject p=store.profiles().optJSONObject(id);
        if(p==null) {
            ui.home.home();
            return;
        }
        ui.page(p.optString("name"),"Tap-triggered routine");
        ui.metricPair("Actions",String.valueOf(p.optJSONArray("actions").length()),"State",p.optBoolean("enabled",true)?"On":"Off");
        ui.section("Action sequence");
        JSONArray a=p.optJSONArray("actions");
        for(int i=0; i<a.length(); i++) {
            JSONObject x=a.optJSONObject(i);
            LinearLayout card=ui.card();ui.add(card,ui.bold(String.format(Locale.ROOT,"%02d  %s",i+1,x.optString("type")),16,ui.palette.text),0);ui.add(card,ui.text(x.optString("value"),14,ui.palette.muted),6);ui.cardToRoot(card);
        }
        ui.primary("Run now",()->engine.runProfile(id));
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
        JSONObject old=id==null?draft:store.profiles().optJSONObject(id);draft=null;
        ui.page("Create automation","Actions run in order. Put app launches and settings panels last.");
        EditText name=ui.edit("Profile name",old==null?"":old.optString("name"));
        LinearLayout trigger=ui.card();ui.add(trigger,ui.bold("TRIGGER  ·  NFC tag tapped",15,ui.palette.primary),0);ui.add(trigger,ui.text("Assign a physical tag after saving.",13,ui.palette.muted),7);ui.cardToRoot(trigger);
        ui.section("Routine settings");
        CheckBox ask=new CheckBox(activity);
        ask.setText("Confirm before running from NFC");
        ask.setChecked(old!=null&&old.optBoolean("confirm"));
        ui.styleCheck(ask);ui.add(ui.root,ask,8);
        CheckBox toggle=new CheckBox(activity);
        toggle.setText("Second tap restores supported settings");
        toggle.setChecked(old!=null&&old.optBoolean("toggle"));
        ui.styleCheck(toggle);ui.add(ui.root,toggle,6);
        ui.section("Action sequence");
        LinearLayout area=new LinearLayout(activity);
        area.setOrientation(1);
        ui.add(ui.root,area,8);
        ArrayList<Row> rows=new ArrayList<>();
        JSONArray a=old==null?new JSONArray():old.optJSONArray("actions");
        for(int i=0; i<a.length(); i++) {
            JSONObject x=a.optJSONObject(i);
            addRow(area,rows,x.optString("type"),x.optString("value"));
        }
        if(rows.isEmpty())addRow(area,rows,"Open app","");
        ui.button("+ Add action",()->addRow(area,rows,"Open app",""));
        ui.primary("Save automation",()-> {
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
                put(p,"enabled",old==null||old.optBoolean("enabled",true));
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
        r.box.setBackground(ui.outlined(ui.palette.surface,22));
        ui.pad(r.box,14);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,ui.dp(5),0,ui.dp(5));
        area.addView(r.box,lp);
        r.type=ui.spinner(TYPES);
        int ix=Math.max(0,Arrays.asList(TYPES).indexOf(t));
        r.type.setSelection(ix);
        r.box.addView(r.type);
        r.value=new EditText(activity);
        r.value.setSingleLine(true);ui.styleEdit(r.value);
        r.value.setText(v);
        r.value.setHint(HINTS[ix]);
        ui.add(r.box,r.value,10);
        Button pick=ui.actionButton(r.box,"Choose installed app",()->ui.pickApp(r.value),false);
        r.type.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p,View view,int i,long l) {
                r.value.setHint(HINTS[i]);
                pick.setVisibility(i==0?View.VISIBLE:View.GONE);
            }
            public void onNothingSelected(AdapterView<?> p) {
            }
        });
        LinearLayout controls=ui.row();
        String[] labels={"Move up","Move down","Remove action"};
        for(int j=0;j<labels.length;j++){
            final int action=j;Button b=new Button(activity);b.setAllCaps(false);b.setText(labels[j]);b.setTextSize(12);b.setTextColor(j==2?ui.palette.danger:ui.palette.primary);b.setBackgroundTintList(null);
            ui.clickable(b,ui.shape(ui.palette.elevated,14),()->{
                int from=rows.indexOf(r);if(from<0)return;
                if(action==2){rows.remove(from);area.removeView(r.box);return;}
                int to=from+(action==0?-1:1);if(to<0||to>=rows.size())return;
                Collections.swap(rows,from,to);area.removeView(r.box);area.addView(r.box,to);
            });LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,ui.dp(50),1);if(j>0)bp.leftMargin=ui.dp(5);controls.addView(b,bp);
        }
        ui.add(r.box,controls,9);rows.add(r);
    }
}
