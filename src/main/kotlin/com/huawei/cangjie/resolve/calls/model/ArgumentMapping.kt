package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.types.ErrorUtils



fun ResolvedCall<*>.isReallySuccess(): Boolean = status.isSuccess && !ErrorUtils.isError(resultingDescriptor)
