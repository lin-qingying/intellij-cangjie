package com.linqingying.cangjie.resolve.calls

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.annotations.Annotations

object DslMarkerUtils{

    object FunctionTypeAnnotationsKey : CallableDescriptor.UserDataKey<Annotations>

}
