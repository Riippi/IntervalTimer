package org.leeko.intervaltimer.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import org.leeko.intervaltimer.R;

public class RoundsDialog  extends DialogFragment {

    NumberPicker numberPicker;
    int value, workoutID;


    public void setArguments(Bundle bundle) {
        super.setArguments(bundle);
        this.value = getArguments().getInt("rounds");
        this.workoutID = getArguments().getInt("id");
    }


    /* The activity that creates an instance of this dialog fragment must
	 * implement this interface in order to receive event callbacks.
	 * Each method passes the DialogFragment in case the host needs to query it. */
	public interface NoticeDialogListener {
		public void onRoundsDialogOk(int roundsAmount, int workoutID);
	}

	// Use this instance of the interface to deliver action events
	NoticeDialogListener mListener;

	// Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Verify that the host activity implements the callback interface
		try {
			// Instantiate the NoticeDialogListener so we can send events to the host
			mListener = (NoticeDialogListener) activity;
		} catch (ClassCastException e) {
			// The activity doesn't implement the interface, throw exception
			throw new ClassCastException(activity.toString()
					+ " must implement NoticeDialogListener");
		}
	}


	protected void callBack() {
		mListener.onRoundsDialogOk(numberPicker.getValue(), workoutID);
	}



	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {


		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();

		View view = inflater.inflate(R.layout.dialog_rounds, null);


		// Inflate and set the layout for the dialog
		// Pass null as the parent view because its going in the dialog layout
		builder.setView(view)

		.setTitle("Rounds")

		// Add action buttons
		.setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				callBack();
			}
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				//                       LoginDialogFragment.this.getDialog().cancel();
			}
		});      


		// Create the array of numbers that will populate the numberpicker
		final String[] nums = new String[100];
		for(int i=0; i<nums.length; i++) {
			nums[i] = Integer.toString(i);

		}
		//        
		numberPicker = (NumberPicker) view.findViewById(R.id.RoundsPicker);        
		numberPicker.setMaxValue(99);
		numberPicker.setMinValue(0);
		numberPicker.setValue(value);
		numberPicker.setWrapSelectorWheel(false);
		numberPicker.setDisplayedValues(nums);
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

		return builder.create();

	}
	
}