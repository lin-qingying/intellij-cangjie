/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi.codeFragmentUtil

import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import com.intellij.openapi.util.Key
/**
 * 扩展函数：检查元素是否应在调试模式下抑制诊断
 *
 * ## 判断逻辑
 * 1. 如果元素本身是 CjFile，直接检查其 suppressDiagnosticsInDebugMode 属性
 * 2. 否则，查找包含该元素的文件，检查文件的属性
 *
 * ## 使用场景
 * 在生成诊断错误之前，检查是否应该抑制该错误：
 * ```kotlin
 * if (!element.suppressDiagnosticsInDebugMode()) {
 *     trace.report(WARNING.on(element))
 * }
 * ```
 *
 * @return true 表示应该抑制诊断，false 表示正常报告诊断
 */
fun CjElement.suppressDiagnosticsInDebugMode(): Boolean {
    return if (this is CjFile) {
        this.suppressDiagnosticsInDebugMode
    } else {
        val file = this.containingFile
        file is CjFile && file.suppressDiagnosticsInDebugMode
    }
}

var CjFile.suppressDiagnosticsInDebugMode: Boolean
    get() = when (this) {
        is CjCodeFragment -> true
        else -> getUserData(SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE) ?: false
    }
    set(skip) {
        putUserData(SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE, skip)
    }

val DEBUG_TYPE_REFERENCE_STRING: String = "DebugTypeCangJieRulezzzz"
val SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE: Key<Boolean> = Key.create<Boolean>("SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE")
