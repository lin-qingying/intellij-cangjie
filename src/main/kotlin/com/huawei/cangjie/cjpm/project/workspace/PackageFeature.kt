package com.huawei.cangjie.cjpm.project.workspace

import FeatureName
import com.huawei.cangjie.utils.PresentableNodeData

data class PackageFeature(val pkg: CjpmWorkspace.Package, val name: FeatureName) : PresentableNodeData {
    override val text: String
        get() = "${pkg.name}/$name"

    override fun toString(): String = text
}

enum class FeatureState {
    Enabled,
    Disabled;

    val isEnabled: Boolean
        get() = when (this) {
            Enabled -> true
            Disabled -> false
        }

    operator fun not(): FeatureState = when (this) {
        Enabled -> Disabled
        Disabled -> Enabled
    }
}
