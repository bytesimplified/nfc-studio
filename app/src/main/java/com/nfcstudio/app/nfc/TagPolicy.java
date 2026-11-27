package com.nfcstudio.app.nfc;
import java.util.Locale;
public final class TagPolicy {
    private TagPolicy() {
    }
    public static boolean isDuplicate(String uid,String prior,long elapsed) {
        return uid.equals(prior)&&elapsed>=0&&elapsed<700;
    }
    public static boolean canTrigger(String profileId,String registeredId,boolean exists) {
        return profileId!=null&&profileId.equals(registeredId)&&exists;
    }
    public static void requireWritable(boolean writable,int capacity,int size)throws java.io.IOException {
        if(!writable)throw new java.io.IOException("Tag is read-only");
        if(size>capacity)throw new java.io.IOException("Record exceeds tag capacity");
    }
    public static String hex(byte[] b) {
        StringBuilder s=new StringBuilder();
        for(byte x:b)s.append(String.format(Locale.ROOT,"%02X",x&255));
        return s.toString();
    }
}
