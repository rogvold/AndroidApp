package com.cardiomood.sport.android.client.json;

import java.io.Serializable;

/**
 * Project: CardioSport
 * User: danon
 * Date: 15.06.13
 * Time: 18:20
 */
public class JsonTrainee implements Serializable {
    private Long id;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phone;
    private long currentWorkoutId;

    public JsonTrainee() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public long getCurrentWorkoutId() {
        return currentWorkoutId;
    }

    public void setCurrentWorkoutId(long currentWorkoutId) {
        this.currentWorkoutId = currentWorkoutId;
    }
}
