package com.cardiomood.sport.android.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.cardiomood.sport.android.R;
import com.cardiomood.sport.android.client.json.JsonActivity;

/**
 * Project: CardioSport
 * User: danon
 * Date: 18.06.13
 * Time: 17:24
 */
public class ActivityDetailsDialog extends DialogFragment {

    public static final String ACTIVITY_ARGUMENT = "activity";

    private TextView title;
    private TextView description;
    private TextView duration;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.dialog_activity_info, null);
        builder.setView(view);

        populateView(view, (JsonActivity) getArguments().getSerializable(ACTIVITY_ARGUMENT));

        builder.setTitle(getString(R.string.activity_details));

        return builder.create();
    }

    private void populateView(View view, JsonActivity activity) {
        title = (TextView) view.findViewById(R.id.name);
        description = (TextView) view.findViewById(R.id.description);
        duration = (TextView) view.findViewById(R.id.duration);

        title.setText(activity.getName());
        description.setText(activity.getDescription());
        duration.setText(getDurationText(activity.getDuration()));
    }

    private String getDurationText(long duration) {
        long hours = (duration/1000) / 3600;
        long mins = (duration/1000 - hours * 3600) / 60;
        long seconds = duration/1000 - hours*3600 - mins*60;

        String text = "";
        if (hours > 0)
            text += hours + " h. ";
        if (mins > 0)
            text += mins + " min. ";
        if (seconds > 0)
            text += seconds + " s.";
        text = text.trim();

        return text.isEmpty() ? "0 seconds" : text;
    }


}
