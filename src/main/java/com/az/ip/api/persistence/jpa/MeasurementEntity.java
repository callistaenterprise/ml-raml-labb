package com.az.ip.api.persistence.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import java.util.Date;

import static javax.persistence.TemporalType.DATE;
import static javax.persistence.TemporalType.TIMESTAMP;

@Entity
public class MeasurementEntity extends AbstractEntity {

    private String description;
    @Temporal(TIMESTAMP)
    private Date timestamp;
    private int steps;

    @ManyToOne
    private PatientDoctorStudyEntity patient;


    /**
     * Constructor for new (not yet persited) entities, without specifying id and version
     *
     * @param patient
     * @param description
     * @param timestamp
     * @param steps
     */
    public MeasurementEntity(PatientDoctorStudyEntity patient, String description, Date timestamp, int steps) {

        this.patient = patient;
        this.description = description;
        this.timestamp = timestamp;
        this.steps = steps;
    }

    /**
     * Constructor for existing (already persisted) entities, specifying id, version together with the actual business information regarding the entity
     *
     * @param id
     * @param version
     * @param patient
     * @param description
     * @param timestamp
     * @param steps
     */
    public MeasurementEntity(String id, int version, PatientDoctorStudyEntity patient, String description, Date timestamp, int steps) {
        this(patient, description, timestamp, steps);
        setIdAndVersionForExistingEntity(id, version);
    }

    /**
     * The default constructor is required by the JPA implementation, but can be set protected to protect is from public visibility
     */
    protected MeasurementEntity() {}

    public PatientDoctorStudyEntity getPatient() {
        return patient;
    }

    public void setPatient(PatientDoctorStudyEntity patient) {
        this.patient = patient;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }
}
