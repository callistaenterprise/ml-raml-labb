package com.az.ip.api.persistence.jpa;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface StudyRepository extends PagingAndSortingRepository<JpaStudy, String> {
    JpaStudy findByName(String name);
}
