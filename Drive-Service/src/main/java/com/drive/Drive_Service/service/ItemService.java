package com.drive.Drive_Service.service;

import com.drive.Drive_Service.model.DriveItem;
import com.drive.Drive_Service.repository.ItemRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ItemService {
    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional
    public DriveItem create(String name, UUID parentId, DriveItem.Kind kind, long size, String mediaType) {
        if (name == null || name.isBlank() || name.length() > 255 || name.equals(".")
                || name.equals("..") || name.contains("/") || name.contains("\\")
                || name.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "use proper naming");
        }
        if (parentId != null) {
            DriveItem parent = itemRepository.lockById(parentId).orElseThrow(ItemService::missing);
            if (parent.getKind() != DriveItem.Kind.FOLDER || parent.getStatus() != DriveItem.Status.READY) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "parent must be an available folder");
            }
        }
        return itemRepository.saveAndFlush(new DriveItem(name, parentId, kind, size, mediaType));
    }

    @Transactional
    public List<DriveItem> list(UUID id) {
        if (id == null) {
            return itemRepository.findByParentIdIsNullAndStatusOrderByNameAsc(DriveItem.Status.READY);
        }
        DriveItem item = get(id);
        if (item.getKind() != DriveItem.Kind.FOLDER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "parent must be a folder");
        }
        return itemRepository.findByParentIdAndStatusOrderByNameAsc(id, DriveItem.Status.READY);
    }

    @Transactional
    public DriveItem get(UUID id) {
        DriveItem item = itemRepository.findById(id).orElseThrow(ItemService::missing);
        if (item.getStatus() != DriveItem.Status.READY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "item is not ready");
        }
        return item;
    }

    @Transactional
    public DriveItem ready(UUID id) {
        DriveItem item = itemRepository.findById(id).orElseThrow(ItemService::missing);
        if (item.getStatus() != DriveItem.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "item is no longer pending");
        }
        item.setStatus(DriveItem.Status.READY);
        return item;
    }

    /** Marks the item as DELETING. Safe to call again if an earlier delete failed halfway. */
    @Transactional
    public DriveItem delete(UUID id) {
        DriveItem item = itemRepository.lockById(id).orElseThrow(ItemService::missing);
        if (item.getStatus() == DriveItem.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "item upload is still pending");
        }
        if (item.getStatus() == DriveItem.Status.DELETING) {
            return item;
        }
        if (item.getKind() == DriveItem.Kind.FOLDER && itemRepository.existsByParentId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "folder is not empty");
        }
        item.setStatus(DriveItem.Status.DELETING);
        return item;
    }

    @Transactional
    public void remove(UUID id) {
        itemRepository.deleteById(id);
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found");
    }
}