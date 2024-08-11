package com.huawei.cangjie.resolve.scopes.receivers

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.types.CangJieType


// ClassReceiver cast to given target type
// NB: equals / hashCode are inherited from ImplicitClassReceiver (as designed)
class CastImplicitClassReceiver(originalDescriptor: ClassDescriptor, val targetType: CangJieType) : ImplicitClassReceiver(originalDescriptor)
