package com.huawei.cangjie.resolve.calls

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations

object DslMarkerUtils{

    object FunctionTypeAnnotationsKey : CallableDescriptor.UserDataKey<Annotations>

}