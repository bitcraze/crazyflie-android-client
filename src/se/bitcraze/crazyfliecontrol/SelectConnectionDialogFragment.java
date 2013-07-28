package se.bitcraze.crazyfliecontrol;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class SelectConnectionDialogFragment extends DialogFragment {

    public interface SelectCrazyflieDialogListener {
        public void onClick(int which);
    }

    SelectCrazyflieDialogListener mListener;
    
    public void setListener(SelectCrazyflieDialogListener listener){
        this.mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String[] crazyflieList = getArguments().getStringArray("connection_array");
        
        builder.setTitle(R.string.select_connection_dialog_title);
        builder.setItems(crazyflieList, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if(mListener != null){
                        mListener.onClick(which);
                    }
                }
            });

        return builder.create();
    }
}