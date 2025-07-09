package com.playdata.messageservice.common.configs;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class AwsS3Config {

    private S3Client s3Client;

    @Value("${spring.cloud.aws.credentials.accessKey}")
    private String accessKey;
    @Value("${spring.cloud.aws.credentials.secretKey}")
    private String secretKey;
    @Value("${spring.cloud.aws.region.static}")
    private String region;
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @PostConstruct
    private void initializeAmazonS3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    public String uploadToS3Bucket(byte[] uploadFile, String fileName) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(uploadFile));

        return s3Client.utilities()
                .getUrl(b -> b.bucket(bucketName).key(fileName))
                .toString();
    }

    public void deleteFromS3Bucket(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        String decodingKey = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8.toString());
        String key = decodingKey.substring(1);

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }
}
