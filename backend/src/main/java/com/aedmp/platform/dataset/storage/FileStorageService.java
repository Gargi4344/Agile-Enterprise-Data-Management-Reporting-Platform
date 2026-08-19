package com.aedmp.platform.dataset.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstraction over where dataset version files live. Local disk in dev/CI, private S3 in AWS -
 * callers never branch on storage mode; only the two implementations do.
 */
public interface FileStorageService {

    StoredFile store(String datasetId, String versionKey, String originalFilename, InputStream content) throws IOException;

    InputStream load(String location) throws IOException;
}
