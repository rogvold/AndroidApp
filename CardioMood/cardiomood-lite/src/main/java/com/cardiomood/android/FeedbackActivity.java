package com.cardiomood.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
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

import com.cardiomood.android.lite.R;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class FeedbackActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
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
    public static class FeedbackFragment extends Fragment {

        private ProgressDialog progressDialog = null;

        @InjectView(R.id.editText) EditText userMessage;
        @InjectView(R.id.spinner) Spinner feedbackType;
        @InjectView(R.id.ratingBar) RatingBar userRating;
        @InjectView(R.id.button_submit) Button buttonSubmit;
        @InjectView(R.id.button_open_google_play) Button buttonOpenGooglePlay;

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

            // inject views
            ButterKnife.inject(this, rootView);

            return rootView;
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

        @OnClick(R.id.button_open_google_play)
        protected void openGooglePlay() {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=com.cardiomood.android.lite"));
                startActivity(intent);
            } catch (Exception ex) {
                Log.w("FeedbackActivity", "failed to start google play", ex);
            }
        }

        @OnClick(R.id.button_submit)
        protected void submitFeedback() {
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
            feedback.put("app_level", "lite");
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
            //feedback.put("sessions_number", getHelper().getRuntimeExceptionDao(ContinuousSessionEntity.class).countOf());
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
