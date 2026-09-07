package com.finaegis.domain.asset.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, String> {

    List<Asset> findByActiveTrue();

    Optional<Asset> findByCodeAndActiveTrue(String code);
}
