package com.huawei.cangjie.resolve.calls.context;

public enum CheckArgumentTypesMode {
    /**
     * Check value argument types for particular call.
     */
    CHECK_VALUE_ARGUMENTS,
    /**
     * Match callable reference type against expected (callable) type.
     */
    CHECK_CALLABLE_TYPE
}
