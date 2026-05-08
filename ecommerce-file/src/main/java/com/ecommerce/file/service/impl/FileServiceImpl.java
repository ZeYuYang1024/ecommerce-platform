package com.ecommerce.file.service.impl;

import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.file.common.FileErrorCode;
import com.ecommerce.file.config.MinioConfig;
import com.ecommerce.file.service.FileService;
import io.minio.*;
import io.minio.http.Method;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class FileServiceImpl implements FileService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    private static final Set<String> ALLOWED_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/avif"
    ));

    public FileServiceImpl(MinioClient minioClient, MinioConfig minioConfig) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
    }

    @Override
    public String upload(MultipartFile file) {
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(FileErrorCode.FILE_SIZE_EXCEED);
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException(FileErrorCode.FILE_TYPE_UNSUPPORTED);
        }

        String ext = getExtension(file.getOriginalFilename());
        String objectName = SnowflakeUtils.nextIdStr() + ext;

        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return objectName;
        } catch (Exception e) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
        }
    }

    @Override
    public String getUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .method(Method.GET)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
        }
    }

    private void ensureBucket() throws Exception {
        boolean found = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(minioConfig.getBucket()).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.getBucket()).build());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}
