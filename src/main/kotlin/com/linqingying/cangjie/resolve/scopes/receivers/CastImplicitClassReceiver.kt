package com.linqingying.cangjie.resolve.scopes.receivers

import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.types.CangJieType


// ClassReceiver cast to given target type
// NB: equals / hashCode are inherited from ImplicitClassReceiver (as designed)
class CastImplicitClassReceiver(originalDescriptor: ClassDescriptor, val targetType: CangJieType) : ImplicitClassReceiver(originalDescriptor)
