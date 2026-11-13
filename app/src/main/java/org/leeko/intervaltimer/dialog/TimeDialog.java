package org.leeko.intervaltimer.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.leeko.intervaltimer.R;

/**
 * Roller dialog for a time (minutes and seconds). The subclasses only differ in the title
 * and in which setting the host activity is told it was for.
 */
public class TimeDialog extends DialogFragment {

    static final int DEFAULT = 0;
    public static final int WARM_UP_TIME = 1;
    public static final int WORK_TIME = 2;
    public static final int REST_TIME = 3;

    int workoutID;

    int titleResId;
    int min;
    int sec;

    NumberPicker minutes;
    NumberPicker seconds;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks. */
    public interface NoticeDialogListener {
        void onDialogPositiveClick(int type, int number1, int number2, int id);
    }

    NoticeDialogListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Read here (not in setArguments) so the values survive a rotation.
        Bundle args = getArguments();
        if (args != null) {
            min = args.getInt("min");
            sec = args.getInt("sec");
            workoutID = args.getInt("id");
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (NoticeDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement NoticeDialogListener");
        }
    }

    protected void callBack() {
        mListener.onDialogPositiveClick(DEFAULT, minutes.getValue(), seconds.getValue(), workoutID);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_timepick, null);

        // The numbers that populate the pickers, zero padded: 00, 01, ... 99
        final String[] nums = new String[100];
        for (int i = 0; i < nums.length; i++) {
            nums[i] = i < 10 ? "0" + i : Integer.toString(i);
        }

        minutes = (NumberPicker) view.findViewById(R.id.MinutesPicker);
        minutes.setMaxValue(99);
        minutes.setMinValue(0);
        minutes.setValue(min);
        minutes.setWrapSelectorWheel(false);
        minutes.setDisplayedValues(nums);
        minutes.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        PickerText.enlarge(minutes);

        seconds = (NumberPicker) view.findViewById(R.id.SecondsPicker);
        seconds.setMaxValue(59);
        seconds.setMinValue(0);
        seconds.setValue(sec);
        seconds.setWrapSelectorWheel(false);
        seconds.setDisplayedValues(nums);
        seconds.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        PickerText.enlarge(seconds);

        Dialog picker = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setTitle(titleResId)
                .setPositiveButton(R.string.set, (dialog, id) -> callBack())
                .setNegativeButton(R.string.cancel, null)
                .create();
        // Only Set, Cancel and Back close it. A swipe on a wheel that ends up next to the
        // dialog used to count as a tap outside and threw the change away.
        picker.setCanceledOnTouchOutside(false);
        return picker;
    }


    public static class WarmUpTimeDialog extends TimeDialog {
        public WarmUpTimeDialog() {
            super();
            titleResId = R.string.warmup_time_title;
        }

        @Override
        protected void callBack() {
            mListener.onDialogPositiveClick(WARM_UP_TIME, minutes.getValue(), seconds.getValue(), workoutID);
        }
    }

    public static class RestTimeDialog extends TimeDialog {
        public RestTimeDialog() {
            super();
            titleResId = R.string.rest_time_title;
        }

        @Override
        protected void callBack() {
            mListener.onDialogPositiveClick(REST_TIME, minutes.getValue(), seconds.getValue(), workoutID);
        }
    }

    public static class WorkTimeDialog extends TimeDialog {
        public WorkTimeDialog() {
            super();
            titleResId = R.string.work_time_title;
        }

        @Override
        protected void callBack() {
            mListener.onDialogPositiveClick(WORK_TIME, minutes.getValue(), seconds.getValue(), workoutID);
        }
    }
}
