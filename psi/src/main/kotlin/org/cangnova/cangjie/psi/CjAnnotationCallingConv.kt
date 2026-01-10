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

package org.cangnova.cangjie.psi

import com.intellij.lang.ASTNode

/**
 * PSI元素类,表示@CallingConv注解的调用约定参数部分
 *
 * 示例:
 * ```cangjie
 * @CallingConv[CDECL]  // CDECL是调用约定参数
 * @CallingConv[STDCALL]  // STDCALL是调用约定参数
 * ```
 *
 * 此类用于封装@CallingConv注解中方括号内的调用约定标识符,
 * 支持的调用约定包括: CDECL, STDCALL
 */
class CjAnnotationCallingConv(node: ASTNode) : CjElementImpl(node) {
    /**
     * 获取调用约定的文本值
     * @return 调用约定字符串,如 "CDECL" 或 "STDCALL"
     */
    fun getCallingConventionText(): String? = text
}
