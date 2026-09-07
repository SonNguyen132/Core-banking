package com.finaegis.domain.stablecoin.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StablecoinSupplyRepository extends JpaRepository<StablecoinSupply, String> {
}
