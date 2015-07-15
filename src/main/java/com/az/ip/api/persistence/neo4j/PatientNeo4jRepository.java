package com.az.ip.api.persistence.neo4j;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface PatientNeo4jRepository extends PagingAndSortingRepository<Patient, Long> {
    Patient findByUsername(String username);
}
