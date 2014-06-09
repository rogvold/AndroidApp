package com.cardiomood.data.json;

import com.google.gson.Gson;

import java.io.Serializable;

/**
 * Created by danon on 08.03.14.
 */
public class UserProfile implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();

    public static enum Gender {
        MALE, FEMALE, UNSPECIFIED
    }

    public static enum AccountStatus {
        FREE, GOLD
    }

    public static enum Status {
        ACTIVE, BANNED, DELETED
    }

    public static enum Role {
        USER, ADMIN
    }

    private Long id;
    private String firstName;
    private String lastName;
    private Role userRole;
    private AccountStatus accountStatus;
    private Status userStatus;
    private Long registrationDate;
    private Long lastLoginDate;
    private Long lastModificationDate;
    private Double weight;
    private Double height;
    private Gender gender;
    private String phoneNumber;
    private Long birthTimestamp;

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Long getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(Long lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Long getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Long registrationDate) {
        this.registrationDate = registrationDate;
    }

    public Role getUserRole() {
        return userRole;
    }

    public void setUserRole(Role userRole) {
        this.userRole = userRole;
    }

    public Status getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(Status userStatus) {
        this.userStatus = userStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLastModificationDate() {
        return lastModificationDate;
    }

    public void setLastModificationDate(Long lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Long getBirthTimestamp() {
        return birthTimestamp;
    }

    public void setBirthTimestamp(Long birthTimestamp) {
        this.birthTimestamp = birthTimestamp;
    }

    @Override
    public String toString() {
        return GSON.toJson(this);
    }

}
