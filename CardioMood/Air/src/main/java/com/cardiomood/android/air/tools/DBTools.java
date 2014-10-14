package com.cardiomood.android.air.tools;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.PreparedQuery;

import java.util.List;
import java.util.concurrent.Callable;

import bolts.Task;

/**
 * Created by antondanhsin on 14/10/14.
 */
public class DBTools {

    public static <T> Task<List<T>> executeQueryAsync(final BaseDaoImpl<T, ?> dao, final PreparedQuery<T> query) {
        return Task.callInBackground(new Callable<List<T>>() {
            @Override
            public List<T> call() throws Exception {
                return dao.query(query);
            }
        });
    }

}
