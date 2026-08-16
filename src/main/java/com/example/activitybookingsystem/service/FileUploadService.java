package com.example.activitybookingsystem.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    record UploadResult(String objectName, String url) {
    }

    UploadResult uploadCheckImage(MultipartFile file);

    String getCheckImagePresignedUrl(String objectName);
}