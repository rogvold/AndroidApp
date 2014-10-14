package com.cardiomood.android.air.data;

import com.cardiomood.android.air.db.entity.SyncEntity;
import com.parse.ParseObject;

/**
 * Created by antondanhsin on 09/10/14.
 */
public abstract class SynchronizableParseObject extends ParseObject {

    public abstract Class<? extends SyncEntity> getEntityClass();

}
