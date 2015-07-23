package com.az.ip.api.persistence.jpa;

import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class Patient extends AbstractEntity {

    @Column(unique=true)
    private String username;

    private String patientID;
    private String firstname;
    private String lastname;
    private Integer weight;
    private Integer height;


    public Patient(String username, String patientID, String firstname, String lastname, Integer weight, Integer height) {

        Assert.hasText(username);
        Assert.hasText(firstname);
        Assert.hasText(lastname);

        this.username = username;
        this.patientID = patientID;
        this.firstname = firstname;
        this.lastname = lastname;
        this.weight = weight;
        this.height = height;
    }

    protected Patient() {}

    public String getUsername() {
        return username;
    }
    public String getPatientID() {
        return patientID;
    }
    public String getFirstname() {
        return firstname;
    }
    public String getLastname() {
        return lastname;
    }
    public Integer getWeight() {
        return weight;
    }
    public Integer getHeight() {
        return height;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }


}