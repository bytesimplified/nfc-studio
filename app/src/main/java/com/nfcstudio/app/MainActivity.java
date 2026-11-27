package com.nfcstudio.app;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import com.nfcstudio.app.data.ProfileStore;
import com.nfcstudio.app.ui.StudioUi;
public final class MainActivity extends Activity {
    private NfcAdapter nfc;
    private PendingIntent pending;
    private StudioUi ui;
    @Override public void onCreate(Bundle state) {
        SharedPreferences pref=getSharedPreferences("studio",MODE_PRIVATE);
        String appearance=pref.getString("appearance","amoled");
        boolean light="light".equals(appearance)||("system".equals(appearance)&&(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)!=Configuration.UI_MODE_NIGHT_YES);
        setTheme(light?R.style.AppTheme_Light:R.style.AppTheme);
        super.onCreate(state);
        nfc = NfcAdapter.getDefaultAdapter(this);
        pending = PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
        ui = new StudioUi(this, new ProfileStore(getSharedPreferences("studio",MODE_PRIVATE)), nfc);
        boolean settings=pref.getBoolean("return_to_settings",false);pref.edit().remove("return_to_settings").apply();
        if(settings)ui.showSettings();else ui.showHome();
        if(state==null)ui.handle(getIntent());
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        ui.handle(intent);
    }
    @Override protected void onResume() {
        super.onResume();
        if(nfc!=null&&nfc.isEnabled())try {
            nfc.enableForegroundDispatch(this,pending,null,null);
        } catch(IllegalStateException ignored) {
        }
    }
    @Override protected void onPause() {
        if(nfc!=null)try {
            nfc.disableForegroundDispatch(this);
        } catch(IllegalStateException ignored) {
        }
        super.onPause();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        ui.onActivityResult(request,result,data);
    }
    @Override public void onBackPressed() {
        ui.showHome();
    }
    @Override protected void onDestroy() {
        if(ui!=null)ui.close();
        super.onDestroy();
    }
}
