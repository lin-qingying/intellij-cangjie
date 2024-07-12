package com.huawei.cangjie.descriptors

import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.resolve.getResolutionAnchorIfAny

fun ModuleDescriptor.findClassifierAcrossModuleDependencies(classId: ClassId): ClassifierDescriptor? = withResolutionAnchor {
    val packageViewDescriptor = getPackage(classId.packageFqName)
    val segments = classId.relativeClassName.pathSegments()
    val topLevelClass = packageViewDescriptor.memberScope.getContributedClassifier(
        segments.first(),
        NoLookupLocation.FROM_DESERIALIZATION
    ) ?: return@withResolutionAnchor null
    var result = topLevelClass
    for (name in segments.subList(1, segments.size)) {
        if (result !is ClassDescriptor) return@withResolutionAnchor null
        result = result.unsubstitutedInnerClassesScope
            .getContributedClassifier(name, NoLookupLocation.FROM_DESERIALIZATION) as? ClassDescriptor
            ?: return@withResolutionAnchor null
    }
    return@withResolutionAnchor result
}

fun ModuleDescriptor.findClassAcrossModuleDependencies(classId: ClassId): ClassDescriptor? =
    findClassifierAcrossModuleDependencies(classId) as? ClassDescriptor
private inline fun ModuleDescriptor.withResolutionAnchor(
    crossinline doSearch: ModuleDescriptor.() -> ClassifierDescriptor?
): ClassifierDescriptor? {
    val anchor = getResolutionAnchorIfAny()
    return if (anchor == null) doSearch() else anchor.doSearch() ?: doSearch()
}
