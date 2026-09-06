package org.leeko.intervaltimer;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Android 15 (API 35) made edge-to-edge display mandatory for apps targeting API 35+,
 * and API 36 removed the last opt-out flag entirely. That means the system no longer
 * automatically keeps app content clear of the status bar and navigation bar the way
 * it used to - content is drawn full-bleed behind them unless the app pads for it
 * itself. This restores the old, expected behavior.
 */
class EdgeToEdgeHelper {

    /** Pads the activity's content root so it isn't drawn under the status/navigation bars. */
    static void applySystemBarPadding(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (content == null) {
            return;
        }
        final int left = content.getPaddingLeft();
        final int top = content.getPaddingTop();
        final int right = content.getPaddingRight();
        final int bottom = content.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
            return insets;
        });
    }
}
