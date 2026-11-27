package com.ecommerce.file.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileService {
    String upload(MultipartFile file);
    void delete(String objectName);
    String getUrl(String objectName);
    Image openImage(String objectName);

    record Image(InputStream stream, String contentType, long size) {}
}
