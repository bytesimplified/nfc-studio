package com.nfcstudio.app.ui;

import android.animation.ValueAnimator;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.nfcstudio.app.data.ProfileStore;
import com.nfcstudio.app.automation.AutomationEngine;
import java.util.*;

/** Shared visual components and navigation; domain services remain independent. */
public final class StudioUi {
    final Activity activity;
    final ProfileStore store;
    final AutomationEngine engine;
    final NfcAdapter nfc;
    final HomeScreen home;
    final TagScreen tags;
    final AutomationScreen automations;
    final SettingsScreen settings;
    final StudioPalette palette;
    LinearLayout root;
    TextView status;
    private FrameLayout content;
    private LinearLayout shell,nav;
    private ScrollView scroll;
    private String selected="home";

    public StudioUi(Activity activity,ProfileStore store,NfcAdapter nfc){
        this.activity=activity;this.store=store;this.nfc=nfc;
        String pref=store.preferences().getString("appearance","amoled");
        boolean systemDark=(activity.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        palette=StudioPalette.of(pref,systemDark);
        engine=new AutomationEngine(activity,store);home=new HomeScreen(this);tags=new TagScreen(this);
        automations=new AutomationScreen(this);settings=new SettingsScreen(this);
        activity.getWindow().setStatusBarColor(palette.background);
        activity.getWindow().setNavigationBarColor(palette.background);
        activity.getWindow().getDecorView().setSystemUiVisibility(palette.dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    public void showHome(){home.home();}
    public void showSettings(){navigate("settings");}
    public void handle(Intent intent){tags.handle(intent);}
    public void onActivityResult(int request,int result,Intent data){settings.onActivityResult(request,result,data);}
    public void close(){tags.close();engine.close();}
    int dp(float n){return (int)(activity.getResources().getDisplayMetrics().density*n+.5f);}
    TextView text(String s,int size,int color){
        TextView t=new TextView(activity);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        t.setIncludeFontPadding(false);t.setLineSpacing(dp(3),1f);return t;
    }
    TextView bold(String s,int size,int color){TextView t=text(s,size,color);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return t;}
    LinearLayout column(){LinearLayout v=new LinearLayout(activity);v.setOrientation(LinearLayout.VERTICAL);return v;}
    LinearLayout row(){LinearLayout v=new LinearLayout(activity);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    void pad(View v,int p){v.setPadding(dp(p),dp(p),dp(p),dp(p));}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    GradientDrawable outlined(int color,int radius){GradientDrawable d=shape(color,radius);d.setStroke(dp(1),palette.border);return d;}
    GradientDrawable gradient(int a,int b,int r){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(r));return d;}
    void clickable(View v,Drawable background,Runnable action){
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(palette.dark?0x33ffffff:0x22355da5),background,null));
        v.setClickable(true);v.setFocusable(true);v.setOnClickListener(w->{w.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);action.run();});
    }
    void add(LinearLayout parent,View child,int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);parent.addView(child,p);
    }
    void space(LinearLayout parent,int size){View v=new View(activity);parent.addView(v,new LinearLayout.LayoutParams(1,dp(size)));}
    void heading(LinearLayout parent,String title,String sub){
        add(parent,bold(title,30,palette.text),0);if(!sub.isEmpty())add(parent,text(sub,14,palette.muted),7);
    }
    void section(String s){add(root,bold(s,19,palette.text),24);}
    void label(String s){TextView t=text(s,15,palette.text);t.setTextIsSelectable(true);add(root,t,10);}
    void page(String title,String sub){
        status=null;
        selected=title.equals("NFC Studio")?"home":title.equals("My tags")?"tags":title.equals("Ready to scan")||title.equals("Tag details")?"scan":title.equals("Settings")||title.equals("Permissions")||title.equals("History")?"settings":"bolt";
        boolean top=title.equals("NFC Studio")||title.equals("My tags")||title.equals("Automations")||title.equals("Settings");
        shell=column();shell.setBackgroundColor(palette.background);
        content=new FrameLayout(activity);shell.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        scroll=new ScrollView(activity);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
        content.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        root=column();root.setPadding(dp(20),dp(20),dp(20),dp(28));root.setClipChildren(false);
        FrameLayout width=new FrameLayout(activity);scroll.addView(width,new ScrollView.LayoutParams(-1,-2));
        int max=dp(640),screen=activity.getResources().getDisplayMetrics().widthPixels;
        FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(Math.min(max,screen),-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        width.addView(root,rp);
        if(!top){
            LinearLayout back=row();GlyphView icon=new GlyphView(activity,"back",palette.primary);back.addView(icon,new LinearLayout.LayoutParams(dp(24),dp(24)));
            TextView backText=bold("Back to dashboard",13,palette.primary);backText.setPadding(dp(6),0,0,0);back.addView(backText);
            back.setMinimumHeight(dp(48));clickable(back,shape(palette.background,14),home::home);add(root,back,0);
        }
        heading(root,title,sub);
        nav=row();nav.setPadding(dp(8),dp(9),dp(8),dp(10));nav.setBackground(outlined(palette.surface,0));
        String[] ids={"home","tags","scan","bolt","settings"},names={"Home","Tags","Scan","Routines","Settings"};
        for(int i=0;i<ids.length;i++){final String id=ids[i];LinearLayout tab=column();tab.setGravity(Gravity.CENTER);tab.setMinimumHeight(dp(58));
            FrameLayout pill=new FrameLayout(activity);pill.setBackground(shape(id.equals(selected)?palette.elevated:Color.TRANSPARENT,18));
            GlyphView glyph=new GlyphView(activity,id,id.equals(selected)?palette.primary:palette.muted);pill.addView(glyph,new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
            tab.addView(pill,new LinearLayout.LayoutParams(dp(52),dp(34)));
            TextView caption=text(names[i],11,id.equals(selected)?palette.primary:palette.muted);caption.setGravity(Gravity.CENTER);tab.addView(caption);
            tab.setContentDescription("Navigate "+names[i]);clickable(tab,shape(Color.TRANSPARENT,16),()->navigate(id));nav.addView(tab,new LinearLayout.LayoutParams(0,-2,1));
        }
        shell.addView(nav,new LinearLayout.LayoutParams(-1,-2));activity.setContentView(shell);
        if(ValueAnimator.areAnimatorsEnabled()){root.setAlpha(0f);root.setTranslationY(dp(6));root.animate().alpha(1f).translationY(0).setDuration(180).start();}
    }
    private void navigate(String id){
        if(tags.isBusy()){toast("Keep the tag in place until the operation finishes.");return;}
        if(shell!=null)((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(shell.getWindowToken(),0);
        tags.mode="auto";tags.staged=null;tags.selectedId=null;
        switch(id){case "tags":tags.listTags();break;case "scan":tags.waitFor("read",null,null,"Hold a tag against the back of your phone.");break;case "bolt":automations.listProfiles();break;case "settings":settings.dashboard();break;default:home.home();}
    }
    LinearLayout card(){LinearLayout v=column();pad(v,16);v.setBackground(outlined(palette.surface,22));return v;}
    void cardToRoot(LinearLayout v){add(root,v,12);}
    FrameLayout iconBadge(String icon,int color,int size){
        FrameLayout f=new FrameLayout(activity);f.setBackground(shape(palette.elevated,15));
        GlyphView g=new GlyphView(activity,icon,color);f.addView(g,new FrameLayout.LayoutParams(dp(size),dp(size),Gravity.CENTER));return f;
    }
    void item(String icon,String title,String subtitle,int color,Runnable run){
        LinearLayout r=row();pad(r,14);r.setMinimumHeight(dp(72));
        r.addView(iconBadge(icon,color,24),new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout words=column();LinearLayout.LayoutParams w=new LinearLayout.LayoutParams(0,-2,1);w.leftMargin=dp(13);r.addView(words,w);
        add(words,bold(title,16,palette.text),0);if(!subtitle.isEmpty())add(words,text(subtitle,13,palette.muted),5);
        r.addView(new GlyphView(activity,"arrow",palette.muted),new LinearLayout.LayoutParams(dp(20),dp(20)));
        clickable(r,outlined(palette.surface,20),run);add(root,r,9);
    }
    void tileGrid(String[] icons,String[] titles,String[] desc,int[] colors,Runnable[] actions){
        for(int i=0;i<titles.length;i+=2){LinearLayout r=row();r.setGravity(Gravity.TOP);for(int j=i;j<Math.min(i+2,titles.length);j++){
            final int index=j;LinearLayout v=column();pad(v,15);v.setMinimumHeight(dp(116));
            v.addView(iconBadge(icons[j],colors[j],25),new LinearLayout.LayoutParams(dp(44),dp(44)));
            add(v,bold(titles[j],16,palette.text),13);add(v,text(desc[j],12,palette.muted),5);
            clickable(v,outlined(palette.surface,22),actions[index]);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);if(j>i)lp.leftMargin=dp(10);r.addView(v,lp);
        }if(i+1==titles.length)r.addView(new View(activity),new LinearLayout.LayoutParams(0,1,1));add(root,r,10);}
    }
    void metricPair(String a,String av,String b,String bv){
        LinearLayout r=row();String[] names={a,b},values={av,bv};
        for(int i=0;i<2;i++){LinearLayout v=card();add(v,text(names[i],12,palette.muted),0);add(v,bold(values[i],30,palette.primary),7);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);if(i==1)p.leftMargin=dp(10);r.addView(v,p);}add(root,r,14);
    }
    void hero(String title,String desc,String button,Runnable action){
        LinearLayout r=row();pad(r,20);r.setMinimumHeight(dp(178));
        LinearLayout words=column();r.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        TextView eyebrow=bold("NFC  /  QUICK SCAN",11,0xff344f79);eyebrow.setLetterSpacing(.12f);add(words,eyebrow,0);
        add(words,bold(title,29,0xff14294a),12);add(words,text(desc,14,0xff344f79),8);
        TextView cta=bold(button,14,Color.WHITE);pad(cta,12);cta.setGravity(Gravity.CENTER);cta.setBackground(shape(0xff14294a,40));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.topMargin=dp(15);words.addView(cta,p);
        r.addView(new PulseView(activity,0xff14294a,true),new LinearLayout.LayoutParams(dp(106),dp(126)));
        clickable(r,gradient(0xffd5e0ff,0xff93b2f2,28),action);add(root,r,16);
    }
    void pulse(){
        PulseView view=new PulseView(activity,palette.primary,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(220),dp(220));p.gravity=Gravity.CENTER_HORIZONTAL;p.topMargin=dp(14);root.addView(view,p);
    }
    void button(String s,Runnable r){actionButton(root,s,r,false);}
    void primary(String s,Runnable r){actionButton(root,s,r,true);}
    Button actionButton(LinearLayout parent,String s,Runnable r,boolean primary){
        Button b=new Button(activity);b.setText(s);b.setAllCaps(false);b.setTextSize(15);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setTextColor(primary?palette.onPrimary:palette.primary);
        b.setMinHeight(dp(52));b.setMinimumHeight(dp(52));b.setElevation(0);b.setStateListAnimator(null);pad(b,12);
        b.setBackgroundTintList(null);clickable(b,primary?shape(palette.primary,28):outlined(palette.surface,20),r);add(parent,b,10);return b;
    }
    EditText edit(String hint,String value){EditText e=new EditText(activity);styleEdit(e);e.setHint(hint);e.setText(value);add(root,e,12);return e;}
    void styleEdit(EditText e){e.setTextColor(palette.text);e.setHintTextColor(palette.muted);e.setTextSize(15);e.setBackground(outlined(palette.elevated,16));pad(e,15);e.setMinHeight(dp(56));}
    void styleCheck(CompoundButton b){b.setTextColor(palette.text);b.setButtonTintList(ColorStateList.valueOf(palette.primary));b.setTextSize(14);b.setMinHeight(dp(48));}
    Spinner spinner(String[] values){
        Spinner s=new Spinner(activity);s.setBackground(outlined(palette.elevated,16));pad(s,12);s.setMinimumHeight(dp(54));
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,values){
            @Override public View getView(int p,View v,android.view.ViewGroup parent){TextView t=(TextView)super.getView(p,v,parent);t.setTextColor(palette.text);t.setTextSize(15);return t;}
            @Override public View getDropDownView(int p,View v,android.view.ViewGroup parent){TextView t=(TextView)super.getDropDownView(p,v,parent);t.setTextColor(palette.text);t.setBackgroundColor(palette.surface);t.setMinHeight(dp(48));return t;}
        };s.setAdapter(adapter);return s;
    }
    void toast(String s){Toast.makeText(activity,s==null?"Operation failed":s,Toast.LENGTH_LONG).show();}
    void report(String s){if(status!=null)status.setText(s);toast(s);}
    void confirm(String t,String s,Runnable yes){new AlertDialog.Builder(activity).setTitle(t).setMessage(s).setNegativeButton("Cancel",null).setPositiveButton("Continue",(d,w)->yes.run()).show();}
    void pickApp(EditText e){
        Intent q=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps=activity.getPackageManager().queryIntentActivities(q,0);apps.sort(Comparator.comparing(a->a.loadLabel(activity.getPackageManager()).toString().toLowerCase(Locale.ROOT)));
        ListView list=new ListView(activity);list.setDividerHeight(0);
        BaseAdapter adapter=new BaseAdapter(){
            public int getCount(){return apps.size();}public Object getItem(int p){return apps.get(p);}public long getItemId(int p){return p;}
            public View getView(int p,View old,android.view.ViewGroup parent){
                ResolveInfo a=apps.get(p);LinearLayout r=row();pad(r,13);ImageView icon=new ImageView(activity);icon.setImageDrawable(a.loadIcon(activity.getPackageManager()));r.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(42)));
                LinearLayout words=column();LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(14);r.addView(words,lp);add(words,bold(a.loadLabel(activity.getPackageManager()).toString(),15,palette.text),0);add(words,text(a.activityInfo.packageName,11,palette.muted),4);return r;
            }};list.setAdapter(adapter);AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Choose an installed app").setView(list).setNegativeButton("Cancel",null).create();
        list.setOnItemClickListener((p,v,i,l)->{e.setText(apps.get(i).activityInfo.packageName);dialog.dismiss();});dialog.show();
    }
    void launch(Intent i){try{activity.startActivity(i);}catch(Exception e){toast("Cannot open: "+e.getMessage());}}
    void launch(Intent i,int request){try{activity.startActivityForResult(i,request);}catch(Exception e){toast("Cannot open: "+e.getMessage());}}
}
