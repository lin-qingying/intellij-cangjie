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

package org.cangnova.cangjie.configurable.services

import org.cangnova.cangjie.configurable.services.Feature.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

enum class Feature {
    SEMANTIC_TOKENS, // 语义标记
    AUTO_COMPLETE,    // 自动补全
    SIGNATURE_HELP,   // 签名帮助
    HOVER_INFO,       // 悬停信息
    FORMATTING,       // 格式化
    DIAGNOSTICS,      // 诊断信息
    GO_TO_DECLARATION,// 转到声明
    REFERENCES,     // 引用解析，包含转到声明与查找用法
    FIND_USAGES,     // 查找用法
    QUICK_FIX, // 快速修复
    LIBRARY_DIAGNOSTICS, // 库诊断信息
}

enum class LanugageServerType {
    AST_ANALYZER, // AST分析器
    LSP_SERVER // LSP服务器
}

@State(name = "CangJieLanguageServerServices", storages = [Storage("cangjie.language.server.xml")])
@Service(Service.Level.APP)
class CangJieLanguageServerServices : PersistentStateComponent<CangJieLanguageServerServices> {
    companion object {
        fun getInstance(): CangJieLanguageServerServices {
            return ApplicationManager.getApplication().getService(CangJieLanguageServerServices::class.java)
        }
    }

    interface Config {
        //        更改主开关
        fun setMainSwitch(enabled: Boolean)

        fun enableFeature(feature: Feature)
        fun disableFeature(feature: Feature)
        fun isFeatureEnabled(feature: Feature): Boolean
        fun isAllFeaturesEnabled(): Boolean
    }

    data class AstAnalyzerConfig
        (

        var enabled: Boolean = false, // 主开关
        var autoComplete: Boolean = true, // 自动补全
        var signatureHelp: Boolean = true, // 签名帮助
        var hoverInfo: Boolean = true, // 悬停信息
        var formatting: Boolean = true, // 格式化
        var diagnostics: Boolean = true, // 诊断信息
        var references: Boolean = true, // 引用解析
        var quickFix: Boolean = true, // 快速修复
        var libraryDiagnostics: Boolean = false, // 库诊断信息

    ) : Config {
        override fun setMainSwitch(enabled: Boolean) {
            this.enabled = enabled
        }

        // 启用指定功能（不验证主开关）
        override fun enableFeature(feature: Feature) {
            when (feature) {
                QUICK_FIX -> quickFix = true
                AUTO_COMPLETE -> autoComplete = true
                SIGNATURE_HELP -> signatureHelp = true
                HOVER_INFO -> hoverInfo = true
                FORMATTING -> formatting = true
                DIAGNOSTICS -> diagnostics = true
                REFERENCES -> references = true
                LIBRARY_DIAGNOSTICS -> libraryDiagnostics = true

                else -> {}
            }
        }

        // 禁用指定功能（不验证主开关）
        override fun disableFeature(feature: Feature) {
            when (feature) {

                QUICK_FIX -> quickFix = false
                AUTO_COMPLETE -> autoComplete = false
                SIGNATURE_HELP -> signatureHelp = false
                HOVER_INFO -> hoverInfo = false
                FORMATTING -> formatting = false
                DIAGNOSTICS -> diagnostics = false
                REFERENCES -> references = false
                LIBRARY_DIAGNOSTICS -> libraryDiagnostics = false
                else -> {}
            }
        }

        // 检查指定功能是否启用
        override fun isFeatureEnabled(feature: Feature): Boolean {
            if (!enabled) return false // 如果主开关被禁用，则返回 false
            return when (feature) {

                AUTO_COMPLETE -> autoComplete
                SIGNATURE_HELP -> signatureHelp
                HOVER_INFO -> hoverInfo
                FORMATTING -> formatting
                DIAGNOSTICS -> diagnostics
                REFERENCES -> references
                QUICK_FIX -> quickFix

                LIBRARY_DIAGNOSTICS -> libraryDiagnostics
                else -> {
                    false
                }
            }
        }

        // 检查是否所有功能都启用
        override fun isAllFeaturesEnabled(): Boolean {
            return enabled && autoComplete && signatureHelp && hoverInfo && formatting && diagnostics && references
        }
    }

