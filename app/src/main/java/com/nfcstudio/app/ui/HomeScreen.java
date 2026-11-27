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
final class HomeScreen {
    private final StudioUi ui;
    private final Activity activity;
    private final ProfileStore store;
    private final AutomationEngine engine;
    HomeScreen(StudioUi ui) {
        this.ui=ui;
        this.activity=ui.activity;
        this.store=ui.store;
        this.engine=ui.engine;
    }
    void home() {
        ui.tags.mode="auto";
        ui.tags.staged=null;
        ui.tags.selectedId=null;
        ui.page("NFC Studio","Read • Write • Automate");
        ui.status=ui.text(ui.nfc==null?"NFC hardware not detected":ui.nfc.isEnabled()?"NFC is enabled — ready to tap":"NFC is disabled",16,ACCENT);
        ui.root.addView(ui.status);
        if(ui.nfc!=null&&!ui.nfc.isEnabled())ui.button("Enable NFC",()->ui.launch(new Intent(Settings.ACTION_NFC_SETTINGS)));
        ui.section("Tag tools");
        ui.button("Read / inspect a tag",()->ui.tags.waitFor("read",null,null,"Hold an NFC tag against your phone."));
        ui.button("Write standard NFC data",ui.tags::writeMenu);
        ui.button("Copy NDEF records",()->ui.tags.waitFor("copy",null,null,"Scan the source tag first."));
        ui.button("Erase / reuse tag",()->ui.confirm("Erase tag?","This replaces existing NDEF data with an empty record.",()->ui.tags.waitFor("write",empty(),null,"Tap the tag to erase.")));
        ui.button("Lock tag permanently",()->ui.confirm("Permanent read-only lock","This cannot be undone on most tags. Continue only for a tag you own.",()->ui.tags.waitFor("lock",null,null,"Tap the tag to lock it.")));
        ui.section("Automations");
        ui.button("Create automation",()->ui.automations.editor(null));
        ui.button("Saved automations ("+store.profiles().length()+")",ui.automations::listProfiles);
        ui.button("My tags ("+store.bindings().length()+")",ui.tags::listTags);
        ui.button("Scan / execution history",ui.settings::showHistory);
        ui.button("Permissions & settings",ui.settings::permissions);
        ui.button("Export configuration",()->ui.launch(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"nfc-studio-backup.json"),101));
        ui.button("Import configuration",()->ui.confirm("Replace local configuration?","Import will replace local profiles and tag bindings. Only import a file you trust.",()->ui.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE),102)));
    }
}
