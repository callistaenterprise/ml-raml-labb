package com.az.ip.api.persistence.jpa;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface DoctorRepository extends PagingAndSortingRepository<JpaDoctor, String> {
    JpaDoctor findByUsername(String username);
}
