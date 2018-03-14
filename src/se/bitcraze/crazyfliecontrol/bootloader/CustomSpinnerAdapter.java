/**
 *    ||          ____  _ __
 * +------+      / __ )(_) /_______________ _____  ___
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 *  ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 *
 * Copyright (C) 2015 Bitcraze AB
 *
 * Crazyflie Nano Quadcopter Client
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package se.bitcraze.crazyfliecontrol.bootloader;

import java.util.List;

import se.bitcraze.crazyflie.lib.bootloader.FirmwareRelease;
import se.bitcraze.crazyfliecontrol2.R;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CustomSpinnerAdapter extends ArrayAdapter<FirmwareRelease> {

    private Activity mActivity;
    private List<FirmwareRelease> mFirmwareReleases;
    private LayoutInflater inflater;

    public CustomSpinnerAdapter(Activity activity, int textViewResourceId, List<FirmwareRelease> firmwareReleases) {
        super(activity, textViewResourceId, firmwareReleases);
        mActivity = activity;
        mFirmwareReleases = firmwareReleases;
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
    private View getCustomView(int position, View convertView, ViewGroup parent) {
        View row = inflater.inflate(R.layout.spinner_rows, parent, false);
        FirmwareRelease firmwareRelease = mFirmwareReleases.get(position);
        if (mFirmwareReleases.isEmpty() || firmwareRelease == null) {
            return row;
        }
        TextView label = (TextView) row.findViewById(R.id.label);
        TextView date = (TextView) row.findViewById(R.id.date);
        TextView type = (TextView) row.findViewById(R.id.type);
        // Set values for each spinner row
        label.setText(firmwareRelease.getTagName());
        date.setText(firmwareRelease.getCreatedAt());
        type.setText(firmwareRelease.getType());
        return row;
    }
}