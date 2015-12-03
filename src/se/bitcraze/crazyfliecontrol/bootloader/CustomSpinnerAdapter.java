package se.bitcraze.crazyfliecontrol.bootloader;

import java.util.List;

import se.bitcraze.crazyfliecontrol2.R;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CustomSpinnerAdapter extends ArrayAdapter<Firmware> {

    private Activity mActivity;
    private List<Firmware> mFirmwares;
    private LayoutInflater inflater;

    public CustomSpinnerAdapter(Activity activity, int textViewResourceId, List<Firmware> firmwares) {
        super(activity, textViewResourceId, firmwares);
        mActivity = activity;
        mFirmwares = firmwares;
        inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    // this function is called for each row (Called data.size() times)
    public View getCustomView(int position, View convertView, ViewGroup parent) {
        View row = inflater.inflate(R.layout.spinner_rows, parent, false);
        Firmware firmware = mFirmwares.get(position);
        if (mFirmwares.isEmpty() || firmware == null) {
            return row;
        }
        TextView label = (TextView) row.findViewById(R.id.label);
        TextView date = (TextView) row.findViewById(R.id.date);
        TextView type = (TextView) row.findViewById(R.id.type);
        // Set values for each spinner row
        label.setText(firmware.getTagName());
        date.setText(firmware.getCreatedAt());
        type.setText(firmware.getType());
        return row;
    }
}