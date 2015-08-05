package com.az.ip.api.persistence.jpa;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Set;

public interface PatientDoctorStudyRepository extends PagingAndSortingRepository<PatientDoctorStudyEntity, String> {

    // FIXME: Hot to perform a lookup of a single entity?
    Set<PatientDoctorStudyEntity> findByPatientAndDoctorAndStudy(PatientEntity patient, DoctorEntity doctorId, StudyEntity studyId);

    Set<PatientDoctorStudyEntity> findByStudyAndDoctor(StudyEntity studyId, DoctorEntity doctorId);
}
