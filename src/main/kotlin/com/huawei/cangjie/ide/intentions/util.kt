package com.huawei.cangjie.ide.intentions

import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.resolve.caches.resolveToCall


internal fun CjExpression.getCallableDescriptor() = resolveToCall()?.resultingDescriptor
