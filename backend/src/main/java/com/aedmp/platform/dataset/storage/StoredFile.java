package com.aedmp.platform.dataset.storage;

public record StoredFile(String location, String checksumSha256, long sizeBytes) {
}
