package com.aedmp.platform.dataset.storage;

import com.aedmp.platform.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aedmp.storage.mode", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final StorageProperties storageProperties;

    @Override
    public StoredFile store(String datasetId, String versionKey, String originalFilename, InputStream content) throws IOException {
        Path root = Path.of(storageProperties.localPath(), datasetId);
        Files.createDirectories(root);
        String safeName = sanitize(originalFilename);
        Path target = root.resolve(versionKey + "-" + safeName);

        byte[] bytes = content.readAllBytes();
        Files.write(target, bytes);
        String checksum = sha256(bytes);
        return new StoredFile("local://" + target.toAbsolutePath(), checksum, bytes.length);
    }

    @Override
    public InputStream load(String location) throws IOException {
        String path = location.startsWith("local://") ? location.substring("local://".length()) : location;
        return new FileInputStream(path);
    }

    private String sanitize(String filename) {
        String base = filename == null ? "upload" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return base.isBlank() ? "upload-" + Instant.now().toEpochMilli() : base;
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
