package com.cardiomood.android.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.cardiomood.android.lite.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Anton Danshin on 30/12/14.
 */
public class InputTextDialog extends DialogFragment {

    private static final String ARG_MESSAGE = "message";
    private static final String ARG_TEXT = "text";
    private static final String ARG_TITLE = "title";

    @InjectView(R.id.textView1) TextView messageView;
    @InjectView(R.id.editTextDialogUserInput) EditText textView;

    private String message;
    private String text;
    private String title;

    private Callback callback;

    public static InputTextDialog newInstance(String title, String message, String text) {
        final InputTextDialog fragment = new InputTextDialog();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_TEXT, text);
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    public InputTextDialog() {
        // required no-arg constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Bundle args = getArguments();
            message = args.getString(ARG_MESSAGE);
            text = args.getString(ARG_TEXT);
            title = args.getString(ARG_TITLE);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // inflate view
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_input_text, null);

        // setup view
        ButterKnife.inject(this, dialogView);
        messageView.setText(message);
        textView.setText(text);
        textView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                text = s.toString();
            }
        });

        // create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);
        builder.setTitle(title);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (callback != null) {
                    callback.onOk(text);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (callback != null) {
                    callback.onCancel();
                }
            }
        });

        return builder.create();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public static interface Callback {
        void onCancel();
        void onOk(String text);
    }
}
