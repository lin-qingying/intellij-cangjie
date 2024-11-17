/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

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
