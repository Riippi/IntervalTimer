package org.leeko.intervaltimer.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.leeko.intervaltimer.R;

/** Dialog for renaming a workout. */
public class RenameDialog extends DialogFragment {

    String name;
    EditText input;
    int workoutID;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks. */
    public interface NoticeDialogListener {
        void onTextDialogOk(String text, int id);
    }

    NoticeDialogListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            name = args.getString("name");
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
        mListener.onTextDialogOk(input.getText().toString(), workoutID);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_textedit, null);

        input = (EditText) view.findViewById(R.id.dialog_txt_name);
        input.setText(name);

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setTitle(R.string.rename_dialog_title)
                .setPositiveButton(R.string.set, (dialog, id) -> callBack())
                .setNegativeButton(R.string.cancel, null)
                .create();
    }
}
