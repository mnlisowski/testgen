package com.mlisows.testgen.infrastructure.persistence.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ObservedProfileRepository extends JpaRepository<ObservedProfileEntity, UUID> {
}
