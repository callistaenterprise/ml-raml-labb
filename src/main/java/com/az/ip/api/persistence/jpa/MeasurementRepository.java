package com.az.ip.api.persistence.jpa;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface MeasurementRepository extends PagingAndSortingRepository<MeasurementEntity, String> {
}
