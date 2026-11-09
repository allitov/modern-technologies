package io.allitov.cadr.repository;

import io.allitov.cadr.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Доступ к таблице {@code assets}.
 */
public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

    List<Asset> findByAssignedFalse();

    boolean existsByInventoryNumber(String inventoryNumber);
}
