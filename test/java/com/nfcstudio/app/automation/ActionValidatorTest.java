package com.nfcstudio.app.automation;
import org.junit.Test;
import static org.junit.Assert.*;
public class ActionValidatorTest {
    @Test public void validActions() throws Exception {
        ActionValidator.validate("Media volume","100");
        ActionValidator.validate("Ring mode","vibrate");
        ActionValidator.validate("Set alarm","23:59");
        ActionValidator.validate("Open link","geo:17.385,78.4867");
        ActionValidator.validate("HTTPS webhook","https://example.com/hook");
        ActionValidator.validate("Wi-Fi controls","");
    }
    @Test public void rejectsInvalidActions() {
        assertThrows(Exception.class,()->ActionValidator.validate("Media volume","101"));
        assertThrows(Exception.class,()->ActionValidator.validate("Delay","-1"));
        assertThrows(Exception.class,()->ActionValidator.validate("Set alarm","24:00"));
        assertThrows(Exception.class,()->ActionValidator.validate("HTTPS webhook","http://example.com"));
        assertThrows(Exception.class,()->ActionValidator.validate("Open link","javascript:alert(1)"));
        assertThrows(Exception.class,()->ActionValidator.validate("Unknown",""));
    }
}
