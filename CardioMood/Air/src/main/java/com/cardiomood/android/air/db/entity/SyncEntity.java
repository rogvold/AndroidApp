package com.cardiomood.android.air.db.entity;

import com.cardiomood.android.air.db.annotations.ParseClass;
import com.cardiomood.android.air.db.annotations.ParseField;
import com.cardiomood.android.air.tools.ReflectionUtils;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.parse.ParseObject;

import java.lang.reflect.Field;
import java.util.Date;

/**
 * Created by antondanhsin on 08/10/14.
 */
public abstract class SyncEntity {

    @DatabaseField(columnName = "_id", generatedId = true)
    private Long id;

    @DatabaseField(columnName = "sync_id", unique = true)
    private String syncId;

    @DatabaseField(columnName = "sync_timestamp", dataType = DataType.DATE_LONG)
    private Date syncDate;

    @DatabaseField(columnName = "creation_timestamp", dataType = DataType.DATE_LONG)
    private Date creationDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSyncId() {
        return syncId;
    }

    public void setSyncId(String syncId) {
        this.syncId = syncId;
    }

    public Date getSyncDate() {
        return syncDate;
    }

    public void setSyncDate(Date syncDate) {
        this.syncDate = syncDate;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public static <T extends SyncEntity> void fromParseObject(final ParseObject parseObject, final T entity) {
        try {
            ReflectionUtils.doWithFields(
                    entity.getClass(),
                    new ReflectionUtils.FieldCallback() {
                        @Override
                        public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                            ParseField a = field.getAnnotation(ParseField.class);
                            if (a == null)
                                return;
                            String parseFieldName = field.getName();
                            if (a.name() != null && !a.name().isEmpty()) {
                                parseFieldName = a.name();
                            }

                            boolean accessible = field.isAccessible();
                            field.setAccessible(true);
                            Object remoteValue = parseObject.get(parseFieldName);
                            Class localValueType = field.getType();
                            field.set(entity, convertValue(remoteValue, localValueType));
                            field.setAccessible(accessible);
                        }
                    }
            );

            entity.setSyncId(parseObject.getObjectId());
            entity.setSyncDate(parseObject.getUpdatedAt());
            entity.setCreationDate(parseObject.getCreatedAt());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T extends SyncEntity> T fromParseObject(ParseObject parseObject, Class<T> entityClass) {
        try {
            final T entity = entityClass.newInstance();
            fromParseObject(parseObject, entity);
            return entity;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static <T> T convertValue(Object value, Class<T> targetClass) {
        if (targetClass.isInstance(value)) {
            // no need to convert types
            return (T) value;
        }

        // special handling for null value
        if (value == null) {
            if (targetClass.isPrimitive()) {
                // return 0 or FALSE or null
                if (char.class.equals(targetClass))
                    return (T) new Character('\u0000');
                if (boolean.class.equals(targetClass))
                    return (T) Boolean.FALSE;
                if (byte.class.equals(targetClass))
                    return (T) Byte.valueOf((byte) 0);
                if (short.class.equals(targetClass))
                    return (T) Short.valueOf((short) 0);
                if (int.class.equals(targetClass))
                    return (T) Integer.valueOf(0);
                if (long.class.equals(targetClass))
                    return (T) Long.valueOf(0L);
                if (float.class.equals(targetClass))
                    return (T) Float.valueOf(0.0f);
                if (double.class.equals(targetClass))
                    return (T) Double.valueOf(0.0d);
            }

            // just return null
            return null;
        }

        // ok, value is not null and requires conversion
        if (String.class.equals(targetClass)) {
            // numbers will be converted to String too! :)
            return (T) value.toString();
        }

        if (Date.class.equals(targetClass)) {
            if (Number.class.isInstance(value)) {
                return (T) new Date(((Number) value).longValue());
            }
        }

        if (Long.class.equals(targetClass)) {
            if (Date.class.isInstance(value)) {
                return (T) Long.valueOf((Long) value);
            }
        }

        // target class is not String
        if (Boolean.class.equals(targetClass)) {
            return (T) Boolean.valueOf(value.toString());
        }
        if (Character.class.equals(targetClass)) {
            if (value.toString().length() == 1) {
                return (T) Character.valueOf(value.toString().charAt(0));
            }
        }
        if (Byte.class.equals(targetClass)) {
            return (T) Byte.valueOf(value.toString());
        }
        if (Short.class.equals(targetClass)) {
            return (T) Short.valueOf(value.toString());
        }
        if (Integer.class.equals(targetClass)) {
            return (T) Integer.valueOf(value.toString());
        }
        if (Long.class.equals(targetClass)) {
            return (T) Long.valueOf(value.toString());
        }
        if (Float.class.equals(targetClass)) {
            return (T) Float.valueOf(value.toString());
        }
        if (Double.class.equals(targetClass)) {
            return (T) Double.valueOf(value.toString());
        }

        // just try to cast (this probably won't work)
        return (T) value;
    }

    public static <T extends SyncEntity, P extends ParseObject> void toParseObject(final T entity, final P parseObject) {
        try {
            Class entityClass = entity.getClass();

            // update objectId
            if (entity.getSyncId() != null) {
                Field objectIdField = ParseObject.class.getDeclaredField("objectId");
                objectIdField.setAccessible(true);
                objectIdField.set(parseObject, entity.getSyncId());
                objectIdField.setAccessible(false);
            }

            // update createdAt
            if (entity.getCreationDate() != null) {
                Field createdAtField = ParseObject.class.getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(parseObject, entity.getCreationDate());
                createdAtField.setAccessible(false);
            }

            // update updatedAt
            if (entity.getCreationDate() != null) {
                Field updatedAtField = ParseObject.class.getDeclaredField("updatedAt");
                updatedAtField.setAccessible(true);
                updatedAtField.set(parseObject, entity.getCreationDate());
                updatedAtField.setAccessible(false);
            }

            ReflectionUtils.doWithFields(
                    entityClass,
                    new ReflectionUtils.FieldCallback() {
                        @Override
                        public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                            ParseField a = field.getAnnotation(ParseField.class);
                            if (a == null)
                                return;
                            String parseFieldName = field.getName();
                            if (a.name() != null && !a.name().isEmpty()) {
                                parseFieldName = a.name();
                            }

                            boolean accessible = field.isAccessible();
                            field.setAccessible(true);
                            parseObject.put(parseFieldName, field.get(entity));
                            field.setAccessible(accessible);
                        }
                    }
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T extends SyncEntity> ParseObject toParseObject(T entity) {
        try {
            Class entityClass = entity.getClass();
            ParseClass a = (ParseClass) entityClass.getAnnotation(ParseClass.class);
            String parseClassName = entityClass.getSimpleName();

            if (a.name() != null && !a.name().isEmpty()) {
                parseClassName = a.name();
            }

            ParseObject parseObject = ParseObject.create(parseClassName);
            toParseObject(entity, parseObject);

            return parseObject;

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
