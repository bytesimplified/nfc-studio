package com.nfcstudio.app.data;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class BackupCodecTest {
    private static final String ID="1234567890abcdef";
    private JSONObject backup() throws Exception {
        JSONObject action=new JSONObject().put("type","Show message").put("value","Hello");
        JSONObject profile=new JSONObject().put("name","Test").put("actions",new JSONArray().put(action));
        return BackupCodec.encode(new JSONObject().put(ID,profile),new JSONObject().put("ABCD",ID));
    }
    @Test public void roundTrip() throws Exception {
        JSONObject imported=BackupCodec.decode(backup().toString());
        assertEquals(ID,imported.getJSONObject("bindings").getString("ABCD"));
        assertEquals("Test",imported.getJSONObject("profiles").getJSONObject(ID).getString("name"));
    }
    @Test public void rejectsUnknownVersion() throws Exception {
        JSONObject b=backup().put("version",2);
        assertThrows(Exception.class,()->BackupCodec.decode(b.toString()));
    }
    @Test public void rejectsDanglingBinding() throws Exception {
        JSONObject b=backup();b.getJSONObject("bindings").put("ABCD","missing");
        assertThrows(Exception.class,()->BackupCodec.decode(b.toString()));
    }
    @Test public void rejectsInvalidAction() throws Exception {
        JSONObject b=backup();b.getJSONObject("profiles").getJSONObject(ID).getJSONArray("actions").getJSONObject(0).put("type","Unrecognized");
        assertThrows(Exception.class,()->BackupCodec.decode(b.toString()));
    }
}
