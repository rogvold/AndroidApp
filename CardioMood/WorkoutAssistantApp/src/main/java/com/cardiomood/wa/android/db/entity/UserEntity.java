package com.cardiomood.wa.android.db.entity;

import com.j256.ormlite.field.DatabaseField;

/**
 * Created by danon on 19.04.2014.
 */

public class UserEntity {

    @DatabaseField(generatedId = true)
    private long id;

    @DatabaseField(index = true)
    private Long externalId;

    @DatabaseField(index = true)
    private String email;

    @DatabaseField(index = true)
    private String password;

    public UserEntity() {
        // required by OrmLite!!!
    }

    public UserEntity(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public UserEntity(long id, Long externalId, String email, String password) {
        this.id = id;
        this.externalId = externalId;
        this.email = email;
        this.password = password;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    @Override
    public String toString() {
        return "UserEntity{" +
                "id=" + id +
                ", externalId=" + externalId +
                ", email='" + email + '\'' +
                '}';
    }
}
