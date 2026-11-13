package org.leeko.intervaltimer.dialog;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;

import java.lang.reflect.Field;

/**
 * Larger numbers for the roller (NumberPicker) dialogs; the default digits are small next to
 * everything else in the app. The size is in sp, so it follows the system text size.
 */
final class PickerText {

    private static final float TEXT_SIZE_SP = 26f;

    private PickerText() {
    }

    // The reflection below only runs on Android 5-9. Android 9 blocks this private field
    // for apps targeting a recent API level; that lands in the catch and the wheel keeps
    // its normal size, which is the fallback this method promises.
    @SuppressLint("SoonBlockedPrivateApi")
    static void enlarge(NumberPicker picker) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SP,
                picker.getResources().getDisplayMetrics());

        if (Build.VERSION.SDK_INT >= 29) {
            picker.setTextSize(px);
            return;
        }

        // Android 5-9: NumberPicker has no setter, so set the size on the number field
        // inside it and on the paint that draws the wheel. If this does not work on some
        // device the picker simply keeps its normal size.
        try {
            for (int i = 0; i < picker.getChildCount(); i++) {
                View child = picker.getChildAt(i);
                if (child instanceof EditText) {
                    ((EditText) child).setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
                }
            }
            Field field = NumberPicker.class.getDeclaredField("mSelectorWheelPaint");
            field.setAccessible(true);
            ((android.graphics.Paint) field.get(picker)).setTextSize(px);
            picker.requestLayout();
            picker.invalidate();
        } catch (Throwable ignored) {
            // keep the default size
        }
    }
}
