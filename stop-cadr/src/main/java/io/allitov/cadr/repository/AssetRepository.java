package io.allitov.cadr.repository;

import io.allitov.cadr.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Доступ к таблице {@code assets}.
 */
public interface AssetRepository extends JpaRepository<Asset, Long> {

    List<Asset> findByAssignedFalse();
}
