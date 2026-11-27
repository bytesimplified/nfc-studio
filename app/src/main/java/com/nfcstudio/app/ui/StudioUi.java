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
public final class StudioUi {
    static final int BG=0xff101b2d, PANEL=0xff1e3049, INK=0xffeaf2ff, MUTED=0xffa9bbd3, ACCENT=0xff8bc7ff;
    final Activity activity;
    final ProfileStore store;
    final AutomationEngine engine;
    final NfcAdapter nfc;
    final HomeScreen home;
    final TagScreen tags;
    final AutomationScreen automations;
    final SettingsScreen settings;
    LinearLayout root;
    TextView status;
    public StudioUi(Activity activity,ProfileStore store,NfcAdapter nfc) {
        this.activity=activity;
        this.store=store;
        this.nfc=nfc;
        this.engine=new AutomationEngine(activity,store);
        this.home=new HomeScreen(this);
        this.tags=new TagScreen(this);
        this.automations=new AutomationScreen(this);
        this.settings=new SettingsScreen(this);
        activity.getWindow().setStatusBarColor(BG);
        activity.getWindow().setNavigationBarColor(BG);
    }
    public void showHome() {
        home.home();
    }
    public void handle(Intent intent) {
        tags.handle(intent);
    }
    public void onActivityResult(int request,int result,Intent data) {
        settings.onActivityResult(request,result,data);
    }
    public void close() {
        tags.close();
        engine.close();
    }
    int dp(int n) {
        return (int)(activity.getResources().getDisplayMetrics().density*n+0.5f);
    }
    TextView text(String s,int size,int color) {
        TextView t=new TextView(activity);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(0,dp(5),0,dp(5));
        return t;
    }
    void page(String title,String sub) {
        ScrollView sc=new ScrollView(activity);
        sc.setFillViewport(true);
        root=new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(24),dp(20),dp(24));
        root.setBackgroundColor(BG);
        sc.addView(root);
        activity.setContentView(sc);
        TextView h=text(title,30,INK);
        h.setTypeface(null,Typeface.BOLD);
        root.addView(h);
        root.addView(text(sub,14,MUTED));
    }
    void label(String s) {
        root.addView(text(s,17,INK));
    }
    void section(String s) {
        TextView t=text(s,20,ACCENT);
        t.setTypeface(null,Typeface.BOLD);
        root.addView(t);
    }
    void button(String s,Runnable r) {
        Button b=new Button(activity);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(INK);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));
        b.setPadding(dp(12),dp(10),dp(12),dp(10));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,dp(4),0,dp(4));
        root.addView(b,lp);
        b.setOnClickListener(v->r.run());
    }
    EditText edit(String hint,String value) {
        EditText e=new EditText(activity);
        e.setTextColor(INK);
        e.setHintTextColor(MUTED);
        e.setHint(hint);
        e.setText(value);
        root.addView(e,new LinearLayout.LayoutParams(-1,-2));
        return e;
    }
    void toast(String s) {
        Toast.makeText(activity,s,Toast.LENGTH_LONG).show();
    }
    void report(String s) {
        if(status!=null)status.setText(s);
        toast(s);
    }
    void confirm(String t,String s,Runnable yes) {
        new AlertDialog.Builder(activity).setTitle(t).setMessage(s).setNegativeButton("Cancel",null).setPositiveButton("Continue",(d,w)->yes.run()).show();
    }
    void pickApp(EditText e) {
        Intent q=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps=activity.getPackageManager().queryIntentActivities(q,0);
        apps.sort(Comparator.comparing(a->a.loadLabel(activity.getPackageManager()).toString().toLowerCase(Locale.ROOT)));
        String[] labels=new String[apps.size()];
        for(int i=0; i<apps.size(); i++)labels[i]=apps.get(i).loadLabel(activity.getPackageManager())+" • "+apps.get(i).activityInfo.packageName;
        new AlertDialog.Builder(activity).setTitle("Choose app").setItems(labels,(d,i)->e.setText(apps.get(i).activityInfo.packageName)).show();
    }
    void launch(Intent i) {
        try {
            activity.startActivity(i);
        } catch(Exception e) {
            toast("Cannot open: "+e.getMessage());
        }
    }
    void launch(Intent i,int request) {
        try {
            activity.startActivityForResult(i,request);
        } catch(Exception e) {
            toast("Cannot open: "+e.getMessage());
        }
    }
}
