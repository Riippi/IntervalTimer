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

/** Roller dialog for the number of rounds. 0 means unlimited. */
public class RoundsDialog extends DialogFragment {

    NumberPicker numberPicker;
    int value, workoutID;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks. */
    public interface NoticeDialogListener {
        void onRoundsDialogOk(int roundsAmount, int workoutID);
    }

    NoticeDialogListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            value = args.getInt("rounds");
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
        mListener.onRoundsDialogOk(numberPicker.getValue(), workoutID);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_rounds, null);

        // The numbers that populate the picker. 0 rounds means "unlimited" (see
        // CounterFactory), so show that as text instead of a confusing "0".
        final String[] nums = new String[100];
        for (int i = 0; i < nums.length; i++) {
            nums[i] = Integer.toString(i);
        }
        nums[0] = getString(R.string.rounds_unlimited);

        numberPicker = (NumberPicker) view.findViewById(R.id.RoundsPicker);
        numberPicker.setMaxValue(99);
        numberPicker.setMinValue(0);
        numberPicker.setValue(value);
        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setDisplayedValues(nums);
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setTitle(R.string.rounds_title)
                .setPositiveButton(R.string.set, (dialog, id) -> callBack())
                .setNegativeButton(R.string.cancel, null)
                .create();
    }
}
