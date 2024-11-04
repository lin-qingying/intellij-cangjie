package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.resolve.getResolutionAnchorIfAny

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
fun ModuleDescriptor.findTypeAliasAcrossModuleDependencies(classId: ClassId): TypeAliasDescriptor? {
    // TODO what if typealias becomes a class / interface?
    return findClassifierAcrossModuleDependencies(classId) as? TypeAliasDescriptor
}
// Returns a mock class descriptor if no existing class is found.
// NB: the returned class has no type parameters and thus cannot be given arguments in types
fun ModuleDescriptor.findNonGenericClassAcrossDependencies(classId: ClassId, notFoundClasses: NotFoundClasses): ClassDescriptor {
    val existingClass = findClassAcrossModuleDependencies(classId)
    if (existingClass != null) return existingClass

    // Take a list of N zeros, where N is the number of class names in the given ClassId
    val typeParametersCount = generateSequence(classId, ClassId::outerClassId).map { 0 }.toList()

    return notFoundClasses.getClass(classId, typeParametersCount)
}
