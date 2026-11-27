package com.nfcstudio.app.ui;
import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Scalable, resolution-independent line illustrations. */
final class GlyphView extends View {
    private final String glyph;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    GlyphView(Context context,String glyph,int color){
        super(context);this.glyph=glyph;paint.setColor(color);paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeWidth(1.75f);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private void path(Canvas c,float...p){
        Path x=new Path();x.moveTo(p[0],p[1]);for(int i=2;i<p.length;i+=2)x.lineTo(p[i],p[i+1]);c.drawPath(x,paint);
    }
    private void rect(Canvas c,float a,float b,float d,float e,float r){c.drawRoundRect(a,b,d,e,r,r,paint);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float s=Math.min(getWidth(),getHeight())/24f;c.save();c.translate((getWidth()-24*s)/2,(getHeight()-24*s)/2);c.scale(s,s);
        switch(glyph){
            case "home":path(c,2,11,12,3,22,11);path(c,5,9,5,21,10,21,10,15,14,15,14,21,19,21,19,9);break;
            case "tags":path(c,3,4,13,4,21,12,12,21,3,12,3,4);c.drawCircle(7,8,1.5f,paint);break;
            case "scan":path(c,2,8,2,3,8,3);path(c,16,3,22,3,22,8);path(c,22,16,22,21,16,21);path(c,8,21,2,21,2,16);c.drawLine(5,12,19,12,paint);break;
            case "bolt":path(c,13,2,4,14,11,14,10,22,21,10,14,10,13,2);break;
            case "settings":c.drawCircle(12,12,3.4f,paint);c.drawCircle(12,12,8,paint);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(12+(float)Math.cos(a)*8,12+(float)Math.sin(a)*8,12+(float)Math.cos(a)*11,12+(float)Math.sin(a)*11,paint);}break;
            case "write":rect(c,3,3,18,21,3);path(c,9,15,10,11,19,2,22,5,13,14,9,15);break;
            case "copy":rect(c,8,8,21,21,3);path(c,16,5,16,3,3,3,3,16,5,16);break;
            case "erase":path(c,3,15,13,4,21,12,12,21,8,21,3,15);path(c,8,10,16,18);break;
            case "lock":rect(c,5,10,19,22,3);c.drawArc(8,2,16,13,180,180,false,paint);break;
            case "plus":path(c,12,4,12,20);path(c,4,12,20,12);break;
            case "arrow":path(c,9,5,16,12,9,19);break;
            case "back":path(c,15,5,8,12,15,19);break;
            case "check":path(c,4,12,10,18,20,6);break;
            case "moon":c.drawArc(3,3,21,21,40,280,false,paint);c.drawArc(11,1,24,14,70,230,false,paint);break;
            case "work":rect(c,3,7,21,21,3);rect(c,8,3,16,7,2);c.drawLine(3,13,21,13,paint);break;
            case "car":path(c,3,12,6,5,18,5,21,12,21,20,3,20,3,12);c.drawLine(3,12,21,12,paint);c.drawCircle(7,16,1,paint);c.drawCircle(17,16,1,paint);break;
            case "volume":path(c,3,9,8,9,13,4,13,20,8,15,3,15,3,9);c.drawArc(10,5,23,19,-55,110,false,paint);break;
            case "sun":c.drawCircle(12,12,4,paint);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(12+(float)Math.cos(a)*7,12+(float)Math.sin(a)*7,12+(float)Math.cos(a)*10,12+(float)Math.sin(a)*10,paint);}break;
            case "clock":c.drawCircle(12,12,9,paint);path(c,12,6,12,12,16,15);break;
            case "app":rect(c,6,2,18,22,3);c.drawLine(10,19,14,19,paint);break;
            case "globe":c.drawCircle(12,12,9,paint);c.drawOval(8,3,16,21,paint);c.drawLine(3,12,21,12,paint);break;
            case "wifi":c.drawArc(1,5,23,24,220,100,false,paint);c.drawArc(5,10,19,24,220,100,false,paint);c.drawCircle(12,19,1,paint);break;
            case "text":rect(c,5,3,19,21,2);path(c,8,8,16,8);path(c,8,12,16,12);path(c,8,16,14,16);break;
            case "history":c.drawArc(3,3,21,21,215,300,false,paint);path(c,3,4,3,10,9,10);path(c,12,6,12,12,16,14);break;
            default:rect(c,5,3,19,21,3);c.drawArc(4,8,16,16,-55,110,false,paint);c.drawArc(7,6,20,18,-55,110,false,paint);break;
        }c.restore();
    }
}
