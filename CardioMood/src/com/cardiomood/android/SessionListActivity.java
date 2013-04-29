package com.cardiomood.android;

import java.util.List;

import com.cardiomood.android.db.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateSession;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;

/**
 * An activity representing a list of Sessions. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link SessionDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link SessionListFragment} and the item details (if present) is a
 * {@link SessionDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link SessionListFragment.Callbacks} interface to listen for item
 * selections.
 */
public class SessionListActivity extends FragmentActivity implements
		SessionListFragment.Callbacks {

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	private boolean mTwoPane;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_session_list);
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);

		if (findViewById(R.id.session_detail_container) != null) {
			// The detail container view will be present only in the
			// large-screen layouts (res/values-large and
			// res/values-sw600dp). If this view is present, then the
			// activity should be in two-pane mode.
			mTwoPane = true;

			// In two-pane mode, list items should be given the
			// 'activated' state when touched.
			((SessionListFragment) getSupportFragmentManager()
					.findFragmentById(R.id.session_list))
					.setActivateOnItemClick(true);
		}

		// TODO: If exposing deep links into your app, handle intents here.
	}
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_session_list, menu);
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
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.menu_sync_sessions:
			attemptSyncSessions();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Callback method from {@link SessionListFragment.Callbacks} indicating
	 * that the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(Long id) {
		if (mTwoPane) {
			// In two-pane mode, show the detail view in this activity by
			// adding or replacing the detail fragment using a
			// fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putLong(SessionDetailFragment.ARG_SESSION_ID, id);
			SessionDetailFragment fragment = new SessionDetailFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.session_detail_container, fragment).commit();

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected item ID.
			Intent detailIntent = new Intent(this, SessionDetailActivity.class);
			detailIntent.putExtra(SessionDetailFragment.ARG_SESSION_ID, id);
			startActivity(detailIntent);
		}
	}
	
	private void attemptSyncSessions() {
		HeartRateSessionDAO sessionDAO = new HeartRateSessionDAO(this);
		sessionDAO.open();
		List<HeartRateSession> sessions = sessionDAO.getAllSessions();
		sessionDAO.close();
		SessionDetailActivity act = new SessionDetailActivity();
		for (HeartRateSession session: sessions) {
			if(session.getStatus() == HeartRateSession.COMPLETED_STATUS)
				act.attemptSyncSession(this, session.getId());
		}
		
	}
}
