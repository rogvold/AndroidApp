package com.cardiomood.android.services;

import android.text.TextUtils;

import java.io.Serializable;

/**
 * Entity class for JAX-RS.
 * @author Shaykhlislamov Sabir (email: sha-sabir@yandex.ru)
 */
public class User implements Serializable {

	private static final long serialVersionUID = 401325866089482972L;
	
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String department;
    private String about;
    private String diagnosis;
    private String description;
    private String statusMessage;

    private Float weight;
    private Float height;
    
    private Integer sex;
    private Integer age;
    
    public User() {
        super();
    }

    public User(String email, String password, String firstName, String lastName, String department, String about, String diagnosis, String description, String statusMessage) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.department = department;
        this.about = about;
        this.diagnosis = diagnosis;
        this.description = description;
        this.statusMessage = statusMessage;
    }

    public User(String email, String password, String firstName, String lastName, String department, String about, String diagnosis, String description, String statusMessage, Float weight, Float height, Integer sex, Integer age) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.department = department;
        this.about = about;
        this.diagnosis = diagnosis;
        this.description = description;
        this.statusMessage = statusMessage;
        this.weight = weight;
        this.height = height;
        this.sex = sex;
        this.age = age;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
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

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

	@Override
	public String toString() {
		return TextUtils.concat(firstName,  " ", lastName).toString();
	}
    
    
  
}