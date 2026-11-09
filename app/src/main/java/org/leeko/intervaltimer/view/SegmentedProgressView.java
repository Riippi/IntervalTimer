package org.leeko.intervaltimer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import org.leeko.intervaltimer.PhasePlan;
import org.leeko.intervaltimer.R;

/**
 * The running timer's progress bar: one rounded segment per phase, as wide as the phase
 * is long. Finished parts are solid black, what is left is black at 18 %.
 */
public class SegmentedProgressView extends View {

    private static final float HEIGHT_DP = 10f;
    private static final float GAP_DP = 4f;
    private static final float RADIUS_DP = 5f;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF segment = new RectF();
    private final Path clip = new Path();
    private final float density;

    private PhasePlan plan;
    private int elapsedSeconds;

    public SegmentedProgressView(Context context) {
        this(context, null);
    }

    public SegmentedProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        trackPaint.setColor(ContextCompat.getColor(context, R.color.on_phase_track));
        fillPaint.setColor(ContextCompat.getColor(context, R.color.on_phase));
        setContentDescription(context.getString(R.string.cd_workout_progress));
    }

    public void setPlan(PhasePlan plan) {
        this.plan = plan;
        invalidate();
    }

    public void setElapsedSeconds(int seconds) {
        if (seconds == elapsedSeconds) {
            return;
        }
        elapsedSeconds = seconds;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = resolveSize((int) (HEIGHT_DP * density), heightMeasureSpec);
        setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec), height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (plan == null || plan.count() == 0 || plan.totalSeconds() <= 0) {
            return;
        }

        int count = plan.count();
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        float height = getHeight() - getPaddingTop() - getPaddingBottom();

        // With very many phases (99 rounds) the usual gap would eat the whole bar.
        float gap = Math.min(GAP_DP * density, count > 1 ? width * 0.25f / (count - 1) : 0f);
        float usable = width - gap * (count - 1);

        float left = getPaddingLeft();
        float top = getPaddingTop();
        for (int i = 0; i < count; i++) {
            float segmentWidth = usable * plan.seconds(i) / plan.totalSeconds();
            segment.set(left, top, left + segmentWidth, top + height);

            float radius = Math.min(RADIUS_DP * density, Math.min(segmentWidth, height) / 2f);
            canvas.drawRoundRect(segment, radius, radius, trackPaint);

            float fill = plan.fill(i, elapsedSeconds);
            if (fill > 0f) {
                canvas.save();
                clip.reset();
                clip.addRoundRect(segment, radius, radius, Path.Direction.CW);
                canvas.clipPath(clip);
                canvas.drawRect(segment.left, segment.top,
                        segment.left + segmentWidth * fill, segment.bottom, fillPaint);
                canvas.restore();
            }
            left += segmentWidth + gap;
        }
    }
}
