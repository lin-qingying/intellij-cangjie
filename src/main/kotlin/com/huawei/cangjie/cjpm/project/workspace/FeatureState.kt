package com.huawei.cangjie.cjpm.project.workspace


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
