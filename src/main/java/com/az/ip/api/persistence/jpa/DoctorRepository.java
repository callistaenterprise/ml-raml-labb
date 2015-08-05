package com.az.ip.api.persistence.jpa;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface DoctorRepository extends PagingAndSortingRepository<DoctorEntity, String> {
    DoctorEntity findByUsername(String username);
}
