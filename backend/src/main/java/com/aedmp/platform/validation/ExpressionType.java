package com.aedmp.platform.validation;

/** How {@link ValidationRule#getExpression()} should be interpreted by the rule engine. */
public enum ExpressionType {
    NOT_NULL,
    UNIQUE,
    REGEX,
    RANGE_MIN,
    RANGE_MAX,
    ENUM,
    TYPE_MATCH,
    COLUMN_PRESENT
}
