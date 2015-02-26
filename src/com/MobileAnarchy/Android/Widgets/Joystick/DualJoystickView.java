package com.MobileAnarchy.Android.Widgets.Joystick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class DualJoystickView extends LinearLayout {
    @SuppressWarnings("unused")
    private static final String TAG = DualJoystickView.class.getSimpleName();

    private final boolean D = false;
    private Paint dbgPaint1;

    private JoystickView stickL;
    private JoystickView stickR;

    private View padding;

    private int mViewWidth;
    private int mViewHeight;

    public DualJoystickView(Context context) {
        super(context);
        stickL = new JoystickView(context);
        stickR = new JoystickView(context);
        initDualJoystickView();
    }

    public DualJoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        stickL = new JoystickView(context, attrs);
        stickR = new JoystickView(context, attrs);
        initDualJoystickView();
    }

    private void initDualJoystickView() {
        setOrientation(LinearLayout.HORIZONTAL);

        if (D) {
            dbgPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
            dbgPaint1.setColor(Color.CYAN);
            dbgPaint1.setStrokeWidth(1);
            dbgPaint1.setStyle(Paint.Style.STROKE);
        }

        padding = new View(getContext());
    }

    @Override
    protected void onSizeChanged(int wNew, int hNew, int wOld, int hOld){
        super.onSizeChanged(wNew, hNew, wOld, hOld);
        mViewWidth = wNew;
        mViewHeight = hNew;

        drawJoysticks();
    }

    private void drawJoysticks() {
        removeView(stickL);
        removeView(padding);
        removeView(stickR);

        int joyHeight = Math.round(mViewHeight);
        int joyWidth = joyHeight;
        int paddingWidth = mViewWidth - (joyWidth * 2);

        // Layout fix for HP Touchpad
        if (paddingWidth < 0) {
            joyWidth = mViewWidth / 2;
            joyHeight = joyWidth;
            paddingWidth = mViewWidth - (joyWidth * 2);
        }

        LayoutParams joyParams = new LayoutParams(joyWidth, joyHeight);
        stickL.setLayoutParams(joyParams);
        stickR.setLayoutParams(joyParams);

        stickL.TAG = "L";
        stickR.TAG = "R";
        stickL.setPointerId(JoystickView.INVALID_POINTER_ID);
        stickR.setPointerId(JoystickView.INVALID_POINTER_ID);

        ViewGroup.LayoutParams padLParams = new ViewGroup.LayoutParams(paddingWidth, joyHeight);
        padding.setLayoutParams(padLParams);

        addView(stickL);
        addView(padding);
        addView(stickR);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        stickL.setTouchOffset(stickL.getLeft(), stickL.getTop());
        stickR.setTouchOffset(stickR.getLeft(), stickR.getTop());
    }

    public void setAutoReturnToCenter(boolean left, boolean right) {
        stickL.setAutoReturnToCenter(left);
        stickR.setAutoReturnToCenter(right);
    }

    public void setOnJoystickMovedListener(JoystickMovedListener left, JoystickMovedListener right) {
        stickL.setOnJoystickMovedListener(left);
        stickR.setOnJoystickMovedListener(right);
    }

    public void setOnJoystickClickedListener(JoystickClickedListener left, JoystickClickedListener right) {
        stickL.setOnJoystickClickedListener(left);
        stickR.setOnJoystickClickedListener(right);
    }

    public void setYAxisInverted(boolean leftYAxisInverted, boolean rightYAxisInverted) {
        stickL.setYAxisInverted(leftYAxisInverted);
        stickR.setYAxisInverted(rightYAxisInverted);
    }

    public void setMovementConstraint(int movementConstraint) {
        stickL.setMovementConstraint(movementConstraint);
        stickR.setMovementConstraint(movementConstraint);
    }

    public void setMovementRange(float movementRangeLeft, float movementRangeRight) {
        stickL.setMovementRange(movementRangeLeft);
        stickR.setMovementRange(movementRangeRight);
    }

    public void setMoveResolution(float leftMoveResolution, float rightMoveResolution) {
        stickL.setMoveResolution(leftMoveResolution);
        stickR.setMoveResolution(rightMoveResolution);
    }

    public void setUserCoordinateSystem(int leftCoordinateSystem, int rightCoordinateSystem) {
        stickL.setUserCoordinateSystem(leftCoordinateSystem);
        stickR.setUserCoordinateSystem(rightCoordinateSystem);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (D) {
            canvas.drawRect(1, 1, getMeasuredWidth() - 1, getMeasuredHeight() - 1, dbgPaint1);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean l = stickL.dispatchTouchEvent(ev);
        boolean r = stickR.dispatchTouchEvent(ev);
        return l || r;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean l = stickL.onTouchEvent(ev);
        boolean r = stickR.onTouchEvent(ev);
        return l || r;
    }
}
