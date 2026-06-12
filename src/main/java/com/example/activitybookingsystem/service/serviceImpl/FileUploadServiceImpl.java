package com.example.activitybookingsystem.service.serviceImpl;

import com.example.activitybookingsystem.common.exception.BusinessException;
import com.example.activitybookingsystem.config.MinioProperties;
import com.example.activitybookingsystem.service.FileUploadService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final Set<String> IMAGE_SUFFIXES = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp");

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

        if (!isImageFile(file, suffix)) {
            throw new BusinessException("只能上传图片文件！");
        }

        //minio对象名
        String objectName = "check-in/" + LocalDate.now() + "/" + UUID.randomUUID() + suffix;

        try{
            // bucket 不存在时自动创建，避免首次上传因为未建桶失败。
            ensureBucketExists();
            //设置minio对象
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return minioProperties.getEndpoint() + "/" + minioProperties.getBucketName() + "/" + objectName;
        }catch (Exception e){
            log.error("上传打卡图片失败，bucket={}，object={}", minioProperties.getBucketName(), objectName, e);
            throw new BusinessException("图片上传失败，请检查 MinIO 服务和存储桶配置！");
        }
    }

    private boolean isImageFile(MultipartFile file, String suffix) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        return IMAGE_SUFFIXES.contains(suffix.toLowerCase(Locale.ROOT));
    }

    private void ensureBucketExists() throws Exception {
        String bucketName = minioProperties.getBucketName();
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        }
    }
}
