package com.cardiomood.android.air.tools;

import android.text.TextUtils;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by danon on 14.08.2014.
 */
public abstract class ParseTools {

    public static final int DEFAULT_PARSE_QUERY_LIMIT = 100;

    public static String getUserFullName(ParseUser pu) {
        String fullName = pu.has("lastName") ? pu.getString("lastName") : "";
        if (!TextUtils.isEmpty(fullName))
            fullName += " ";
        if (pu.has("firstName"))
            fullName += pu.getString("firstName");
        return fullName;
    }

    public static <T extends ParseObject> Task<List<T>> fetchAllParseObjectsAsync(Class<T> clazz) {
        return fetchParseObjectsRecursively(clazz, 0, DEFAULT_PARSE_QUERY_LIMIT, Task.<List<T>>create(), new ArrayList<T>());
    }

    public static <T extends ParseObject> Task<List<T>> fetchAllParseObjectsAsync(ParseQuery<T> query) {
        return fetchParseObjectsRecursively(query, DEFAULT_PARSE_QUERY_LIMIT, Task.<List<T>>create(), new ArrayList<T>());
    }

    private static <T extends ParseObject> Task<List<T>> fetchParseObjectsRecursively(
            final Class<T> clazz,
            final int skip,
            final int limit,
            final Task<List<T>>.TaskCompletionSource task,
            final List<T> taskResult
    ) {
        ParseQuery<T> query = ParseQuery.getQuery(clazz)
                .setLimit(limit)
                .setSkip(skip);
        findAsync(query).continueWith(new Continuation<List<T>, Object>() {
            @Override
            public Object then(Task<List<T>> listTask) throws Exception {
                if (listTask.isFaulted()) {
                    task.setError(listTask.getError());
                    return null;
                } else if (listTask.isCompleted()) {
                    List<T> result = listTask.getResult();
                    if (!result.isEmpty()) {
                        taskResult.addAll(result);
                        if (result.size() == limit) {
                            fetchParseObjectsRecursively(clazz, skip + limit, limit, task, taskResult);
                            return null;
                        }
                    }
                    if (result.size() < limit) {
                        task.setResult(taskResult);
                    }
                }
                return null;
            }
        });

        return task.getTask();
    }

    private static <T extends ParseObject> Task<List<T>> fetchParseObjectsRecursively(
            final ParseQuery<T> query,
            final int limit,
            final Task<List<T>>.TaskCompletionSource task,
            final List<T> taskResult
    ) {
        query.setLimit(limit);
        findAsync(query).continueWith(new Continuation<List<T>, Object>() {
            @Override
            public Object then(Task<List<T>> listTask) throws Exception {
                if (listTask.isFaulted()) {
                    task.setError(listTask.getError());
                    return null;
                } else if (listTask.isCompleted()) {
                    List<T> result = listTask.getResult();
                    if (!result.isEmpty()) {
                        taskResult.addAll(result);
                        if (result.size() == DEFAULT_PARSE_QUERY_LIMIT) {
                            query.setSkip(query.getSkip() + limit);
                            fetchParseObjectsRecursively(query, limit, task, taskResult);
                            return null;
                        }
                    }
                    if (result.size() < limit) {
                        task.setResult(taskResult);
                    }
                }
                return null;
            }
        });

        return task.getTask();
    }

    public static <T extends ParseObject> Task<List<T>> saveAllAsync(final List<T> parseObjects) {
        return Task.callInBackground(new Callable<List<T>>() {
            @Override
            public List<T> call() throws Exception {
                ParseObject.saveAll((List<ParseObject>) parseObjects);
                return parseObjects;
            }
        });
    }

    public static <T extends ParseObject> Task<T> saveAsync(final T parseObject) {
        return Task.callInBackground(new Callable<T>() {
            @Override
            public T call() throws Exception {
                parseObject.save();
                return parseObject;
            }
        });
    }

    public static <T extends ParseObject> Task<List<T>> findAsync(ParseQuery<T> query) {
        final Task<List<T>>.TaskCompletionSource task = Task.create();
        query.findInBackground(new FindCallback<T>() {
            @Override
            public void done(List<T> ts, ParseException e) {
                if (e == null) {
                    task.setResult(ts);
                } else {
                    task.setError(e);
                }
            }
        });
        return task.getTask();
    }

    public static <T extends ParseObject> Task<T> fetchAsync(T obj) {
        final Task<T>.TaskCompletionSource task = Task.<T> create();
        obj.fetchInBackground(new GetCallback<T>() {
            public void done(T object, ParseException e) {
                if (e == null) {
                    task.setResult(object);
                } else {
                    task.setError(e);
                }
            }
        });
        return task.getTask();
    }

}
