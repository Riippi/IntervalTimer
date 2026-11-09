package org.leeko.intervaltimer.view;

import android.content.Context;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * A single-line TextView that picks the largest text size that fits its own box, so the
 * timer digits are as big as the screen allows on any phone or tablet.
 * <p>
 * The size is worked out from a template where every digit is a 0. With tabular figures
 * (all digits the same width) the size therefore stays still while the digits tick, and
 * only changes when the text gets longer or shorter (for example 10:00 to 9:59).
 * Give it a fixed size in the layout (match_parent or a weight), not wrap_content.
 */
public class FitTextView extends TextView {

    private static final float MIN_SIZE_SP = 12f;

    private float maxSizePx;
    private boolean fitHeight = true;
    private float heightFactor = 1f;

    public FitTextView(Context context) {
        this(context, null);
    }

    public FitTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public FitTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        maxSizePx = getTextSize();
        setSingleLine(true);
        setIncludeFontPadding(false);
    }

    /** The largest size to ever use, in sp. Defaults to the textSize set in the layout. */
    public void setMaxTextSizeSp(float sp) {
        maxSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                getResources().getDisplayMetrics());
        refit();
    }

    /**
     * How much of the font's full line height has to fit inside the view. Digits have no
     * descenders, so a value below 1 (for example 0.85) lets them grow a little bigger.
     */
    public void setHeightFactor(float factor) {
        this.heightFactor = factor;
        refit();
    }

    /** Also keep the text inside the view's height (on by default). */
    public void setFitHeight(boolean fitHeight) {
        this.fitHeight = fitHeight;
        refit();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        refit();
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        refit();
    }

    private void refit() {
        int availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int availableHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        CharSequence text = getText();
        if (availableWidth <= 0 || text == null || text.length() == 0) {
            return;
        }
        if (fitHeight && availableHeight <= 0) {
            return;
        }

        String template = text.toString().replaceAll("[0-9]", "0");
        TextPaint paint = new TextPaint(getPaint());
        float minPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, MIN_SIZE_SP,
                getResources().getDisplayMetrics());

        float low = minPx;
        float high = Math.max(minPx, maxSizePx);
        for (int i = 0; i < 12; i++) {
            float mid = (low + high) / 2f;
            paint.setTextSize(mid);
            boolean fits = paint.measureText(template) <= availableWidth;
            if (fits && fitHeight) {
                TextPaint.FontMetrics fm = paint.getFontMetrics();
                fits = (fm.descent - fm.ascent) * heightFactor <= availableHeight;
            }
            if (fits) {
                low = mid;
            } else {
                high = mid;
            }
        }

        if (Math.abs(getTextSize() - low) > 0.5f) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, low);
        }
    }
}
