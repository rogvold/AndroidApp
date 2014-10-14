package com.cardiomood.android.air;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.air.db.AirSessionDAO;
import com.cardiomood.android.air.db.HelperFactory;
import com.cardiomood.android.air.db.entity.AirSessionEntity;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class HistoryActivity extends Activity implements AdapterView.OnItemClickListener {

    private ListView airSessionsListView;

    private ArrayAdapter<AirSessionEntity> sessionArrayAdapter;
    private List<AirSessionEntity> airSessions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        airSessionsListView = (ListView) findViewById(R.id.air_sessions);
        airSessionsListView.setOnItemClickListener(this);

        // initialize planes list
        airSessions = new ArrayList<AirSessionEntity>();
        sessionArrayAdapter = new AirSessionListArrayAdapter(this, airSessions);
        airSessionsListView.setAdapter(sessionArrayAdapter);
        refreshSessionList();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.acitivity_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AirSessionEntity entity = sessionArrayAdapter.getItem(position);
        Intent intent = new Intent(this, DebriefingActivity.class);
        intent.putExtra(DebriefingActivity.EXTRA_SESSION_ID, entity.getId());
        startActivity(intent);
    }

    private void refreshSessionList() {
        Task.callInBackground(new Callable<List<AirSessionEntity>>() {
            @Override
            public List<AirSessionEntity> call() throws Exception {
                AirSessionDAO dao = HelperFactory.getHelper().getAirSessionDao();
                return dao.queryBuilder()
                        .orderBy("creation_timestamp", false)
                        .where().eq("sync_user_id", ParseUser.getCurrentUser().getObjectId())
                        .query();
            }
        }).continueWith(new Continuation<List<AirSessionEntity>, Object>() {
            @Override
            public Object then(Task<List<AirSessionEntity>> listTask) throws Exception {
                if (listTask.isFaulted()) {
                    Toast.makeText(HistoryActivity.this, "Task failed with exception: "
                            + listTask.getError().getMessage(), Toast.LENGTH_SHORT).show();
                } else if (listTask.isCompleted()) {
                    airSessions.clear();
                    airSessions.addAll(listTask.getResult());
                    sessionArrayAdapter.notifyDataSetChanged();
                }
                return null;
            }
        });
    }

    public class AirSessionListArrayAdapter extends ArrayAdapter<AirSessionEntity> {

        public AirSessionListArrayAdapter(Context context, List<AirSessionEntity> src) {
            super(context, android.R.layout.simple_list_item_2, src);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        private View getCustomView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View itemView = inflater.inflate(R.layout.two_lines_layout, parent, false);
            itemView.setBackgroundResource(R.drawable.list_selector_background);
            AirSessionEntity entity = getItem(position);

            TextView text1 = (TextView) itemView.findViewById(android.R.id.text1);
            text1.setText("AirSession " + entity.getCreationDate().toString());
            text1.setTypeface(null, Typeface.NORMAL);

            TextView text2 = (TextView) itemView.findViewById(android.R.id.text2);
            text2.setText("<some extra info>");

            return itemView;
        }
    }
}
