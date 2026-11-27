package com.nfcstudio.app.nfc;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class RecordCodecDeviceTest {
    @Test public void emptyRecordRoundTrip() throws Exception {
        NdefMessage original=RecordCodec.empty();
        NdefMessage parsed=new NdefMessage(original.toByteArray());
        assertEquals(NdefRecord.TNF_EMPTY,parsed.getRecords()[0].getTnf());
    }
    @Test public void wifiRecordUsesWscMime() throws Exception {
        NdefRecord record=RecordCodec.wifi("TestNetwork","testpassword");
        assertEquals("application/vnd.wfa.wsc",record.toMimeType());
        assertTrue(record.getPayload().length>20);
    }
}
