package com.cardiomood.framework.db.entity;

/**
 * Interface that represents recording.
 * Provides access to essential data, such as id, name, description,
 * start and end timestamp
 *
 * Created by Anton Danshin on 08/01/15.
 */
public interface AbstractSessionEntity {

    Long getId();

    String getName();
    void setName(String name);

    String getDescription();
    void setDescription(String description);

    long getStartTimestamp();
    void setStartTimestamp(long startTimestamp);

    long getEndTimestamp();
    void setEndTimestamp(long startTimestamp);

}
