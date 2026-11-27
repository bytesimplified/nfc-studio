package com.nfcstudio.app.ui;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Soft radar animation; only runs while the illustration is attached and visible. */
final class PulseView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int color;private final boolean hero;
    private ValueAnimator animator;private float phase;
    PulseView(Context context,int color,boolean hero){super(context);this.color=color;this.hero=hero;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();updateMotion();}
    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);updateMotion();}
    private void stop(){if(animator!=null){animator.cancel();animator=null;}}
    private void updateMotion(){if(!isAttachedToWindow()||getWindowVisibility()!=VISIBLE||!ValueAnimator.areAnimatorsEnabled()){stop();return;}if(animator!=null)return;
        animator=ValueAnimator.ofFloat(0,1);animator.setDuration(2400);animator.setRepeatCount(ValueAnimator.INFINITE);animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});animator.start();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)*.91f;
        p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,r*.012f));
        for(int i=0;i<3;i++){float q=(phase+i/3f)%1f;p.setAlpha((int)(85*(1-q)+12));c.drawCircle(cx,cy,r*(.48f+.5f*q),p);}
        p.setStyle(Paint.Style.FILL);p.setAlpha(hero?27:34);c.drawCircle(cx,cy,r*.51f,p);
        p.setAlpha(255);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(r*.037f);p.setStrokeCap(Paint.Cap.ROUND);
        c.save();c.rotate(-12,cx,cy);float w=r*.42f,h=r*.67f;
        c.drawRoundRect(cx-w/2,cy-h/2,cx+w/2,cy+h/2,r*.085f,r*.085f,p);
        c.drawLine(cx-w*.15f,cy+h*.36f,cx+w*.15f,cy+h*.36f,p);
        for(int i=1;i<=3;i++){float a=r*(.045f+i*.043f);c.drawArc(cx-a,cy-a,cx+a,cy+a,-60,120,false,p);}c.restore();
    }
}
