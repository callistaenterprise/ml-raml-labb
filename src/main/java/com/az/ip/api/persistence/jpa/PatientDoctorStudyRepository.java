package com.az.ip.api.persistence.jpa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface PatientDoctorStudyRepository extends PagingAndSortingRepository<PatientDoctorStudyEntity, String> {

    // FIXME: Can these find methods be performed on ID's insted of entities???

    // FIXME: How to perform a lookup of a single entity and not a list?
    List<PatientDoctorStudyEntity> findByPatientAndDoctorAndStudy(PatientEntity patient, DoctorEntity doctorId, StudyEntity studyId);

    List<PatientDoctorStudyEntity> findByStudyAndDoctor(StudyEntity studyId, DoctorEntity doctorId);

    List<PatientDoctorStudyEntity> findByStudy(StudyEntity studyId);

    List<PatientDoctorStudyEntity> findByDoctor(DoctorEntity doctorId);

    List<PatientDoctorStudyEntity> findByPatient(PatientEntity patientId);

    @Query("select pds from PatientDoctorStudyEntity pds where pds.patient.id = ?")
    List<PatientDoctorStudyEntity> findByPatientId(String patientId);

    @Query("select pds from PatientDoctorStudyEntity pds where pds.patient.id = ? and pds.study.id = ?")
    List<PatientDoctorStudyEntity> findByPatientIdAndStudyId(String patientId, String studyId);
}
