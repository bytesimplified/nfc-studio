package com.nfcstudio.app.ui;
import org.junit.Test;
import static org.junit.Assert.*;
public class StudioPaletteTest {
    @Test public void amoledUsesTrueBlack(){assertEquals(0xff000000,StudioPalette.of("amoled",false).background);}
    @Test public void systemTracksConfiguration(){assertTrue(StudioPalette.of("system",true).dark);assertFalse(StudioPalette.of("system",false).dark);}
    @Test public void lightAndMidnightAreDistinct(){assertFalse(StudioPalette.of("light",true).dark);assertTrue(StudioPalette.of("dark",false).dark);assertNotEquals(StudioPalette.of("amoled",true).background,StudioPalette.of("dark",true).background);}
}