    data class LspConfig(
        var enabled: Boolean = true, // 主开关
        var autoComplete: Boolean = true, // 自动补全
        var signatureHelp: Boolean = false, // 签名帮助
        var hoverInfo: Boolean = true, // 悬停信息
        var formatting: Boolean = false, // 格式化
        var diagnostics: Boolean = true, // 诊断信息
        var goToDeclaration: Boolean = true, // 转到声明
        var findUsages: Boolean = true, // 查找用法
        var semanticTokens: Boolean = true, //语义标记
    ) : Config {
        override fun setMainSwitch(enabled: Boolean) {
            this.enabled = enabled


        }

        // 启用指定功能（不验证主开关）
        override fun enableFeature(feature: Feature) {
            when (feature) {
                SEMANTIC_TOKENS -> semanticTokens = true
                AUTO_COMPLETE -> autoComplete = true
                SIGNATURE_HELP -> signatureHelp = true
                HOVER_INFO -> hoverInfo = true
                FORMATTING -> formatting = true
                DIAGNOSTICS -> diagnostics = true
                GO_TO_DECLARATION -> goToDeclaration = true
                FIND_USAGES -> findUsages = true
                else -> {}
            }
        }

        // 禁用指定功能（不验证主开关）
        override fun disableFeature(feature: Feature) {
            when (feature) {
                SEMANTIC_TOKENS -> semanticTokens = false

                AUTO_COMPLETE -> autoComplete = false
                SIGNATURE_HELP -> signatureHelp = false
                HOVER_INFO -> hoverInfo = false
                FORMATTING -> formatting = false
                DIAGNOSTICS -> diagnostics = false
                GO_TO_DECLARATION -> goToDeclaration = false
                FIND_USAGES -> findUsages = false
                else -> {}
            }
        }

        // 检查指定功能是否启用
        override fun isFeatureEnabled(feature: Feature): Boolean {
            if (!enabled) return false // 如果主开关被禁用，则返回 false
            return when (feature) {
                SEMANTIC_TOKENS -> semanticTokens

                AUTO_COMPLETE -> autoComplete
                SIGNATURE_HELP -> signatureHelp
                HOVER_INFO -> hoverInfo
                FORMATTING -> formatting
                DIAGNOSTICS -> diagnostics
                GO_TO_DECLARATION -> goToDeclaration
                FIND_USAGES -> findUsages
                else -> {
                    false
                }
            }
        }

        // 检查是否所有功能都启用
        override fun isAllFeaturesEnabled(): Boolean {
            return enabled &&
                    autoComplete &&
                    signatureHelp &&
                    hoverInfo &&
                    formatting &&
                    diagnostics &&
                    goToDeclaration &&
                    findUsages &&
                    semanticTokens
        }
    }

    var lspConfig = LspConfig()
    var astConfig = AstAnalyzerConfig()

    fun getConfig(type: LanugageServerType): Config {
        return when (type) {
            LanugageServerType.LSP_SERVER -> lspConfig
            LanugageServerType.AST_ANALYZER -> astConfig
        }
    }

    //    启用
    fun enableFeature(type: LanugageServerType, feature: Feature) {
        getConfig(type).enableFeature(feature)
    }

    fun setMainSwitch(type: LanugageServerType, enabled: Boolean) {
        getConfig(type).setMainSwitch(enabled)
    }

    //    禁用
    fun disableFeature(type: LanugageServerType, feature: Feature) {
        getConfig(type).disableFeature(feature)
    }

    //    检查是否启用
    fun isFeatureEnabled(type: LanugageServerType, feature: Feature): Boolean {
        return getConfig(type).isFeatureEnabled(feature)
    }

    //    检查是否所有功能都启用
    fun isAllFeaturesEnabled(type: LanugageServerType): Boolean {
        return getConfig(type).isAllFeaturesEnabled()
    }


    override fun getState(): CangJieLanguageServerServices {
        return this
    }

    override fun loadState(state: CangJieLanguageServerServices) {
        XmlSerializerUtil.copyBean(state, this)

    }

}
