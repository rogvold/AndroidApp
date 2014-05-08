package com.cardiomood.android.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

import com.cardiomood.android.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by danon on 10.01.14.
 */
public class WhatsNewDialog extends Dialog {

    private static final String TAG = WhatsNewDialog.class.getSimpleName();

    public static final String CONFIG_SHOW_DIALOG_ON_STARTUP = "app.whats_new.show_on_startup_v133";

    private Context mContext;

    public WhatsNewDialog(Context context) {
        super(context, android.R.style.Theme_Holo_Light_Dialog);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_whats_new);

        TextView tv = (TextView) findViewById(R.id.whats_new_text);
        tv.setText(Html.fromHtml(readRawTextFile(R.raw.whats_new)));
        tv.setLinkTextColor(Color.RED);
        tv.setLinksClickable(true);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
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
