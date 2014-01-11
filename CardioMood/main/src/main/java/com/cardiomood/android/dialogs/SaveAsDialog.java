package com.cardiomood.android.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.cardiomood.android.R;
import com.cardiomood.android.db.dao.HeartRateDataItemDAO;
import com.cardiomood.android.db.dao.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by danon on 11.01.14.
 */
public class SaveAsDialog extends Dialog {

    public static final String TAG = SaveAsDialog.class.getSimpleName();

    private long sessionId;
    private Context mContext;
    private ListView mListView;
    private WebView mWebView;

    private boolean savingInProgress = false;
    private SavingCallback savingCallback;

    public SaveAsDialog(Context context, long sessionId, WebView webView) {
        super(context, android.R.style.Theme_Holo_Light_Dialog);
        mContext = context;
        mWebView = webView;
        this.sessionId = sessionId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dialog_save_as);
        mListView = (ListView) findViewById(R.id.save_options_list);
        mListView.setAdapter(
                new ArrayAdapter<String>(
                    mContext,
                    android.R.layout.simple_list_item_1,
                    android.R.id.text1,
                    Arrays.asList(mContext.getText(R.string.item_save_as_image).toString(), mContext.getText(R.string.item_save_as_text).toString())
            )
        );
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0: saveAsImage();
                        break;
                    case 1: saveAsTxt();
                        break;
                }
                dismiss();
            }
        });
    }

    public boolean isSavingInProgress() {
        return savingInProgress;
    }

    public void setSavingInProgress(boolean savingInProgress) {
        this.savingInProgress = savingInProgress;
    }

    public SavingCallback getSavingCallback() {
        return savingCallback;
    }

    public void setSavingCallback(SavingCallback savingCallback) {
        this.savingCallback = savingCallback;
    }

    private void saveAsImage() {
        if (!savingInProgress) {
            setSavingInProgress(true);
            new SaveAsImageTask().execute(sessionId, mWebView.capturePicture());
        }
    }

    private void saveAsTxt() {
        if (!savingInProgress) {
            setSavingInProgress(true);
            new SaveAsTextTask().execute(sessionId);
        }
    }

    public static interface SavingCallback {
        void onBeginSave();
        void onEndSave();
        void onError();
    }

    private class SaveAsTextTask extends AsyncTask<Long, Void, String> {

        private File getTextStorageDirectory() {
            String status = Environment.getExternalStorageState();
            if (!Environment.MEDIA_MOUNTED.equals(status)) {
                return SaveAsDialog.this.mContext.getFilesDir();
            } else {
                File dir = null;
                dir = new File(Environment.getExternalStorageDirectory(), "CardioMood");
                dir = new File(dir, "txt");
                dir.mkdirs();
                return dir;
            }
        }

        private String generateFileName() {
            Date date = new Date() ;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss") ;
            return  dateFormat.format(date)+"_s" + String.format("%03d", sessionId) + ".txt";
        }

        @Override
        protected void onPreExecute() {
            if (savingCallback != null)
                savingCallback.onBeginSave();
        }

        @Override
        protected String doInBackground(Long... params) {
            long sessionId = params[0];
            PrintWriter pw = null;
            try {
                File outputFile = new File(getTextStorageDirectory(), generateFileName());
                pw = new PrintWriter(new FileWriter(outputFile));

                HeartRateSessionDAO sessionDAO = new HeartRateSessionDAO();
                HeartRateSession session = sessionDAO.findById(sessionId);
                if (session == null) {
                    throw new IllegalArgumentException("Session doesn't exist: sessionId = " + sessionId);
                }
                String name = session.getName();
                if (name == null || name.trim().isEmpty()) {
                    name = mContext.getText(R.string.dafault_measurement_name) + " #" + sessionId;
                }
                pw.println(name);
                if (session.getDescription() != null && !session.getDescription().isEmpty())
                    pw.println(session.getDescription());
                pw.println("Date: " + session.getDateStarted());
                pw.println();
                pw.printf("%4s  %-14s  %-4s %-3s%n", "n", "timestamp", "rr", "bpm");
                List<HeartRateDataItem> items = new HeartRateDataItemDAO().getItemsBySessionId(sessionId);
                int i = 1;
                for (HeartRateDataItem item: items) {
                    pw.printf("%4d  %14d  %4d %3d%n", i++, item.getTimeStamp().getTime(), (int)item.getRrTime(), item.getHeartBeatsPerMinute());
                }
                pw.println();
                pw.flush();
                return outputFile.getAbsolutePath();
            } catch (Exception ex) {
                Log.e(TAG, "SaveAsTextTask.doInBackground(): exception", ex);
            } finally {
                if (pw != null)
                    pw.close();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            setSavingInProgress(false);
            if (savingCallback != null)
                savingCallback.onEndSave();
            if (result != null)
                Toast.makeText(SaveAsDialog.this.mContext, result, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(SaveAsDialog.this.mContext, R.string.failed_to_save_file, Toast.LENGTH_SHORT).show();
        }
    }

    private class SaveAsImageTask extends AsyncTask<Object, Void, String> {

        @Override
        protected void onPreExecute() {
            if (savingCallback != null)
                savingCallback.onBeginSave();
        }

        private String generateFileName() {
            Date date = new Date() ;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss") ;
            return  dateFormat.format(date)+"_s" + String.format("%03d", sessionId) + ".png";
        }

        private File getPictureStorageDirectory() {
            String status = Environment.getExternalStorageState();
            if (!Environment.MEDIA_MOUNTED.equals(status)) {
                return SaveAsDialog.this.mContext.getFilesDir();
            } else {
                File dir = null;
                dir = new File(Environment.getExternalStorageDirectory(), "CardioMood");
                dir = new File(dir, "png");
                dir.mkdirs();
                return dir;
            }
        }

        @Override
        protected String doInBackground(Object... params) {
            long sessionId = (Long) params[0];
            Picture picture = (Picture) params[1];
            Bitmap bitmap = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            picture.draw(canvas);
            FileOutputStream fos = null;
            try {
                File directory = getPictureStorageDirectory();
                Log.d(TAG, "SaveAsImageTask: directory = " + directory.getAbsolutePath());

                File outFile = new File(directory, generateFileName());

                fos = new FileOutputStream(outFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                return outFile.getAbsolutePath();
            } catch (Exception e) {
                Log.e(TAG, "SaveAsImageTask: failed to save!", e);
                Toast.makeText(SaveAsDialog.this.mContext, R.string.failed_to_save_file, Toast.LENGTH_SHORT).show();
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (Exception ex) {

                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            setSavingInProgress(false);
            if (savingCallback != null)
                savingCallback.onEndSave();
            if (result != null)
                Toast.makeText(SaveAsDialog.this.mContext, result, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(SaveAsDialog.this.mContext, R.string.failed_to_save_image, Toast.LENGTH_SHORT).show();
        }
    }



}
