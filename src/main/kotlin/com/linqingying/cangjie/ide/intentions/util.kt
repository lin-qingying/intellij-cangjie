package com.linqingying.cangjie.ide.intentions

import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.resolve.caches.resolveToCall


internal fun CjExpression.getCallableDescriptor() = resolveToCall()?.resultingDescriptor
