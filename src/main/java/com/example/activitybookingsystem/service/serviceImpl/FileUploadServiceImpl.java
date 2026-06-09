package com.example.activitybookingsystem.service.serviceImpl;

import com.example.activitybookingsystem.common.exception.BusinessException;
import com.example.activitybookingsystem.config.MinioProperties;
import com.example.activitybookingsystem.service.FileUploadService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public FileUploadServiceImpl(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @Override
    public String uploadCheckImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传图片不能为空！");
        }
        //文件原始名
        String originalFileName = file.getOriginalFilename();
        //获取文件后缀名
        String suffix = originalFileName != null && originalFileName.contains(".")
                ? originalFileName.substring(originalFileName.lastIndexOf("."))
                : ".jpg";
        //minio对象名
        String objectName = "check-in/" + LocalDate.now() + "/" + UUID.randomUUID() + suffix;

        try{
            //设置minio对象
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return minioProperties.getEndpoint() + "/" + minioProperties.getBucketName() + "/" + objectName;
        }catch (Exception e){
             throw new BusinessException("图片上传失败!");
        }
    }
}
