package com.nfcstudio.app.data;
import org.json.*;
public final class JsonValues {
    private JsonValues() {
    }
    public static JSONObject object(String s) {
        try {
            return new JSONObject(s);
        } catch(Exception e) {
            return new JSONObject();
        }
    }
    public static JSONArray array(String s) {
        try {
            return new JSONArray(s);
        } catch(Exception e) {
            return new JSONArray();
        }
    }
    public static void put(JSONObject o,String k,Object v) {
        try {
            o.put(k,v);
        } catch(JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
