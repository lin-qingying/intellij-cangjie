package com.huawei.cangjie.cjpm.project.model.impl

import FeatureName
import PackageRoot
import com.huawei.cangjie.cjpm.project.workspace.FeatureState
import com.huawei.cangjie.cjpm.project.workspace.PackageFeature


abstract class UserDisabledFeatures {

    abstract val pkgRootToDisabledFeatures: Map<PackageRoot, Set<FeatureName>>

//    fun getDisabledFeatures(packages: Iterable<CjpmWorkspace.Package>): List<PackageFeature> {
//        return packages.flatMap { pkg ->
//            pkgRootToDisabledFeatures[pkg.rootDirectory]
//                ?.mapNotNull { name -> PackageFeature(pkg, name).takeIf { it in pkg.features } }
//                ?: emptyList()
//        }
//    }

    fun isEmpty(): Boolean {
        return pkgRootToDisabledFeatures.isEmpty() || pkgRootToDisabledFeatures.values.all { it.isEmpty() }
    }

    fun toMutable(): MutableUserDisabledFeatures = MutableUserDisabledFeatures(
        pkgRootToDisabledFeatures
            .mapValues { (_, v) -> v.toMutableSet() }
            .toMutableMap()
    )
//    fun retain(packages: Iterable<CjpmWorkspace.Package>): UserDisabledFeatures {
//        val newMap = EMPTY.toMutable()
//        for (disabledFeature in getDisabledFeatures(packages)) {
//            newMap.setFeatureState(disabledFeature, FeatureState.Disabled)
//        }
//        return newMap
//    }

    companion object {
        val EMPTY: UserDisabledFeatures = ImmutableUserDisabledFeatures(emptyMap())

    }
}


private class ImmutableUserDisabledFeatures(
    override val pkgRootToDisabledFeatures: Map<PackageRoot, Set<FeatureName>>
) : UserDisabledFeatures()

class MutableUserDisabledFeatures(
    override val pkgRootToDisabledFeatures: MutableMap<PackageRoot, MutableSet<FeatureName>>
) : UserDisabledFeatures() {

    fun setFeatureState(
        feature: PackageFeature,
        state: FeatureState
    ) {
        val packageRoot = feature.pkg.rootDirectory
        when (state) {
            FeatureState.Enabled -> {
                pkgRootToDisabledFeatures[packageRoot]?.remove(feature.name)
            }

            FeatureState.Disabled -> {
                pkgRootToDisabledFeatures.getOrPut(packageRoot) { hashSetOf() }
                    .add(feature.name)
            }
        }
    }
}
