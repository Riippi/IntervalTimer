package org.leeko.intervaltimer;

import android.content.Context;
import android.graphics.Typeface;
import android.util.SparseArray;

import androidx.core.content.res.ResourcesCompat;

/**
 * The two bundled font families of the redesign: Inter for text, Space Grotesk for
 * headings and numbers (both SIL Open Font License, texts in assets/licenses/).
 * Loaded with ResourcesCompat so it also works below API 26 (minSdk is 21).
 */
final class Fonts {

    private static final SparseArray<Typeface> CACHE = new SparseArray<>();

    private Fonts() {
    }

    /** @param weight 400, 500, 600 or 700 */
    static Typeface inter(Context context, int weight) {
        switch (weight) {
            case 400:
                return load(context, R.font.inter_regular, Typeface.DEFAULT);
            case 500:
                return load(context, R.font.inter_medium, Typeface.DEFAULT);
            case 600:
                return load(context, R.font.inter_semibold, Typeface.DEFAULT_BOLD);
            default:
                return load(context, R.font.inter_bold, Typeface.DEFAULT_BOLD);
        }
    }

    /** @param weight 500, 600 or 700 */
    static Typeface spaceGrotesk(Context context, int weight) {
        switch (weight) {
            case 500:
                return load(context, R.font.space_grotesk_medium, Typeface.DEFAULT);
            case 600:
                return load(context, R.font.space_grotesk_semibold, Typeface.DEFAULT_BOLD);
            default:
                return load(context, R.font.space_grotesk_bold, Typeface.DEFAULT_BOLD);
        }
    }

    private static Typeface load(Context context, int fontResId, Typeface fallback) {
        Typeface cached = CACHE.get(fontResId);
        if (cached != null) {
            return cached;
        }
        Typeface loaded = null;
        try {
            loaded = ResourcesCompat.getFont(context.getApplicationContext(), fontResId);
        } catch (RuntimeException e) {
            // fall through to the system font rather than crash a workout over a font
        }
        if (loaded == null) {
            return fallback;
        }
        CACHE.put(fontResId, loaded);
        return loaded;
    }
}
