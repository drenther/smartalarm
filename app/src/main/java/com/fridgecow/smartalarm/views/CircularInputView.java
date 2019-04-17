package com.fridgecow.smartalarm.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.fridgecow.smartalarm.R;

/**
 * TODO: document your custom view class.
 */
public class CircularInputView extends ViewGroup {
    private static final String TAG = CircularInputView.class.getSimpleName();
    private int mTextColor = ContextCompat.getColor(getContext(), R.color.card_text_color);
    private int mBackgroundColor = ContextCompat.getColor(getContext(), R.color.card_default_background);
    private int mThickness = 10;
    private int mMin = 0;
    private int mMax = 60;
    private double mAngle = 0;

    private TextPaint mTextPaint;
    private TextPaint mHighlightPaint;
    private Paint mBackgroundPaint;
    private Paint mViewPaint;

    private float mTextHeight;
    private float mHighlightHeight;

    private int mNumbers = 12;
    private int mNumber = mMin;

    // Touch related
    private boolean mScrolling = false;
    private float mLastX;
    private float mLastY;

    // Layout related
    private int mPaddingLeft;
    private int mPaddingTop;
    private int mPaddingRight;
    private int mPaddingBottom;
    private int mContentWidth;
    private int mContentHeight;
    private int mOuterRadius;
    private int mInnerRadius;
    private int mMidRadius;
    private int mCenterX;
    private int mCenterY;

    public static abstract class onChangeListener{
        public abstract void onChange(int newNumber);
    }
    private onChangeListener mListener;

    public CircularInputView(Context context) {
        super(context);
        init(null, 0);
    }

    public CircularInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CircularInputView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Measure self
        mPaddingLeft = getPaddingLeft();
        mPaddingTop = getPaddingTop();
        mPaddingRight = getPaddingRight();
        mPaddingBottom = getPaddingBottom();

        mContentWidth = getWidth() - mPaddingLeft - mPaddingRight;
        mContentHeight = getHeight() - mPaddingTop - mPaddingBottom;

        mOuterRadius = Math.min(mContentWidth, mContentHeight) / 2;
        mInnerRadius = mOuterRadius - mThickness;
        mMidRadius = mOuterRadius - mThickness / 2;

        mCenterX = getWidth() / 2;
        mCenterY = getHeight() / 2;

        // Lay views out linearly downwards, centering them vertically and horizontally
        int totalHeight = 0;
        final int count = getChildCount();

        final int xSpec = MeasureSpec.makeMeasureSpec(mContentWidth, MeasureSpec.AT_MOST);
        final int ySpec = MeasureSpec.makeMeasureSpec(mContentHeight, MeasureSpec.AT_MOST);

        // Get total height
        for(int i = 0; i < count; i++) {
            View child = getChildAt(i);

            if (child.getVisibility() == GONE){
                continue;
            }

            child.measure(xSpec, ySpec);
            totalHeight += child.getMeasuredHeight();
        }

