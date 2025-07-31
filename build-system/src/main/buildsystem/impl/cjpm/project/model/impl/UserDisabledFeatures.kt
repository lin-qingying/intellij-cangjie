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

package cn.cangnova.cangjie.buildsystem.impl.cjpm.project.model.impl

import cn.cangnova.cangjie.cjpm.project.workspace.FeatureName
import cn.cangnova.cangjie.cjpm.project.workspace.FeatureState
import cn.cangnova.cangjie.cjpm.project.workspace.PackageFeature
import cn.cangnova.cangjie.cjpm.project.workspace.PackageRoot


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
