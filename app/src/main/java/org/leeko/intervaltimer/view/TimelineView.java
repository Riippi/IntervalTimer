package org.leeko.intervaltimer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import org.leeko.intervaltimer.PhasePlan;
import org.leeko.intervaltimer.R;

/**
 * The bar on the main screen that shows what a workout is made of: one colored segment per
 * phase (yellow warm-up, green work, red rest), as wide as the phase is long. A manual rest
 * has no length, so it shows as a thin faded red marker between two work segments. With
 * unlimited rounds the bar shows one cycle followed by three dots.
 */
public class TimelineView extends View {

    private static final float HEIGHT_DP = 10f;
    private static final float GAP_DP = 2f;
    private static final float MIN_SEGMENT_DP = 3f;
    private static final float RADIUS_DP = 3f;
    private static final float MARKER_DP = 4f;
    private static final float MARKER_RADIUS_DP = 2f;
    private static final float DOT_DP = 4f;
    private static final int DOT_COUNT = 3;

    private final Paint warmupPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint workPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint restPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;

    private PhasePlan plan;

    public TimelineView(Context context) {
        this(context, null);
    }

    public TimelineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        warmupPaint.setColor(ContextCompat.getColor(context, R.color.phase_warmup));
        workPaint.setColor(ContextCompat.getColor(context, R.color.phase_work));
        restPaint.setColor(ContextCompat.getColor(context, R.color.phase_rest));
        markerPaint.setColor(ContextCompat.getColor(context, R.color.phase_rest));
        markerPaint.setAlpha(128);
        dotPaint.setColor(ContextCompat.getColor(context, R.color.on_surface_variant));
        setContentDescription(context.getString(R.string.cd_workout_timeline));
    }

    public void setPlan(PhasePlan plan) {
        this.plan = plan;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                resolveSize((int) (HEIGHT_DP * density), heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (plan == null || plan.count() == 0 || plan.totalSeconds() <= 0) {
            return;
        }

        int count = plan.count();
        float gap = GAP_DP * density;
        float marker = MARKER_DP * density;
        float dot = DOT_DP * density;
        float minSegment = MIN_SEGMENT_DP * density;
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        float height = getHeight() - getPaddingTop() - getPaddingBottom();

        int markers = 0;
        for (int i = 0; i < count; i++) {
            if (plan.hasManualRestAfter(i)) {
                markers++;
            }
        }

        // Everything that is not a phase segment has a fixed width: gaps, markers, end dots.
        int items = count + markers;
        float fixed = gap * (items - 1) + markers * marker;
        if (plan.isUnlimited()) {
            fixed += gap + DOT_COUNT * dot + (DOT_COUNT - 1) * dot;
        }
        float[] widths = segmentWidths(width - fixed, minSegment);

        float left = getPaddingLeft();
        float top = getPaddingTop();
        for (int i = 0; i < count; i++) {
            rect.set(left, top, left + widths[i], top + height);
            float radius = Math.min(RADIUS_DP * density, Math.min(widths[i], height) / 2f);
            canvas.drawRoundRect(rect, radius, radius, paintFor(plan.kind(i)));
            left += widths[i] + gap;

            if (plan.hasManualRestAfter(i)) {
                rect.set(left, top, left + marker, top + height);
                canvas.drawRoundRect(rect, MARKER_RADIUS_DP * density, MARKER_RADIUS_DP * density, markerPaint);
                left += marker + gap;
            }
        }

        if (plan.isUnlimited()) {
            float cy = top + height / 2f;
            for (int d = 0; d < DOT_COUNT; d++) {
                canvas.drawCircle(left + dot / 2f + d * dot * 2f, cy, dot / 2f, dotPaint);
            }
        }
    }

    /** Widths proportional to each phase's seconds, but never thinner than the minimum. */
    private float[] segmentWidths(float available, float minWidth) {
        int count = plan.count();
        float[] widths = new float[count];
        boolean[] pinned = new boolean[count];
        float remaining = Math.max(available, minWidth * count);
        float seconds = plan.totalSeconds();

        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < count; i++) {
                if (!pinned[i] && seconds > 0 && remaining * plan.seconds(i) / seconds < minWidth) {
                    pinned[i] = true;
                    widths[i] = minWidth;
                    remaining -= minWidth;
                    seconds -= plan.seconds(i);
                    changed = true;
                }
            }
        }
        for (int i = 0; i < count; i++) {
            if (!pinned[i]) {
                widths[i] = seconds > 0 ? remaining * plan.seconds(i) / seconds : minWidth;
            }
        }
        return widths;
    }

    private Paint paintFor(int kind) {
        switch (kind) {
            case PhasePlan.WARMUP:
                return warmupPaint;
            case PhasePlan.REST:
                return restPaint;
            default:
                return workPaint;
        }
    }
}
