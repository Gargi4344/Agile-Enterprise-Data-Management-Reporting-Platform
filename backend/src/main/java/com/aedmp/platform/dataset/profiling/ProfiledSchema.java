package com.aedmp.platform.dataset.profiling;

import java.util.List;

public record ProfiledSchema(long rowCount, List<ProfiledColumn> columns, List<List<String>> sampleRows) {
}
