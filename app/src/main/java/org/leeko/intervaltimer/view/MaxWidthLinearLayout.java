package org.leeko.intervaltimer.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import org.leeko.intervaltimer.R;

/**
 * A LinearLayout that is never wider than maxContentWidth. Give it layout_gravity
 * center_horizontal and it sits in the middle of a wide screen instead of stretching.
 */
public class MaxWidthLinearLayout extends LinearLayout {

    private int maxWidth = Integer.MAX_VALUE;

    public MaxWidthLinearLayout(Context context) {
        this(context, null);
    }

    public MaxWidthLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaxWidthLinearLayout);
        try {
            maxWidth = a.getDimensionPixelSize(R.styleable.MaxWidthLinearLayout_maxContentWidth, Integer.MAX_VALUE);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        if (mode != MeasureSpec.UNSPECIFIED && width > maxWidth) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth,
                    mode == MeasureSpec.EXACTLY ? MeasureSpec.EXACTLY : MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
