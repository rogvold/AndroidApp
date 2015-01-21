package com.cardiomood.framework.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.cardiomood.android.tools.ReachabilityTest;
import com.cardiomood.framework.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


/**
 * Created by danshin on 01.11.13.
 */
public abstract class AbstractHistoryFragment<E> extends Fragment
        implements SearchView.OnQueryTextListener, ActionMode.Callback {

    private static final String TAG = AbstractHistoryFragment.class.getSimpleName();

    private ListView listView;
    private View root;
    private SelectableAdapter<E> listAdapter = null;
    private ActionMode mActionMode = null;
    private List<E> mItems = new ArrayList<>();
    private AbstractEndlessAdapter<E> mEndlessAdapter;
    private SyncStrategy syncStrategy = SyncStrategy.SYNC_WHEN_MODIFIED;

    // work around for 'view already has a parent...'
    private boolean initial = true;

    protected abstract ArrayAdapter<E> createArrayAdapter(Context context, List<E> dataSource);
    protected abstract AbstractEndlessAdapter<E> createEndlessAdapter(Context context, SelectableAdapter<E> wrapped);
    protected abstract void openItem(E item);
    protected abstract void onRenameItem(E item);
    protected abstract void onDeleteItem(E item);
    protected abstract Task<Boolean> checkInternetConnection();
    protected abstract void doSync();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listAdapter = new SelectableAdapter<>(createArrayAdapter(getActivity(), mItems));
        mEndlessAdapter = createEndlessAdapter(getActivity(), listAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_history, container, false);
        listView = (ListView) root.findViewById(R.id.sessionList);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode == null) {
                    E item = listAdapter.getItem(position);
                    openItem(item);
                } else {
                    view.setSelected(true);
                    listAdapter.setSelectedItem(position);
                    listAdapter.notifyDataSetChanged();
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode == null) {
                    // Start the CAB using this activity for callbacks
                    @SuppressWarnings("unchecked")
                    ActionBarActivity activity = (ActionBarActivity) getActivity();
                    mActionMode = activity.startSupportActionMode(AbstractHistoryFragment.this);
                    listAdapter.setSelectedItem(position);
                    listAdapter.notifyDataSetChanged();
                }
                return true;
            }
        });
        listView.setAdapter(mEndlessAdapter);

        return root;
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.history_context_menu, menu);
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        // Return false if nothing is done
        return false;
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_delete) {
            if (listAdapter.getSelectedItem() >= 0)
                deleteItem(listAdapter.getSelectedItem());
            mode.finish(); // Action picked, so close the CAB
            return true;
        }
        if (itemId == R.id.menu_rename_item) {
            if (listAdapter.getSelectedItem() >= 0)
                renameItem(listAdapter.getSelectedItem());
            mode.finish(); // Action picked, so close the CAB
            return true;
        }
        return false;
    }

    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        if (listAdapter != null) {
            listAdapter.setSelectedItem(-1);
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        // work around for app crash due to 'view already has a parent...' - bug in EndlessAdapter
        if (initial) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    refresh();
                    initial = false;
                }
            }, 1000);
        } else {
            refresh();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_sessions_list, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        MenuItem menuItem = menu.findItem(R.id.action_search);
        SearchView searchView = new SearchView(getActivity());
        MenuItemCompat.setActionView(menuItem, searchView);
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getActivity().getComponentName()));

        configureSearchView(searchView);

        super.onCreateOptionsMenu(menu, inflater);
    }

    protected void configureSearchView(SearchView searchView) {
        EditText txtSearch = ((EditText) searchView.findViewById(R.id.search_src_text));
        txtSearch.setHintTextColor(Color.DKGRAY);
        txtSearch.setTextColor(Color.WHITE);
        txtSearch.setHint("Search in history");
        searchView.setOnQueryTextListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
                sync();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected final void deleteItem(int position) {
        final E item = listAdapter.getItem(position);
        onDeleteItem(item);
    }


    protected final void renameItem(int i) {
        final E item = listAdapter.getItem(i);
        onRenameItem(item);
    }

    protected void testReachability(String host, int port, ReachabilityTest.Callback callback) {
        new ReachabilityTest(
                getActivity(),
                "api.parse.com",
                80,
                callback
        ).execute();
    }

    protected void sync() {
        checkInternetConnection()
                .continueWith(new Continuation<Boolean, Object>() {
                    @Override
                    public Object then(Task<Boolean> task) throws Exception {
                        Context context = getActivity();
                        if (task.isFaulted()) {
                            Log.w(TAG, "checkInternetConnection() failed", task.getError());
                            if (context != null) {
                                Toast.makeText(context, "Connection failed.", Toast.LENGTH_SHORT).show();
                            }
                            simpleRefresh();
                        } else if (task.isCompleted()) {
                            boolean connected = task.getResult();
                            if (connected) {
                                doSync();
                            } else {
                                Toast.makeText(context, "Cannot connect with the server.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);
    }

    protected final void refresh() {
        if (mActionMode != null) {
            return;
        }

        // started?
        if (initial && syncStrategy == SyncStrategy.SYNC_ON_START) {
            sync();
            return;
        }

        Task.callInBackground(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return hasUpdatedSessions();
            }
        }).continueWith(new Continuation<Boolean, Object>() {
            @Override
            public Object then(Task<Boolean> task) throws Exception {
                Activity activity = getActivity();
                if (activity == null)
                    return null;
                if (!task.isFaulted() && task.isCompleted()) {
                    if (syncStrategy == SyncStrategy.SYNC_WHEN_MODIFIED) {
                        if (task.getResult()) {
                            sync();
                            return null;
                        }
                    }
                }
                simpleRefresh();
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);

    }

    protected boolean hasUpdatedSessions() {
        return false;
    }

    protected final void simpleRefresh() {
        listAdapter.setSelectedItem(-1);
        mEndlessAdapter.refresh();
    }


    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        mEndlessAdapter.getFilter().filter(s);
        return false;
    }

    public static enum SyncStrategy {
        SYNC_WHEN_MODIFIED,
        SYNC_ON_START,
        SYNC_ON_DEMAND
    }

}