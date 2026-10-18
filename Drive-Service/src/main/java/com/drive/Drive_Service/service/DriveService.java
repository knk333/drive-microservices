package com.drive.Drive_Service.service;

import com.drive.Drive_Service.client.StorageClient;
import com.drive.Drive_Service.dto.FileDownload;
import com.drive.Drive_Service.model.DriveItem;
import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class DriveService {
    private static final Logger log = LoggerFactory.getLogger(DriveService.class);

    private final StorageClient storageClient;
    private final ItemService itemService;

    public DriveService(ItemService itemService, StorageClient storageClient) {
        this.itemService = itemService;
        this.storageClient = storageClient;
    }

    public DriveItem createFolder(String name, UUID parentId) {
        return itemService.create(name, parentId, DriveItem.Kind.FOLDER, 0, null);
    }

    public List<DriveItem> list(UUID id) {
        return itemService.list(id);
    }

    public DriveItem upload(MultipartFile file, UUID parentId) {
        DriveItem item = itemService.create(file.getOriginalFilename(), parentId,
                DriveItem.Kind.FILE, file.getSize(), file.getContentType());
        try {
            storageClient.put(item.getId(), file);
            return itemService.ready(item.getId());
        } catch (RuntimeException e) {
            try {
                storageClient.delete(item.getId());
                itemService.remove(item.getId());
            } catch (RuntimeException ex) {
                log.warn("pending item {} needs cleanup: {}", item.getId(), ex.getMessage());
            }
            throw e;
        }
    }

    public FileDownload download(UUID id) {
        DriveItem item = itemService.get(id);
        if (item.getKind() != DriveItem.Kind.FILE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item has no content");
        }
        Response response = storageClient.get(id);
        return new FileDownload(item.getName(), item.getSize(), response);
    }

    public void delete(UUID id) {
        DriveItem item = itemService.delete(id);          // mark DELETING
        if (item.getKind() == DriveItem.Kind.FILE) {
            storageClient.delete(id);                     // delete the blob first
        }
        itemService.remove(id);                           // then remove the row
    }
}