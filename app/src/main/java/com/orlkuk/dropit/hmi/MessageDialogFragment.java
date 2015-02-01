package com.orlkuk.dropit.hmi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;


import com.google.android.gms.maps.model.LatLng;
import com.orlkuk.dropit.R;

/**
 * Created by guillaume on 18/12/14.
 */
public class MessageDialogFragment extends DialogFragment{

    public interface MessageDialogListener {
        void onSendMessageClicked(String inputText, LatLng latLng);
    }

    private EditText mEditText;
    private LatLng mlatLng;

    public void setPosition(LatLng mlatLng) {
        this.mlatLng = mlatLng;
    }


    public MessageDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder b=  new  AlertDialog.Builder(getActivity())
                .setTitle("Message")
                .setPositiveButton("Send",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                // Return input text to activity
                                MessageDialogListener activity = (MessageDialogListener) getActivity();
                                activity.onSendMessageClicked(mEditText.getText().toString(), mlatLng);
                                dismiss();
                            }
                        }
                )
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }
                );
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_message_dialog, null);
        mEditText = (EditText) view.findViewById(R.id.messageEditText);
        b.setView(view);

        return b.create();
    }
}
