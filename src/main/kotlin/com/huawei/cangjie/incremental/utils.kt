package com.huawei.cangjie.incremental

import com.huawei.cangjie.descriptors.PackageFragmentDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.incremental.components.LookupTracker
import com.huawei.cangjie.incremental.components.Position
import com.huawei.cangjie.incremental.components.ScopeKind
import com.huawei.cangjie.name.Name


fun LookupTracker.record(from: LookupLocation, scopeOwner: PackageFragmentDescriptor, name: Name) {
    recordPackageLookup(from, scopeOwner.fqName.asString(), name.asString())
}
fun LookupTracker.recordPackageLookup(from: LookupLocation, packageFqName: String, name: String) {
    if (this === LookupTracker.DO_NOTHING) return
    val location = from.location ?: return
    val position = if (requiresPosition) location.position else Position.NO_POSITION
    record(location.filePath, position, packageFqName, ScopeKind.PACKAGE, name)
}