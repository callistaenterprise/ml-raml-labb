package com.az.ip.api.persistence.jpa;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface PatientRepository extends PagingAndSortingRepository<JpaPatient, String> {
    JpaPatient findByUsername(String username);
}
