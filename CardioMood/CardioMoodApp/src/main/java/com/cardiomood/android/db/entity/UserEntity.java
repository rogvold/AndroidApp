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

    @Override
    public String toString() {
        return "UserEntity{" +
                "id=" + id +
                ", externalId=" + externalId +
                ", email='" + email + '\'' +
                '}';
    }
}
