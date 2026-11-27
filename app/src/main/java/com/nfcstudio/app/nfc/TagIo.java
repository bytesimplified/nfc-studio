package com.nfcstudio.app.nfc;
import android.nfc.*;
import android.nfc.tech.*;
import java.io.IOException;
import java.util.Arrays;
public final class TagIo {
    public static final class Result {
        public final NdefMessage message;
        public final String details;
        public final boolean verified;
        Result(NdefMessage m,String d,boolean v) {
            message=m;
            details=d;
            verified=v;
        }
    }
    public Result scan(Tag tag,String uid,String op,NdefMessage msg)throws Exception {
        NdefMessage read=null;
        StringBuilder details=new StringBuilder("UID: "+uid+"\nTechnologies: "+String.join(", ",tag.getTechList()));
        Ndef n=Ndef.get(tag);
        if(n!=null) {
            details.append("\nType: ").append(n.getType()).append("\nCapacity: ").append(n.getMaxSize()).append(" bytes\nWritable: ").append(n.isWritable());
            n.connect();
            try {
                if(op.equals("write")) {
                    if(msg==null)throw new IOException("No staged record");
                    TagPolicy.requireWritable(n.isWritable(),n.getMaxSize(),msg.toByteArray().length);
                    n.writeNdefMessage(msg);
                    read=n.getNdefMessage();
                    if(read==null||!Arrays.equals(read.toByteArray(),msg.toByteArray()))throw new IOException("Write verification failed");
                } else if(op.equals("lock")) {
                    if(!n.canMakeReadOnly()||!n.makeReadOnly())throw new IOException("This tag cannot be locked");
                } else read=n.getNdefMessage();
            } finally {
                n.close();
            }
        } else if(op.equals("write")) {
            NdefFormatable f=NdefFormatable.get(tag);
            if(f==null)throw new IOException("Not NDEF-writable on this device");
            f.connect();
            try {
                f.format(msg);
            } finally {
                f.close();
            }
        } else if(op.equals("lock"))throw new IOException("Tag is not NDEF formatted");
        else details.append("\nNot NDEF formatted, or unsupported by this phone.");
        return new Result(read,details.toString(),n!=null);
    }
}
