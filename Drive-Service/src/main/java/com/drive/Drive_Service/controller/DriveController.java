package com.drive.Drive_Service.controller;

import com.drive.Drive_Service.dto.FileDownload;
import com.drive.Drive_Service.dto.FolderRequest;
import com.drive.Drive_Service.model.DriveItem;
import com.drive.Drive_Service.service.DriveService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DriveController {
    private final DriveService driveService;

    public DriveController(DriveService driveService) {
        this.driveService = driveService;
    }

    @PostMapping("/folders")
    @ResponseStatus(HttpStatus.CREATED)
    public DriveItem folder(@Valid @RequestBody FolderRequest request) {
        return driveService.createFolder(request.name(), request.parentId());
    }

    @GetMapping("/items")
    public List<DriveItem> list(@RequestParam(required = false) UUID id) {
        return driveService.list(id);
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DriveItem upload(@RequestPart("file") MultipartFile file,
                            @RequestParam(required = false) UUID parentId) {
        return driveService.upload(file, parentId);
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable UUID id) {
        FileDownload file = driveService.download(id);
        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename(file.name(), StandardCharsets.UTF_8).build().toString())
                    .header("X-Content-Type-Options", "nosniff")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(file.size())
                    .body(file::writeTo);
        } catch (RuntimeException e) {
            file.close();
            throw e;
        }
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        driveService.delete(id);
    }
}