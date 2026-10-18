package com.drive.Drive_Service.repository;

import com.drive.Drive_Service.model.DriveItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<DriveItem, UUID> {

    List<DriveItem> findByParentIdAndStatusOrderByNameAsc(UUID parentId, DriveItem.Status status);

    List<DriveItem> findByParentIdIsNullAndStatusOrderByNameAsc(DriveItem.Status status);

    boolean existsByParentId(UUID parentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from DriveItem i where i.id = :id")
    Optional<DriveItem> lockById(@Param("id") UUID id);
}