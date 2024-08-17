package com.huawei.cangjie.types.error

class ThrowingScope(kind: ErrorScopeKind, vararg formatParams: String) : ErrorScope(kind, *formatParams)