        int curTop = mCenterY - totalHeight/2;
        for(int i = 0; i < count; i++){
            View child = getChildAt(i);
            final int width = child.getMeasuredWidth();
            final int height = child.getMeasuredHeight();

            if (child.getVisibility() == GONE){
                continue;
            }

            child.layout(mCenterX - width/2, curTop, mCenterX+width/2, curTop + height);
            curTop += height;
        }

    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.CircularInputView, defStyle, 0);

        // Custom drawing
        setWillNotDraw(false);

        mTextColor = a.getColor(
                R.styleable.CircularInputView_textColor, mTextColor);
        mBackgroundColor = a.getColor(
                R.styleable.CircularInputView_backgroundColor,
                mBackgroundColor);

        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mThickness = a.getDimensionPixelSize(
                R.styleable.CircularInputView_thickness,
                mThickness);

        mMin = a.getInt(R.styleable.CircularInputView_min, mMin);
        mMax = a.getInt(R.styleable.CircularInputView_max, mMax);
        mNumber = a.getInt(R.styleable.CircularInputView_value, mNumber);

        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        mHighlightPaint = new TextPaint();
        mHighlightPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mHighlightPaint.setTextAlign(Paint.Align.LEFT);

        mBackgroundPaint = new Paint();

        mViewPaint = new Paint();
        mViewPaint.setColor(getResources().getColor(android.R.color.black));
        // mViewPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        // Update paint and text measurements from attributes
        invalidatePaintAndMeasurements();
    }

    private void invalidatePaintAndMeasurements() {
        // TextPaint
        setTextSizeForWidth(mTextPaint, mThickness/2, Integer.toString(mMax));
        mTextPaint.setColor(mTextColor);

        mHighlightPaint.setTextSize(mTextPaint.getTextSize() + 10);
        mHighlightPaint.setColor(mTextColor);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;

        Paint.FontMetrics fontMetrics1 = mHighlightPaint.getFontMetrics();
        mHighlightHeight = fontMetrics1.bottom;

        // Background Paint
        mBackgroundPaint.setColor(mBackgroundColor);
    }

    @Override
    protected void onFinishInflate(){
        super.onFinishInflate();
    }

    /* stackoverflow.com/questions/12166476/android-canvas-drawtext-set-font-size-from-width */
    private static void setTextSizeForWidth(Paint paint, float desiredWidth, String text) {
        final float testTextSize = 48f;

        // Get the bounds of the text, using our testTextSize.
        paint.setTextSize(testTextSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        // Calculate the desired size as a proportion of our testTextSize.
        float desiredTextSize = testTextSize * desiredWidth / bounds.width();

        // Set the paint for that size.
        paint.setTextSize(desiredTextSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: cache as many computations as possible

        canvas.drawRect(0, 0, mContentWidth, mContentHeight, mViewPaint);

        canvas.drawCircle(mCenterX, mCenterY, mOuterRadius, mBackgroundPaint);
        canvas.drawCircle(mCenterX, mCenterY, mInnerRadius, mViewPaint);

        final double angleDelta = (Math.PI * 2) / mNumbers;
        final int numberDelta = (mMax - mMin + 1) / mNumbers;
        final double highlightWidth = Math.sin(angleDelta/(2*numberDelta))*mMidRadius;
        for(int i = 0; i < mNumbers; i++){
            final double angle = i*angleDelta + mAngle - Math.PI/2;
            final String number = Integer.toString(i*numberDelta + mMin);

            final int x = (int) (mCenterX + Math.cos(angle)*mMidRadius);
            final int y = (int) (mCenterY + Math.sin(angle)*mMidRadius);

            if(y < mCenterY && Math.abs(x - mCenterX) < highlightWidth){
                final float textWidth = mHighlightPaint.measureText(number);
                canvas.drawText(number, x - textWidth / 2, y + mHighlightHeight / 2, mHighlightPaint);
            }else {
                final float textWidth = mTextPaint.measureText(number);
                canvas.drawText(number, x - textWidth / 2, y + mTextHeight / 2, mTextPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();
        final int action = event.getAction();

        if(action == MotionEvent.ACTION_DOWN){
            // Log.d(TAG, "Touch Down");
            // Check where touch occurred
            final double dist = Math.pow(x - mCenterX, 2) + Math.pow(y - mCenterY, 2);
            if(dist > Math.pow(mInnerRadius, 2)){
                // Log.d(TAG, "Within Boundary");
                mScrolling = true;
                mLastX = x;
                mLastY = y;
                return true;
            }else {
                mScrolling = false;
            }
        }else if(action == MotionEvent.ACTION_UP){
            // Log.d(TAG, "Touch Up");
            mScrolling = false;
            return true;
        }else if(action == MotionEvent.ACTION_MOVE && mScrolling){
            // Find angular difference between positions
            // Log.d(TAG, "X: "+x+" Y: "+y);

            final double curAngle = Math.atan2(mCenterY - y, mCenterX - x);
            final double lastAngle = Math.atan2(mCenterY - mLastY, mCenterX - mLastX);

            // Log.d(TAG, "Current: "+curAngle+" Last: "+lastAngle);

            final double dAngle = Math.atan2(Math.sin(curAngle-lastAngle), Math.cos(curAngle-lastAngle));

            // Log.d(TAG, "Delta: "+dAngle);

            setAngle(getAngle() + dAngle);

            mLastX = x;
            mLastY = y;

            return true;
        }

        return super.onTouchEvent(event);
    }

    public void setOnChangeListener(onChangeListener listener){
        mListener = listener;
        listener.onChange(mNumber);
    }

    public void clearOnChangeListener(){
        mListener = null;
    }

    public int getMin() {
        return mMin;
    }

    public void setMin(int min) {
        this.mMin = min;
        invalidate();
    }

    public int getMax(){
        return mMax;
    }

    public void setMax(int max){
        mMax = max;
        invalidatePaintAndMeasurements();
        invalidate();
    }

    public void setThickness(int thickness){
        mThickness = thickness;
        invalidatePaintAndMeasurements();
    }

    public int getThickness(){
        return mThickness;
    }

    public void setTextColor(int color){
        mTextColor = color;
        invalidatePaintAndMeasurements();
    }

    public int getTextColor(){
        return mTextColor;
    }

    public void setBackgroundColor(int color){
        mBackgroundColor = color;
        invalidatePaintAndMeasurements();
    }

    public int getBackgroundColor(){
        return mBackgroundColor;
    }

    public double getAngle() {
        return mAngle;
    }

    public void setAngle(double angle) {
        mAngle = angle % (Math.PI*2);
        if(mAngle < 0){
            mAngle += Math.PI*2;
        }
        // Log.d(TAG, "New angle: "+Math.toDegrees(angle));

        // Get number from angle around circle
        int number =getNumberFromAngle(mAngle);

        if(number != mNumber){
            mNumber = number;
            if(mListener != null) {
                mListener.onChange(mNumber);
            }
        }
        invalidate();
    }

    private int getNumberFromAngle(double angle){
        int number = mMin - (int) Math.round((mMax - mMin + 1)*angle/(2*Math.PI));

        // Put number in range
        number = Math.floorMod(number - mMin, mMax - mMin + 1) + mMin;
        return number;
    }

    public int getValue(){
        return mNumber;
    }

    public void setValue(int value){
        if(value >= mMin && value <= mMax){
            // mNumber = value;
            setAngle(((mMin - value)/((double) (mMax - mMin + 1)))*Math.PI*2);
        }

    }

    public boolean canScrollHorizontally(int direction) {
        return mScrolling;
    }
}
