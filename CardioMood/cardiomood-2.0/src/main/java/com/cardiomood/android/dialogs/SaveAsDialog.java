package com.cardiomood.android.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.cardiomood.android.R;
import com.cardiomood.android.db.DatabaseHelper;
import com.cardiomood.android.db.DatabaseHelperFactory;
import com.cardiomood.android.db.entity.CardioItemDAO;
import com.cardiomood.android.db.entity.SessionDAO;
import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.android.fragments.details.TextReport;
import com.flurry.android.FlurryAgent;

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
    private int filterCount = 0;
    private DatabaseHelper databaseHelper;

    private boolean savingInProgress = false;
    private SavingCallback savingCallback;

    public SaveAsDialog(Context context, long sessionId, int filterCount) {
        super(context);
        mContext = context;
        this.databaseHelper = DatabaseHelperFactory.getHelper();
        this.sessionId = sessionId;
        this.filterCount = filterCount;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dialog_save_as);
        mListView = (ListView) findViewById(R.id.save_options_list);
        mListView.setAdapter(
                new ArrayAdapter<>(
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
                    case 0:
                        FlurryAgent.logEvent("save_as_image_clicked");
                        saveAsImage();
                        break;
                    case 1:
                        FlurryAgent.logEvent("save_as_text_clicked");
                        saveAsTxt();
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
            Toast.makeText(mContext, "This feature is currently unavailable. Hold POWER and VOLUME DOWN buttons to capture a screenshot.", Toast.LENGTH_SHORT).show();
//            if (viewToSave != null) {
//                setSavingInProgress(true);
//                viewToSave.setDrawingCacheEnabled(true);
//                viewToSave.setDrawingCacheBackgroundColor(Color.WHITE);
//                new SaveAsImageTask().execute(sessionId, viewToSave.getDrawingCache());
//            }
        }
    }

    public void saveAsTxt() {
        if (!savingInProgress) {
            setSavingInProgress(true);
            new SaveAsTextTask().execute(sessionId);
        }
    }

    public static interface SavingCallback {
        void onBeginSave();
        void onEndSave(String fileName);
        void onError();
    }

    private class SaveAsTextTask extends AsyncTask<Long, Void, String> {

        private File getTextStorageDirectory() {
            String status = Environment.getExternalStorageState();
            if (!Environment.MEDIA_MOUNTED.equals(status)) {
                return SaveAsDialog.this.mContext.getFilesDir();
            } else {
                File dir = new File(Environment.getExternalStorageDirectory(), "CardioMood");
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

                SessionDAO sessionDAO = databaseHelper.getSessionDao();
                SessionEntity session = sessionDAO.queryForId(sessionId);
                if (session == null) {
                    throw new IllegalArgumentException("Session doesn't exist: sessionId = " + sessionId);
                }
                String name = session.getName();
                if (name == null || name.trim().isEmpty()) {
                    name = mContext.getText(R.string.dafault_measurement_name) + " #" + sessionId;
                }
                TextReport.Builder reportBuilder = new TextReport.Builder();
                reportBuilder.setStartDate(new Date(session.getStartTimestamp()));
                if (session.getEndTimestamp() != null) {
                    reportBuilder.setEndDate(new Date(session.getEndTimestamp()));
                } else {
                    reportBuilder.setEndDate(new Date(session.getStartTimestamp()));
                }
                reportBuilder.setTag(name);
                CardioItemDAO hrDAO = databaseHelper.getCardioItemDao();
                final List<String[]> res = hrDAO.queryRaw(
                        "select rr, t, bpm from cardio_items where session_id = ? order by _id asc",
                        String.valueOf(sessionId)
                ).getResults();
                double rr[] = new double[res.size()];
                long time[] = new long[res.size()];
                int bpm[] = new int[res.size()];
                int i = 0;
                for (String[] item: res) {
                    rr[i] = Long.parseLong(item[0]);
                    time[i] = Long.parseLong(item[1]);
                    bpm[i] = Integer.parseInt(item[2]);
                    i++;
                }
                reportBuilder.setRRIntervals(rr);
                reportBuilder.setFilterCount(filterCount);
                TextReport report = reportBuilder.build();
                pw.println(report.toString());
                pw.println();
                pw.flush();
                pw.println("The numbers above were calculated using following data:");
                pw.printf("%4s  %-14s  %-4s %-3s%n", "n", "timestamp", "rr", "bpm");
                rr = report.getRrIntervals();
                for (i=0; i<rr.length; i++) {
                    pw.printf("%4d  %14d  %4d %3d%n", i+1, time[i], (int) rr[i], bpm[i]);
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

            if (result != null) {
                if (savingCallback != null)
                    savingCallback.onEndSave(result);
                Toast.makeText(SaveAsDialog.this.mContext, result, Toast.LENGTH_SHORT).show();
            } else {
                if (savingCallback != null)
                    savingCallback.onError();
                Toast.makeText(SaveAsDialog.this.mContext, R.string.failed_to_save_file, Toast.LENGTH_SHORT).show();
            }
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
            Bitmap bitmap = (Bitmap) params[1];
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
                savingCallback.onEndSave(result);
            if (result != null)
                Toast.makeText(SaveAsDialog.this.mContext, result, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(SaveAsDialog.this.mContext, R.string.failed_to_save_image, Toast.LENGTH_SHORT).show();
        }
    }



}
