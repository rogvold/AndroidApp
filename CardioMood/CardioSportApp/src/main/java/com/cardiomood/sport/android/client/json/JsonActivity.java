package com.cardiomood.sport.android.client.json;

import java.io.Serializable;

/**
 * Project: CardioSport
 * User: danon
 * Date: 16.06.13
 * Time: 21:37
 */
public class JsonActivity implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private int minHeartRate;
    private int maxHeartRate;
    private double minTension;
    private double maxTension;
    private long duration;
    private String name;
    private String description;
    private long workoutId;
    private double minSpeed;
    private double maxSpeed;
    private int orderNumber;
    private ActivityStatus status;
    private long coachId;

    public JsonActivity() {
    }

    public JsonActivity(Long coachId, Integer minHeartRate, Integer maxHeartRate, Double minTension, Double maxTension, Long duration, String name, String description, Long workoutId, Double minSpeed, Double maxSpeed) {
        this.minHeartRate = minHeartRate;
        this.maxHeartRate = maxHeartRate;
        this.minTension = minTension;
        this.maxTension = maxTension;
        this.duration = duration;
        this.name = name;
        this.description = description;
        this.workoutId = workoutId;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        this.coachId = coachId;
        this.status = ActivityStatus.NEW;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getMinHeartRate() {
        return minHeartRate;
    }

    public void setMinHeartRate(int minHeartRate) {
        this.minHeartRate = minHeartRate;
    }

    public int getMaxHeartRate() {
        return maxHeartRate;
    }

    public void setMaxHeartRate(int maxHeartRate) {
        this.maxHeartRate = maxHeartRate;
    }

    public double getMinTension() {
        return minTension;
    }

    public void setMinTension(double minTension) {
        this.minTension = minTension;
    }

    public double getMaxTension() {
        return maxTension;
    }

    public void setMaxTension(double maxTension) {
        this.maxTension = maxTension;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(long workoutId) {
        this.workoutId = workoutId;
    }

    public double getMinSpeed() {
        return minSpeed;
    }

    public void setMinSpeed(double minSpeed) {
        this.minSpeed = minSpeed;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public ActivityStatus getStatus() {
        return status;
    }

    public void setStatus(ActivityStatus status) {
        this.status = status;
    }

    public long getCoachId() {
        return coachId;
    }

    public void setCoachId(long coachId) {
        this.coachId = coachId;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof JsonActivity)) {
            return false;
        }
        JsonActivity other = (JsonActivity) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return orderNumber + " " + name;
    }
}
