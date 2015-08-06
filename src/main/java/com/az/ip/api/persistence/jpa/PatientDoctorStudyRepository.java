package com.az.ip.api.persistence.jpa;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface PatientDoctorStudyRepository extends PagingAndSortingRepository<PatientDoctorStudyEntity, String> {

    // FIXME: Hot to perform a lookup of a single entity?
    List<PatientDoctorStudyEntity> findByPatientAndDoctorAndStudy(PatientEntity patient, DoctorEntity doctorId, StudyEntity studyId);

    List<PatientDoctorStudyEntity> findByStudyAndDoctor(StudyEntity studyId, DoctorEntity doctorId);
}
