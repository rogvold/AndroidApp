package com.cardiomood.android;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.cardiomood.android.db.DatabaseHelper;
import com.cardiomood.android.db.entity.ContinuousSessionEntity;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

public class FeedbackActivity extends OrmLiteBaseActivity<DatabaseHelper> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new FeedbackFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_feedback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public class FeedbackFragment extends Fragment {

        private ProgressDialog progressDialog = null;
        private EditText userMessage;
        private Spinner feedbackType;
        private RatingBar userRating;
        private Button buttonSubmit;
        private Button buttonOpenGooglePlay;
        private PreferenceHelper prefHelper;

        public FeedbackFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
            prefHelper = new PreferenceHelper(getActivity(), true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_feedback, container, false);
            userRating = (RatingBar) rootView.findViewById(R.id.ratingBar);
            userMessage = (EditText) rootView.findViewById(R.id.editText);
            feedbackType = (Spinner) rootView.findViewById(R.id.spinner);
            buttonSubmit = (Button) rootView.findViewById(R.id.button_submit);
            buttonOpenGooglePlay = (Button) rootView.findViewById(R.id.button_open_google_play);

            buttonSubmit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    submitFeedback();
                }
            });

            buttonOpenGooglePlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openGooglePlay();
                }
            });

            return rootView;
        }

        private void openGooglePlay() {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=com.cardiomood.android"));
                startActivity(intent);
            } catch (Exception ex) {
                Log.w("FeedbackActivity", "failed to start google play", ex);
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();
            if (id == R.id.menu_action_send_feedback) {
                submitFeedback();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        private void submitFeedback() {
            if (userRating.getRating() <= 0) {
                Toast.makeText(getActivity(), R.string.invalid_rating_value, Toast.LENGTH_SHORT).show();
                return;
            }

            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage(getText(R.string.submitting_feedback_message));
            progressDialog.setCancelable(false);
            progressDialog.show();

            ParseObject feedback = new ParseObject("CardioMoodAppFeedback");
            feedback.put("os", "android");
            feedback.put("os_version", Build.VERSION.SDK_INT);
            feedback.put("locale", getResources().getConfiguration().locale.toString());
            String version = "";
            try {
                PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                version = pInfo.versionCode + "";
            } catch (Exception ex) {
                Log.w("FeedbackActivity", "submitFeedback(): failed to get app_version", ex);
            }

            feedback.put("app_version", version);
            feedback.put("user_rating", userRating.getRating());
            feedback.put("user_feedback_type", feedbackType.getSelectedItem().toString());
            feedback.put("user_message", userMessage.getText().toString());
            feedback.put("sessions_number", getHelper().getRuntimeExceptionDao(ContinuousSessionEntity.class).countOf());
            feedback.put("user_email", prefHelper.getString(ConfigurationConstants.USER_EMAIL_KEY));

            feedback.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (progressDialog != null) {
                        if (progressDialog.isShowing())
                            progressDialog.dismiss();
                        progressDialog = null;
                    }
                    if (e == null) {
                        Toast.makeText(getActivity(), R.string.feedback_has_been_submitted, Toast.LENGTH_SHORT).show();
                        getActivity().finish();
                    } else {
                        Toast.makeText(getActivity(), R.string.feedback_submission_failed, Toast.LENGTH_SHORT).show();
                        Log.e("FeedbackActivity", "submitFeedback() -> saveInBackground() failed with exception.", e);
                    }
                }
            });
        }

    }

}
