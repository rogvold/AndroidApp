package com.cardiomood.android.air.db;

import com.cardiomood.android.air.data.Aircraft;
import com.cardiomood.android.air.data.SynchronizableParseObject;
import com.cardiomood.android.air.db.entity.AircraftEntity;
import com.cardiomood.android.air.db.entity.SyncEntity;
import com.cardiomood.android.air.tools.DBTools;
import com.cardiomood.android.air.tools.ParseTools;
import com.j256.ormlite.stmt.PreparedQuery;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by antondanhsin on 13/10/14.
 */
public class SyncHelper {

    private Date lastSyncDate = new Date(0);

    public void syncAircraftsInBackground() {
        ParseTools.fetchAllParseObjectsAsync(Aircraft.class).continueWithTask(new Continuation<List<Aircraft>, Task<List<Aircraft>>>() {
            @Override
            public Task<List<Aircraft>> then(Task<List<Aircraft>> listTask) throws Exception {
                if (listTask.isFaulted()) {
                    // failed to fetch objects!!!!
                } else if (listTask.isCompleted()) {
                    // objects were retrieved
                    return syncParseObjects(listTask.getResult());
                }
                return null;
            }
        }).continueWithTask(new Continuation<List<Aircraft>, Task<List<AircraftEntity>>>() {
            @Override
            public Task<List<AircraftEntity>> then(Task<List<Aircraft>> task) throws Exception {
                if (task.isFaulted()) {
                    // task is faulted
                } else if (task.isCompleted()) {
                    AircraftDAO dao = HelperFactory.getHelper().getAircraftDao();
                    PreparedQuery<AircraftEntity> query = dao.queryBuilder().where().gt("sync_updated_at", lastSyncDate.getTime()).prepare();
                    return DBTools.executeQueryAsync(dao, query);
                }
                return null;
            }
        }).continueWithTask(new Continuation<List<AircraftEntity>, Task<List<Aircraft>>>() {
            @Override
            public Task<List<Aircraft>> then(Task<List<AircraftEntity>> task) throws Exception {
                if (task.isFaulted()) {

                } else if (task.isCompleted()) {
                    List<AircraftEntity> localAircrafts = task.getResult();
                    List<Aircraft> parseObjects = new ArrayList<Aircraft>(localAircrafts.size());
                    for (AircraftEntity entity: localAircrafts) {
                        Aircraft parseObject = (Aircraft) SyncEntity.toParseObject(entity);
                        parseObjects.add(parseObject);
                    }
                    return ParseTools.saveAllAsync(parseObjects);
                }
                return null;
            }
        });
    }

    public <T extends SynchronizableParseObject> Task<List<T>> syncParseObjects(final List<T> planes) throws SQLException {
        return Task.callInBackground(new Callable<List<T>>() {
            @Override
            public List<T> call() throws Exception {
                for (SynchronizableParseObject plane: planes) {
                    SyncEntity remoteEntity = SyncEntity.fromParseObject(plane, plane.getEntityClass());
                    SyncDAO dao = HelperFactory.getHelper().getDaoForClass(remoteEntity.getClass());
                    SyncEntity localEntity = dao.findBySyncId(remoteEntity.getSyncId());

                    if (localEntity == null) {
                        dao.create(dao.getDataClass().cast(remoteEntity));
                    } else {
                        // compare updated_at field
                        long localTime = localEntity.getSyncDate() != null ?
                                localEntity.getSyncDate().getTime() : 0L;
                        long remoteTime = remoteEntity.getSyncDate() != null ?
                                remoteEntity.getSyncDate().getTime() : 0L;


                        if (localTime < remoteTime) {
                            SyncEntity.fromParseObject(plane, localEntity);
                            dao.update(dao.getDataClass().cast(localEntity));
                        } else if (localTime > remoteTime) {
                            // object must be updated locally
                            SyncEntity.toParseObject(localEntity, plane);
                            plane.save();
                        }
                    }
                }

                return planes;
            }
        });

    }

}
