package com.ecommerce.file.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.file.service.FileService;
import org.springframework.web.bind.annotation.*;
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
}
