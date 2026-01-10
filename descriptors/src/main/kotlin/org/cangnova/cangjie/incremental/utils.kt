/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.incremental

import org.cangnova.cangjie.descriptors.InheritableDescriptor
import org.cangnova.cangjie.descriptors.PackageFragmentDescriptor

import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.LookupTracker
import org.cangnova.cangjie.incremental.components.Position
import org.cangnova.cangjie.incremental.components.ScopeKind
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.DescriptorUtils


fun LookupTracker.record(from: LookupLocation, scopeOwner: PackageFragmentDescriptor, name: Name) {
    recordPackageLookup(from, scopeOwner.fqName.asString(), name.asString())
}


// These methods are called many times, please pay attention to performance here

fun LookupTracker.record(from: LookupLocation, scopeOwner: InheritableDescriptor, name: Name) {
    if (this === LookupTracker.DO_NOTHING) return
    val location = from.location ?: return
    val position = if (requiresPosition) location.position else Position.NO_POSITION
    record(location.filePath, position, DescriptorUtils.getFqName(scopeOwner).asString(), ScopeKind.CLASSIFIER, name.asString())
}
fun LookupTracker.recordPackageLookup(from: LookupLocation, packageFqName: String, name: String) {
    if (this === LookupTracker.DO_NOTHING) return
    val location = from.location ?: return
    val position = if (requiresPosition) location.position else Position.NO_POSITION
    record(location.filePath, position, packageFqName, ScopeKind.PACKAGE, name)
}
