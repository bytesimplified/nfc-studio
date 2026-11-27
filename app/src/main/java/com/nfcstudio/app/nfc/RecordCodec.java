package com.nfcstudio.app.nfc;
import android.nfc.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class RecordCodec {
    private RecordCodec() {
    }
    public static final String MIME="application/vnd.nfcstudio.profile";
    public static NdefMessage empty() {
        return new NdefMessage(new NdefRecord(NdefRecord.TNF_EMPTY,null,null,null));
    }
    static void tlv(DataOutputStream d,int type,byte[] v)throws IOException {
        d.writeShort(type);
        d.writeShort(v.length);
        d.write(v);
    }
    public static NdefRecord wifi(String ssid,String key)throws IOException {
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        DataOutputStream d=new DataOutputStream(b);
        tlv(d,0x1026,new byte[] {
            1
        });
        tlv(d,0x1045,ssid.getBytes(StandardCharsets.UTF_8));
        tlv(d,0x1003,new byte[] {
            0,0x20
        });
        tlv(d,0x100f,new byte[] {
            0,8
        });
        tlv(d,0x1027,key.getBytes(StandardCharsets.UTF_8));
        tlv(d,0x1020,new byte[] {
            -1,-1,-1,-1,-1,-1
        });
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        tlv(new DataOutputStream(out),0x100e,b.toByteArray());
        return NdefRecord.createMime("application/vnd.wfa.wsc",out.toByteArray());
    }
}
