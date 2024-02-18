package com.huawei.cangjie.cjpm.project.model.impl

import java.nio.file.Path

abstract class UserDisabledFeatures{

abstract val pkgRootToDisabledFeatures: Map<PackageRoot, Set<FeatureName>>


fun isEmpty(): Boolean {
    return pkgRootToDisabledFeatures.isEmpty() || pkgRootToDisabledFeatures.values.all { it.isEmpty() }
}
    companion object{
        val EMPTY: UserDisabledFeatures = ImmutableUserDisabledFeatures(emptyMap())

    }
}
typealias PackageRoot = Path
typealias FeatureName = String

private class ImmutableUserDisabledFeatures(
    override val pkgRootToDisabledFeatures: Map<PackageRoot, Set<FeatureName>>
) : UserDisabledFeatures()