package com.ecommerce.file.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    String upload(MultipartFile file);
    void delete(String objectName);
    String getUrl(String objectName);
}
