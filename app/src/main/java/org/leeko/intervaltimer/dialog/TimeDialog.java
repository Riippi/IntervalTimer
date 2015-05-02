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

public class TimeDialog  extends DialogFragment {


	static final int DEFAULT = 0;
	public static final int WARM_UP_TIME = 1;
	public static final int WORK_TIME = 2;
	public static final int REST_TIME = 3;
	
	int workoutID;

	String title;
	int min;
	int sec;

	NumberPicker minutes;
	NumberPicker seconds;

    public void setArguments(Bundle bundle) {
        super.setArguments(bundle);

        this.min = getArguments().getInt("min");
        this.sec = getArguments().getInt("sec");
        this.workoutID = getArguments().getInt("id");

    }



	/* The activity that creates an instance of this dialog fragment must
	 * implement this interface in order to receive event callbacks.
	 * Each method passes the DialogFragment in case the host needs to query it. */
	public interface NoticeDialogListener {
		public void onDialogPositiveClick(int type, int number1, int number2, int id);
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
		mListener.onDialogPositiveClick(DEFAULT, minutes.getValue(), seconds.getValue(), workoutID);
	}



	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {


		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();

		View view = inflater.inflate(R.layout.dialog_timepick, null);


		// Inflate and set the layout for the dialog
		// Pass null as the parent view because its going in the dialog layout
		builder.setView(view)

		.setTitle(title)

		// Add action buttons
		.setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				// sign in the user ...
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

			if (nums[i].length() == 1) {
				nums[i] = "0"+nums[i];
			}

		}
		//        
		minutes = (NumberPicker) view.findViewById(R.id.MinutesPicker);        
		minutes.setMaxValue(99);
		minutes.setMinValue(0);
		minutes.setValue(min);
		minutes.setWrapSelectorWheel(false);
		minutes.setDisplayedValues(nums);
        minutes.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

		seconds = (NumberPicker) view.findViewById(R.id.SecondsPicker);        
		seconds.setMaxValue(59);
		seconds.setMinValue(0);
		seconds.setValue(sec);
		seconds.setWrapSelectorWheel(false);
		seconds.setDisplayedValues(nums);
        seconds.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

		return builder.create();


	}
	
	
	
	

	public static class WarmUpTimeDialog extends TimeDialog {

		public WarmUpTimeDialog() {
			super();
			title = "Warm-up time";
		}

		protected void callBack() {
			mListener.onDialogPositiveClick(WARM_UP_TIME, minutes.getValue(), seconds.getValue(), workoutID);
		}
	}

	
	public static class RestTimeDialog extends TimeDialog {

		public RestTimeDialog() {
            super();
            title = "Rest time";
		}

		protected void callBack() {
			mListener.onDialogPositiveClick(REST_TIME, minutes.getValue(), seconds.getValue(), workoutID);
		}
	}

	
	public static class WorkTimeDialog extends TimeDialog {

		public WorkTimeDialog() {
            super();
            title = "Work time";
		}

		protected void callBack() {
			mListener.onDialogPositiveClick(WORK_TIME, minutes.getValue(), seconds.getValue(), workoutID);
		}
	}
}