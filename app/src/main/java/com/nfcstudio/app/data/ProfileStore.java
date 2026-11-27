package com.nfcstudio.app.data;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;
import static com.nfcstudio.app.data.JsonValues.*;
public final class ProfileStore {
    private final SharedPreferences prefs;
    private JSONObject profiles,bindings;
    private JSONArray history;
    public ProfileStore(SharedPreferences prefs) {
        this.prefs=prefs;
        profiles=object(prefs.getString("profiles","{}"));
        bindings=object(prefs.getString("bindings","{}"));
        history=array(prefs.getString("history","[]"));
    }
    public JSONObject profiles() {
        return profiles;
    }
    public JSONObject bindings() {
        return bindings;
    }
    public JSONArray history() {
        return history;
    }
    public SharedPreferences preferences() {
        return prefs;
    }
    public void replace(JSONObject p,JSONObject b) {
        profiles=p;
        bindings=b;
        for(String key:prefs.getAll().keySet())if(key.startsWith("snapshot_"))prefs.edit().remove(key).apply();
        save();
    }
    public void clearHistory() {
        history=new JSONArray();
        save();
    }
    public void save() {
        prefs.edit().putString("profiles",profiles.toString()).putString("bindings",bindings.toString()).putString("history",history.toString()).apply();
    }
    public void log(String s) {
        JSONArray next=new JSONArray();
        next.put(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ROOT).format(new Date())+" • "+s);
        for(int i=0; i<Math.min(149,history.length()); i++)next.put(history.optString(i));
        history=next;
        save();
    }
}
