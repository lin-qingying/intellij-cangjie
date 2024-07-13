package com.huawei.cangjie.idea.configuration

import com.intellij.openapi.extensions.ExtensionPointName


enum class ConfigureCangJieStatus {
    /** 使用此配置器正确配置了CangJie。 */
    CONFIGURED,

    /** 配置器不适用于当前项目类型。 */
    NON_APPLICABLE,

    /** 该配置器适用于当前项目类型，可以自动配置CangJie */
    CAN_BE_CONFIGURED,

    /**
     * 配置器适用于当前项目类型且未配置CangJie，
     * 但项目的状态不允许自动配置CangJie。
     */
    BROKEN
}

interface CangJieProjectConfigurator {



    companion object {
        val EP_NAME = ExtensionPointName.create<CangJieProjectConfigurator>("com.huawei.cangjie.projectConfigurator")
    }
}
