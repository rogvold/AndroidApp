package com.cardiomood.android.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.widget.TextView;

import com.cardiomood.android.lite.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by danshin on 06.11.13.
 */
public class AboutDialog extends Dialog {

    private Context mContext;

    public AboutDialog(Context context) {
        super(context, android.R.style.Theme_Holo_Dialog);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_about);
        TextView tv = (TextView) findViewById(R.id.legal_text);
        tv.setText(readRawTextFile(R.raw.legal));
        tv = (TextView) findViewById(R.id.info_text);
        tv.setText(Html.fromHtml(readRawTextFile(R.raw.info)));
        tv.setLinkTextColor(Color.WHITE);
        Linkify.addLinks(tv, Linkify.ALL);
    }

    public String readRawTextFile(int id) {
        InputStream inputStream = mContext.getResources().openRawResource(id);

        InputStreamReader in = new InputStreamReader(inputStream);
        BufferedReader buf = new BufferedReader(in);
        String line;

        StringBuilder text = new StringBuilder();
        try {
            while ((line = buf.readLine()) != null) text.append(line);
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }
}
