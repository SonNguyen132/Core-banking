package com.finaegis.domain.asset.service;

import com.finaegis.domain.asset.model.Asset;
import com.finaegis.domain.asset.model.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;

    public List<Asset> listActive() {
        return assetRepository.findByActiveTrue();
    }

    public Asset find(String code) {
        return assetRepository.findByCodeAndActiveTrue(code)
            .orElseThrow(() -> new RuntimeException("Asset not found: " + code));
    }
}
