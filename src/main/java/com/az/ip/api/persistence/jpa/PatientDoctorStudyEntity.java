package com.az.ip.api.persistence.jpa;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class PatientDoctorStudyEntity extends AbstractEntity {

    @ManyToOne
    @JoinColumn
    private PatientEntity patient;

    @ManyToOne
    @JoinColumn
    private DoctorEntity doctor;

    @ManyToOne
    @JoinColumn
    private StudyEntity study;

    /**
     * Constructor for new (not yet persited) entities, without specifying id and version
     *
     * @param patient
     * @param doctor
     * @param study
     */
    public PatientDoctorStudyEntity(PatientEntity patient, DoctorEntity doctor, StudyEntity study) {

        this.patient = patient;
        this.doctor = doctor;
        this.study = study;
    }

    /**
     * Constructor for existing (already persisted) entities, specifying id, version together with the actual business information regarding the entity
     *
     * @param id
     * @param version
     * @param patient
     * @param doctor
     * @param study
     */
    public PatientDoctorStudyEntity(String id, int version, PatientEntity patient, DoctorEntity doctor, StudyEntity study) {
        this(patient, doctor, study);
        setIdAndVersionForExistingEntity(id, version);
    }

    /**
     * The default constructor is required by the JPA implementation, but can be set protected to protect is from public visibility
     */
    protected PatientDoctorStudyEntity() {}

    public PatientEntity getPatient() {
        return patient;
    }

    public void setPatient(PatientEntity patient) {
        this.patient = patient;
    }

    public DoctorEntity getDoctor() {
        return doctor;
    }

    public void setDoctor(DoctorEntity doctor) {
        this.doctor = doctor;
    }

    public StudyEntity getStudy() {
        return study;
    }

    public void setStudy(StudyEntity study) {
        this.study = study;
    }
}
