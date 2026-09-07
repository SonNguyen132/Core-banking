package com.finaegis.domain.asset.repository;

import com.finaegis.domain.asset.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, String> {

    List<Asset> findByActiveTrue();

    List<Asset> findByType(String type);

    @Query("SELECT a FROM Asset a WHERE a.active = true AND a.code IN :codes")
    List<Asset> findByCodes(Iterable<String> codes);
}
