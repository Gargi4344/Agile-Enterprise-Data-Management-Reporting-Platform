package com.aedmp.platform.dataset.storage;

import com.aedmp.platform.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Private-bucket storage for AWS deployments (spec section 13: "Dataset files -> Private S3").
 * Selected via aedmp.storage.mode=s3; not exercised in local/dev/CI, where LocalFileStorageService
 * is used instead. Bucket access must be private with IAM-role based access, never public ACLs.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aedmp.storage.mode", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    @Override
    public StoredFile store(String datasetId, String versionKey, String originalFilename, InputStream content) throws IOException {
        byte[] bytes = content.readAllBytes();
        String key = "datasets/%s/%s-%s".formatted(datasetId, versionKey, sanitize(originalFilename));

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(storageProperties.s3Bucket())
                .key(key)
                .serverSideEncryption(ServerSideEncryption.AES256)
                .contentType("text/csv")
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(bytes));

        return new StoredFile("s3://" + storageProperties.s3Bucket() + "/" + key, sha256(bytes), bytes.length);
    }

    @Override
    public InputStream load(String location) throws IOException {
        String withoutScheme = location.substring("s3://".length());
        int slash = withoutScheme.indexOf('/');
        String bucket = withoutScheme.substring(0, slash);
        String key = withoutScheme.substring(slash + 1);
        return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    private String sanitize(String filename) {
        return filename == null ? "upload" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
