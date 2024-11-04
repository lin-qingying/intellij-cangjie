package com.linqingying.cangjie.incremental

import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.PackageFragmentDescriptor
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.incremental.components.LookupTracker
import com.linqingying.cangjie.incremental.components.Position
import com.linqingying.cangjie.incremental.components.ScopeKind
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.DescriptorUtils


fun LookupTracker.record(from: LookupLocation, scopeOwner: PackageFragmentDescriptor, name: Name) {
    recordPackageLookup(from, scopeOwner.fqName.asString(), name.asString())
}


// These methods are called many times, please pay attention to performance here

fun LookupTracker.record(from: LookupLocation, scopeOwner: ClassDescriptor, name: Name) {
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
