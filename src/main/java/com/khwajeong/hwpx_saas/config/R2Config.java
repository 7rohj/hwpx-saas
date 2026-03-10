package com.khwajeong.hwpx_saas.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class R2Config {

    @Value("${r2.account-id}")
    private String accountId;

    @Value("${r2.access-key-id}")
    private String accessKeyId;

    @Value("${r2.secret-access-key}")
    private String secretAccessKey;

    @Bean
    public S3Client r2S3Client() {
        String endpoint = "https://" + accountId.trim() + ".r2.cloudflarestorage.com";

        System.out.println("[R2] endpoint = " + endpoint);
        System.out.println("[R2] accessKey length = " + accessKeyId.trim().length());
        System.out.println("[R2] secretKey length = " + secretAccessKey.trim().length());

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                accessKeyId.trim(),
                secretAccessKey.trim()
        );

        return S3Client.builder()
                .httpClientBuilder(
                        ApacheHttpClient.builder()
                                .connectionTimeout(Duration.ofSeconds(10))
                                .socketTimeout(Duration.ofSeconds(30))
                )
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .chunkedEncodingEnabled(false)
                                .build()
                )
                .build();
    }
}