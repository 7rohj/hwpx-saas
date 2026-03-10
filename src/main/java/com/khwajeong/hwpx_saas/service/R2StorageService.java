package com.khwajeong.hwpx_saas.service;

import java.io.ByteArrayInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
public class R2StorageService {

    private final S3Client s3Client;

    @Value("${r2.bucket-name}")
    private String bucketName;

    public R2StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadHwpx(byte[] fileBytes, String objectKey) {
        //System.out.println("[R2] bucketName = [" + safeBucketName + "]");
        //System.out.println("[R2] objectKey = [" + safeObjectKey + "]");

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType("application/octet-stream")
                .build();

        s3Client.putObject(
                request,
                RequestBody.fromInputStream(new ByteArrayInputStream(fileBytes), fileBytes.length)
        );

        return objectKey;
    }

    public byte[] downloadFile(String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
        return response.asByteArray();
    }

    public boolean exists(String objectKey) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.getObjectAsBytes(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void testConnection() {
        System.out.println("[R2] listBuckets start");
        s3Client.listBuckets().buckets().forEach(b ->
                System.out.println("[R2] bucket from account = " + b.name())
        );
    }
}