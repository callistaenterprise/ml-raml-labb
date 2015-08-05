package com.az.ip.api.persistence.jpa;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface PatientDoctorStudyRepository extends PagingAndSortingRepository<PatientDoctorStudyEntity, String> {
}
