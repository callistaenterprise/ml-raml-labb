package com.az.ip.api.persistence.jpa;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface StudyRepository extends PagingAndSortingRepository<StudyEntity, String> {
    StudyEntity findByName(String name);
}
