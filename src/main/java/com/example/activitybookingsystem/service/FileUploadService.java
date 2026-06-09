package com.example.activitybookingsystem.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    String uploadCheckImage(MultipartFile file);
}
