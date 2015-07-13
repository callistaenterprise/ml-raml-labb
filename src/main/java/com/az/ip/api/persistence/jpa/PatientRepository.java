package com.az.ip.api.persistence.jpa;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface PatientRepository extends PagingAndSortingRepository<Patient, Long> {
    Patient findByUsername(String username);
}
