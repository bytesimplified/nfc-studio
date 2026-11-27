package com.nfcstudio.app.data;
import org.json.*;
import java.util.*;
import java.io.IOException;
import com.nfcstudio.app.automation.ActionValidator;
import static com.nfcstudio.app.data.JsonValues.*;
public final class BackupCodec {
    private BackupCodec() {
    }
    public static JSONObject encode(JSONObject profiles,JSONObject bindings) {
        JSONObject b=new JSONObject();
        put(b,"version",1);
        put(b,"profiles",profiles);
        put(b,"bindings",bindings);
        return b;
    }
    public static JSONObject decode(String json)throws Exception {
        JSONObject b=new JSONObject(json);
        if(b.optInt("version",-1)!=1)throw new IOException("Unsupported backup version");
        JSONObject p=b.getJSONObject("profiles"),t=b.getJSONObject("bindings");
        Iterator<String> keys=p.keys();
        while(keys.hasNext()) {
            String k=keys.next();
            if(!k.matches("[a-f0-9]{16}"))throw new IOException("Invalid profile ID");
            JSONObject profile=p.getJSONObject(k);
            if(profile.getString("name").trim().isEmpty())throw new IOException("Missing profile name");
            JSONArray a=profile.getJSONArray("actions");
            if(a.length()==0||a.length()>100)throw new IOException("Invalid action count");
            for(int i=0; i<a.length(); i++) {
                JSONObject x=a.getJSONObject(i);
                ActionValidator.validate(x.getString("type"),x.getString("value"));
            }
        }
        Iterator<String> tags=t.keys();
        while(tags.hasNext()) {
            String k=tags.next();
            if(!k.matches("[A-F0-9]+")||!p.has(t.getString(k)))throw new IOException("Invalid tag binding");
        }
        return b;
    }
}
