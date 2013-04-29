package com.cardiomood.android;

import java.util.ArrayList;
import java.util.List;

import com.cardiomood.android.config.ConfigurationConstants;
import com.cardiomood.android.config.ConfigurationManager;
import com.cardiomood.android.db.HeartRateDataItemDAO;
import com.cardiomood.android.db.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.services.CardioMoodService;
import com.cardiomood.android.services.CardioMoodSimpleRatesData;
import com.cardiomood.android.services.CardioMoodSimpleResponse;
import com.cardiomood.android.services.ICardioMoodService;
import com.google.gson.Gson;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * An activity representing a single Session detail screen. This activity is
 * only used on handset devices. On tablet-size devices, item details are
 * presented side-by-side with a list of items in a {@link SessionListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link SessionDetailFragment}.
 */
public class SessionDetailActivity extends FragmentActivity {

	private long sessionId = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_session_detail);

		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// savedInstanceState is non-null when there is fragment state
		// saved from previous configurations of this activity
		// (e.g. when rotating the screen from portrait to landscape).
		// In this case, the fragment will automatically be re-added
		// to its container so we don't need to manually add it.
		// For more information, see the Fragments API guide at:
		//
		// http://developer.android.com/guide/components/fragments.html
		//
		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.
			Bundle arguments = new Bundle();
			sessionId = getIntent().getLongExtra(SessionDetailFragment.ARG_SESSION_ID, -1L);
			arguments.putLong(SessionDetailFragment.ARG_SESSION_ID, sessionId);
			Toast.makeText(this, "Loading Session #" + sessionId, Toast.LENGTH_SHORT).show();
			SessionDetailFragment fragment = new SessionDetailFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.session_detail_container, fragment).commit();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.session_details, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpTo(this, new Intent(this,
					SessionListActivity.class));
			return true;
		case R.id.menu_sync:
			attemptSyncSession(this, sessionId);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void attemptSyncSession(Context ctx, long sessionId) {
		if (sessionId == -1L) {
			return;
		}
		
		HeartRateSessionDAO sessionDAO = new HeartRateSessionDAO(ctx);
		sessionDAO.open();
		HeartRateDataItemDAO hrItemDAO = new HeartRateDataItemDAO(ctx);
		hrItemDAO.open();
		HeartRateSession session = sessionDAO.findById(sessionId);
		sessionDAO.close();
		if (session != null && session.getStatus() == HeartRateSession.COMPLETED_STATUS) {
			SyncSessionTask task = new SyncSessionTask(ctx);
			CardioMoodSimpleRatesData sessionObject = getSessionObject(ctx, session);
			task.execute(sessionId + "", new Gson().toJson(sessionObject));
		}	
	}
	
	private CardioMoodSimpleRatesData getSessionObject(Context ctx, HeartRateSession session) {
		HeartRateDataItemDAO dao = new HeartRateDataItemDAO(ctx);
		dao.open();
		List<HeartRateDataItem> items = dao.getAllItemsOfSession(session.getId());
		dao.close();
		List<Integer> intervals = new ArrayList<Integer>(items.size());
		for(HeartRateDataItem item: items) {
			intervals.add((int)item.getRrTime());
		}
		ConfigurationManager conf = ConfigurationManager.getInstance();
		CardioMoodSimpleRatesData rates = new CardioMoodSimpleRatesData();
		rates.setStart(session.getDateStarted().getTime());
		rates.setEmail(conf.getString(ConfigurationConstants.USER_EMAIL_KEY));
		rates.setPassword(conf.getString(ConfigurationConstants.USER_PASSWORD_KEY));
		rates.setRates(intervals);
		return rates;
	}

	public class SyncSessionTask extends AsyncTask<String, Void, Boolean> {
		private long sessionId = -1;
		private Context context = null;
		
		public SyncSessionTask(Context ctx) {
			context = ctx;
		}
		
		@Override
		protected Boolean doInBackground(String... params) {
			try {
				sessionId = Long.parseLong(params[0]);
				HeartRateSessionDAO sessionDAO = new HeartRateSessionDAO(context);
				sessionDAO.open();
				HeartRateSession session = sessionDAO.findById(sessionId);
				if (session.getStatus() != HeartRateSession.COMPLETED_STATUS) {
					sessionDAO.close();
					return null;
				}
				session.setStatus(HeartRateSession.SYNCHRONIZING_STATUS);
				sessionDAO.update(session);
				sessionDAO.close();
				ICardioMoodService service = CardioMoodService.getInstance();
				
				CardioMoodSimpleResponse response = service.syncRates(params[1]);
				Log.d("CardioMood", "SessionDetailActivity.SyncSessionTask.doInBackGround -> response = " + response);
				if (response != null && response.getResponse() == 1) {
					return true;
				}
			} catch (Exception e) {
				HeartRateSessionDAO sessionDAO = new HeartRateSessionDAO(context);
				sessionDAO.open();
				HeartRateSession session = sessionDAO.findById(sessionId);
				session.setStatus(HeartRateSession.COMPLETED_STATUS);
				sessionDAO.update(session);
				sessionDAO.close();
				Log.d("CardioMood", "SessionDetailActivity.SyncSessionTask.doInBackGround -> exception = " + e.getMessage());
				return false;
			}
			return false;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			if (success == null) {
				return;
			}
			if (success) {
				if (context != null) {
					HeartRateSessionDAO sessionDAO = new HeartRateSessionDAO(context);
					sessionDAO.open();
					HeartRateSession session = sessionDAO.findById(sessionId);
					session.setStatus(HeartRateSession.SYNCRONIZED_STATUS);
					sessionDAO.update(session);
					sessionDAO.close();
					Toast.makeText(context, "Session #" + sessionId + " has been sent to remote server.", Toast.LENGTH_SHORT).show();
				}
			} else {
				HeartRateSessionDAO sessionDAO = new HeartRateSessionDAO(context);
				sessionDAO.open();
				HeartRateSession session = sessionDAO.findById(sessionId);
				session.setStatus(HeartRateSession.COMPLETED_STATUS);
				sessionDAO.update(session);
				sessionDAO.close();
				if (context != null) {
					Toast.makeText(context, "Operation failed.", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}
}
