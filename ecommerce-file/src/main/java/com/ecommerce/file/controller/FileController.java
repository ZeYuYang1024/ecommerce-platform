package com.ecommerce.file.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.file.service.FileService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.upload(file));
    }

    @DeleteMapping("/{objectName}")
    public Result<Void> delete(@PathVariable String objectName) {
        fileService.delete(objectName);
        return Result.ok();
    }

    @GetMapping("/{objectName}/url")
    public Result<String> getUrl(@PathVariable String objectName) {
        return Result.ok(fileService.getUrl(objectName));
    }

    @GetMapping("/{objectName}/content")
    public ResponseEntity<InputStreamResource> getImage(@PathVariable String objectName) {
        var image = fileService.openImage(objectName);
        var response = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic());
        if (image.size() >= 0) response.contentLength(image.size());
        return response.body(new InputStreamResource(image.stream()));
    }
}
