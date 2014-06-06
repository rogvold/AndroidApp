package com.cardiomood.android.db.entity;

import com.cardiomood.android.db.dao.UserDAO;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

/**
 * Created by danon on 27.05.2014.
 */
@DatabaseTable(tableName = "users", daoClass = UserDAO.class)
public class UserEntity implements Serializable {


    @DatabaseField(generatedId = true, columnName = "_id")
    private Long id;
    @DatabaseField(index = true, unique = true, columnName = "external_id")
    private Long externalId;
    @DatabaseField(index = true, canBeNull = false, columnName = "email")
    private String email;
    @DatabaseField(canBeNull = false, columnName = "password")
    private String password;
    @DatabaseField(columnName = "status")
    private UserStatus status;
    @DatabaseField(columnName = "first_name")
    private String firstName;
    @DatabaseField(columnName = "last_name")
    private String lastName;
    @DatabaseField(columnName = "weight")
    private Float weight;
    @DatabaseField(columnName = "height")
    private Float height;
    @DatabaseField(columnName = "birth_date")
    private Long birthDate;
    @DatabaseField(columnName = "phone_number")
    private String phoneNumber;
    @DatabaseField(columnName = "gender")
    private String gender;
    @DatabaseField(columnName = "last_modified")
    private long lastModified;

    public UserEntity() {
        status = UserStatus.NEW;
    }

    public UserEntity(Long externalId, String email, UserStatus status) {
        this.externalId = externalId;
        this.email = email;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExternalId() {
        return externalId;
    }

    public void setExternalId(Long externalId) {
        this.externalId = externalId;
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

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
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

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }

    public Long getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Long birthDate) {
        this.birthDate = birthDate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                "id=" + id +
                ", externalId=" + externalId +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", status=" + status +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", weight=" + weight +
                ", height=" + height +
                ", birthDate=" + birthDate +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", gender='" + gender + '\'' +
                ", lastModified=" + lastModified +
                '}';
    }
}
