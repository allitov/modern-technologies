package io.allitov.cadr.service;

import io.allitov.cadr.domain.Asset;
import io.allitov.cadr.domain.DomainError;
import io.allitov.cadr.repository.AssetRepository;
import io.allitov.cadr.repository.AssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Сервис работы с техникой.
 */
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssignmentRepository assignmentRepository;

    private static final Supplier<DomainError> ASSET_NOT_FOUND =
            () -> new DomainError("Техника не найдена");
    private static final Supplier<DomainError> CANNOT_DELETE_ASSIGNED_ASSET =
            () -> new DomainError("Нельзя удалить выданную технику — сначала примите возврат");

    private static Supplier<DomainError> inventoryNumberAlreadyExists(String inv) {
        return () -> new DomainError("Инвентарный номер %s уже существует".formatted(inv));
    }

    @Transactional(readOnly = true)
    public List<AssetRow> assets() {
        Map<Long, String> holders = assignmentRepository.findByReturnedAtIsNull().stream()
                .collect(Collectors.toMap(
                        a -> a.getAsset().getId(),
                        a -> a.getEmployee().getFullName(),
                        (first, _) -> first));

        return assetRepository.findAll().stream()
                .map(asset -> new AssetRow(asset, holders.getOrDefault(asset.getId(), "")))
                .toList();
    }

    @Transactional
    public void addAsset(String inv, String model, String category) {
        if (assetRepository.existsByInventoryNumber(inv)) {
            throw inventoryNumberAlreadyExists(inv).get();
        }
        Asset a = new Asset();
        a.setInventoryNumber(inv);
        a.setModel(model);
        a.setCategory(category);
        a.setAssigned(false);
        assetRepository.save(a);
    }

    @Transactional(readOnly = true)
    public List<Asset> freeAssets() {
        return assetRepository.findByAssignedFalse();
    }

    @Transactional
    public void duplicateAsset(long sourceId, String newInv) {
        Asset source = assetRepository.findById(sourceId)
                .orElseThrow(ASSET_NOT_FOUND);
        if (assetRepository.existsByInventoryNumber(newInv)) {
            throw inventoryNumberAlreadyExists(newInv).get();
        }
        Asset copy = new Asset();
        copy.setInventoryNumber(newInv);
        copy.setModel(source.getModel());
        copy.setCategory(source.getCategory());
        copy.setAssigned(false);
        assetRepository.save(copy);
    }

    @Transactional(readOnly = true)
    public List<Asset> searchAssets(String category, boolean onlyFree, String text) {
        Specification<Asset> spec = (_, _, cb) -> cb.conjunction();

        if (category != null && !category.isEmpty()) {
            spec = spec.and((root, _, cb) -> cb.equal(root.get("category"), category));
        }

        if (onlyFree) {
            spec = spec.and((root, _, cb) -> cb.isFalse(root.get("assigned")));
        }

        if (text != null && !text.isEmpty()) {
            String pattern = "%" + text + "%";
            spec = spec.and((root, _, cb) -> cb.or(
                    cb.like(root.get("model"), pattern),
                    cb.like(root.get("inventoryNumber"), pattern)));
        }

        return assetRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public PagedAssets listAssetsPaged(int page, int pageSize, String statusFilter) {
        Specification<Asset> spec = (_, _, cb) -> cb.conjunction();

        if ("В стойке".equals(statusFilter)) {
            spec = spec.and((root, _, cb) -> cb.isFalse(root.get("assigned")));
        } else if ("На съёмке".equals(statusFilter)) {
            spec = spec.and((root, _, cb) -> cb.isTrue(root.get("assigned")));
        }

        List<Asset> all = assetRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));

        int offset = page * pageSize;
        int end = Math.min(offset + pageSize, all.size());

        List<AssetRow> items = new ArrayList<>();
        for (int i = offset; i < end; i++) {
            Asset asset = all.get(i);
            String holder = "";
            if (asset.isAssigned()) {
                holder = assignmentRepository.findByAssetIdAndReturnedAtIsNull(asset.getId())
                        .map(asg -> asg.getEmployee().getFullName())
                        .orElse("");
            }
            items.add(new AssetRow(asset, holder));
        }

        return new PagedAssets(items, offset + pageSize < all.size());
    }

    @Transactional
    public void deleteAssetCascade(long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(ASSET_NOT_FOUND);
        if (asset.isAssigned()) {
            throw CANNOT_DELETE_ASSIGNED_ASSET.get();
        }
        assignmentRepository.deleteByAssetId(assetId);
        assetRepository.delete(asset);
    }
}
