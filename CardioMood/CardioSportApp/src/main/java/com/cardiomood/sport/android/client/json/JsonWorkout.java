package com.cardiomood.sport.android.client.json;

import java.io.Serializable;
import java.util.List;

/**
 * Project: CardioSport
 * User: danon
 * Date: 16.06.13
 * Time: 21:34
 */
public class JsonWorkout implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<JsonActivity> activities;
    private String name;
    private String description;
    private Long startDate;
    private Long id;

    public JsonWorkout() {
    }

    public JsonWorkout(Long id, List<JsonActivity> activities, String name, String description, Long startDate) {
        this.activities = activities;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.id = id;
    }

    public List<JsonActivity> getActivities() {
        return activities;
    }

    public void setActivities(List<JsonActivity> activities) {
        this.activities = activities;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(Long startDate) {
        this.startDate = startDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
