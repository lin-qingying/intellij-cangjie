@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.base.analysisApiPlatform

import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings

/**
 * IDE 平台设置。
 *
 * IntelliJ 产品插件已经把库声明预索引成 stub，
 * 因而 IDE 模式下的 Analysis API / low-level API 必须与 source-like 测试宿主一致，
 * 统一从 stub declaration provider 恢复库符号，保证引用 target、导航与文档链路都能拿到 PSI 绑定。
 */
class CaIdePlatformSettings : CaPlatformSettings {
    override val deserializedDeclarationsOrigin: CaDeserializedDeclarationsOrigin
        get() = CaDeserializedDeclarationsOrigin.STUBS
}
