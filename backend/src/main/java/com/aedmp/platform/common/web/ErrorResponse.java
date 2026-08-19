package com.aedmp.platform.common.web;

import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        String requestId,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(String code, String message, String requestId) {
        return new ErrorResponse(code, message, requestId, null);
    }

    public static ErrorResponse of(String code, String message, String requestId, List<FieldError> fieldErrors) {
        return new ErrorResponse(code, message, requestId, fieldErrors);
    }
}
