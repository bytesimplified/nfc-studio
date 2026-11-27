package com.nfcstudio.app.ui;

/** Immutable color tokens. No Android dependency; usable in JVM tests. */
public final class StudioPalette {
    public final int background, surface, elevated, primary, onPrimary, text, muted, border, success, danger, lilac, amber;
    public final boolean dark;
    private StudioPalette(boolean dark,int background,int surface,int elevated,int primary,int onPrimary,int text,int muted,int border,int success,int danger,int lilac,int amber){
        this.dark=dark;this.background=background;this.surface=surface;this.elevated=elevated;
        this.primary=primary;this.onPrimary=onPrimary;this.text=text;this.muted=muted;this.border=border;
        this.success=success;this.danger=danger;this.lilac=lilac;this.amber=amber;
    }
    public static StudioPalette of(String preference,boolean systemDark){
        boolean dark="system".equals(preference)?systemDark:!"light".equals(preference);
        if(!dark)return new StudioPalette(false,0xfff3f5fa,0xffffffff,0xffe8edf7,0xff355da5,0xffffffff,0xff141c2c,0xff54647d,0xffd8dfed,0xff14734d,0xffb32635,0xff7354ab,0xff995a16);
        boolean amoled="amoled".equals(preference);
        return new StudioPalette(true,amoled?0xff000000:0xff101217,amoled?0xff101217:0xff1c2029,amoled?0xff181d28:0xff252d40,0xffb1c7fa,0xff14294a,0xfff2f5ff,0xffa8b3c8,0xff333c4d,0xff83ddb3,0xffffa3aa,0xffc5adff,0xfff9c68a);
    }
}
