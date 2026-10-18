package com.knk.Storage_Service.controller;

import com.knk.Storage_Service.service.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/internal/objects")
public class StorageController {
    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }
    @PutMapping(value = "/{id}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> put(@PathVariable UUID id, @RequestPart("file")MultipartFile file) throws IOException{
        storageService.put(id,file);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}")
    public ResponseEntity<Resource> get(@PathVariable UUID id) throws IOException {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(storageService.get(id));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) throws IOException {
        storageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
