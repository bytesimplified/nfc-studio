package com.nfcstudio.app.nfc;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
public class TagPolicyTest {
    @Test public void uidIsUnsignedHex() {assertEquals("0080FF",TagPolicy.hex(new byte[]{0,(byte)128,(byte)255}));}
    @Test public void duplicateWindow() {
        assertTrue(TagPolicy.isDuplicate("AA","AA",699));
        assertFalse(TagPolicy.isDuplicate("AA","AA",700));
        assertFalse(TagPolicy.isDuplicate("AA","BB",100));
    }
    @Test public void matchingRegisteredProfileRequired() {
        assertTrue(TagPolicy.canTrigger("id","id",true));
        assertFalse(TagPolicy.canTrigger("id","other",true));
        assertFalse(TagPolicy.canTrigger("id","id",false));
        assertFalse(TagPolicy.canTrigger(null,"id",true));
    }
    @Test public void refusesReadOnlyAndOversizedWrites() throws Exception {
        TagPolicy.requireWritable(true,144,144);
        assertThrows(IOException.class,()->TagPolicy.requireWritable(false,144,10));
        assertThrows(IOException.class,()->TagPolicy.requireWritable(true,144,145));
    }
}
