package com.nfcstudio.app.ui;
import android.content.Intent;
import android.provider.Settings;
import android.widget.*;
import org.json.JSONObject;
import java.util.Iterator;
import static com.nfcstudio.app.nfc.RecordCodec.empty;

final class HomeScreen {
    private final StudioUi ui;
    HomeScreen(StudioUi ui){this.ui=ui;}
    void home(){
        if(ui.tags.isBusy()){ui.toast("NFC operation in progress");return;}
        ui.tags.mode="auto";ui.tags.staged=null;ui.tags.selectedId=null;
        ui.page("NFC Studio","Tap. Connect. Automate.");
        boolean ready=ui.nfc!=null&&ui.nfc.isEnabled();
        LinearLayout status=ui.row();status.setBackground(ui.shape(ui.palette.surface,40));ui.pad(status,10);
        status.addView(ui.iconBadge(ready?"check":"scan",ready?ui.palette.success:ui.palette.amber,15),new LinearLayout.LayoutParams(ui.dp(28),ui.dp(28)));
        ui.status=ui.bold(ui.nfc==null?"NFC hardware not detected":ready?"NFC is enabled":"NFC is disabled",13,ready?ui.palette.success:ui.palette.amber);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=ui.dp(8);status.addView(ui.status,lp);ui.add(ui.root,status,14);
        if(ui.nfc!=null&&!ready)ui.button("Enable NFC",()->ui.launch(new Intent(Settings.ACTION_NFC_SETTINGS)));
        ui.hero("Ready to tap","Discover what is on your NFC tag.","Scan now",()->ui.tags.waitFor("read",null,null,"Hold a tag against the back of your phone."));
        ui.section("Tag toolkit");
        int p=ui.palette.primary,l=ui.palette.lilac,s=ui.palette.success,a=ui.palette.amber;
        ui.tileGrid(new String[]{"scan","write","copy","erase"},new String[]{"Read","Write","Copy","Erase"},
            new String[]{"Inspect tag data","Program new tags","Duplicate records","Reuse your tags"},new int[]{p,l,s,a},new Runnable[]{
                ()->ui.tags.waitFor("read",null,null,"Hold a tag against the back of your phone."),
                ui.tags::writeMenu,()->ui.tags.waitFor("copy",null,null,"Scan the source tag first."),
                ()->ui.confirm("Erase tag?","This replaces existing NDEF data with an empty record.",()->ui.tags.waitFor("write",empty(),null,"Tap the tag to erase."))});
        ui.section("Your routines");ui.metricPair("Saved routines",String.valueOf(ui.store.profiles().length()),"Registered tags",String.valueOf(ui.store.bindings().length()));
        Iterator<String> keys=ui.store.profiles().keys();int count=0;
        while(keys.hasNext()&&count<3){String id=keys.next();JSONObject profile=ui.store.profiles().optJSONObject(id);if(profile!=null){ui.automations.profileCard(id,profile,false);count++;}}
        if(count==0){LinearLayout card=ui.card();ui.add(card,ui.bold("Make your first routine",17,ui.palette.text),0);ui.add(card,ui.text("One tap can open an app, adjust your sound and more.",14,ui.palette.muted),8);ui.actionButton(card,"Create automation",()->ui.automations.editor(null),true);ui.cardToRoot(card);}
        else ui.button("View all routines",ui.automations::listProfiles);
        ui.section("More tools");
        ui.item("lock","Lock tag permanently","Irreversible • compatible tags only",ui.palette.amber,()->ui.confirm("Permanent read-only lock","This cannot be undone on most tags. Continue only for a tag you own.",()->ui.tags.waitFor("lock",null,null,"Tap the tag to lock it.")));
        ui.item("history","Scan / execution history","Inspect recent activity",ui.palette.primary,ui.settings::showHistory);
    }
}
