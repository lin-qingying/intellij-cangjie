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
 */

package org.cangnova.cangjie.macro.psi

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange

/**
 * 宏展开的源位置映射信息
 *
 * 附加到展开后 PSI 节点的 UserData 上，用于追踪展开代码的来源。
 * 通过 [com.intellij.psi.PsiElement.putUserData]/[com.intellij.psi.PsiElement.getUserData] 使用。
 *
 * @param originalFilePath 原始源文件路径
 * @param originalTextRange 宏调用在原始源文件中的文本范围
 * @param macroName 宏名称（如 `@Derive`）
 * @param expansionDepth 展开深度（嵌套宏时递增）
 * @param parent 外层宏展开信息（嵌套宏时指向父级）
 */
data class MacroSourceInfo(
    val originalFilePath: String,
    val originalTextRange: TextRange,
    val macroName: String,
    val expansionDepth: Int = 0,
    val parent: MacroSourceInfo? = null
) {
    companion object {
        val KEY = Key.create<MacroSourceInfo>("CANGJIE_MACRO_SOURCE_INFO")
    }
}
