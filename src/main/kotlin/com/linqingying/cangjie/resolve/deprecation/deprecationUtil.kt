package com.linqingying.cangjie.resolve.deprecation

import com.linqingying.cangjie.config.ApiVersion
import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptor
import com.linqingying.cangjie.resolve.argumentValue
import com.linqingying.cangjie.resolve.constants.StringValue

@DefaultImplementation(DeprecationSettings.Default::class)
interface DeprecationSettings {
    fun propagatedToOverrides(deprecationAnnotation: AnnotationDescriptor): Boolean

    object Default : DeprecationSettings {
        override fun propagatedToOverrides(deprecationAnnotation: AnnotationDescriptor) = true
    }
}
fun DescriptorBasedDeprecationInfo.deprecatedByAnnotationReplaceWithExpression(): String? = (this as? DeprecatedByAnnotation)?.replaceWithValue

fun computeLevelForDeprecatedSinceCangJie(annotation: AnnotationDescriptor, apiVersion: ApiVersion): DeprecationLevelValue? {
    val hiddenSince = annotation.getSinceVersion("hiddenSince")
    if (hiddenSince != null && apiVersion >= hiddenSince) return DeprecationLevelValue.HIDDEN

    val errorSince = annotation.getSinceVersion("errorSince")
    if (errorSince != null && apiVersion >= errorSince) return DeprecationLevelValue.ERROR

    val warningSince = annotation.getSinceVersion("warningSince")
    if (warningSince != null && apiVersion >= warningSince) return DeprecationLevelValue. WARNING

    return null
}
fun AnnotationDescriptor.getSinceVersion(name: String): ApiVersion? =
    (argumentValue(name) as? StringValue)?.value?.takeUnless(String::isEmpty)?.let(ApiVersion.Companion::parse)
