package com.huawei.cangjie.resolve.deprecation

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.descriptors.annotations.AnnotationDescriptor

@DefaultImplementation(DeprecationSettings.Default::class)
interface DeprecationSettings {
    fun propagatedToOverrides(deprecationAnnotation: AnnotationDescriptor): Boolean

    object Default : DeprecationSettings {
        override fun propagatedToOverrides(deprecationAnnotation: AnnotationDescriptor) = true
    }
}
