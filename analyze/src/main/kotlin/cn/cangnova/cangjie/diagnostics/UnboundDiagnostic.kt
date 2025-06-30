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

package cn.cangnova.cangjie.diagnostics

import com.intellij.openapi.util.TextRange

/**
 * 未绑定诊断接口
 * 
 * 定义了诊断的基本属性，不与特定PSI元素绑定
 */
interface UnboundDiagnostic {
    /**
     * 诊断工厂
     * 
     * 用于创建和管理诊断实例
     */
    val factory: DiagnosticFactory<*>
    
    /**
     * 诊断严重性
     * 
     * 表示诊断的严重程度级别
     */
    val severity: Severity
    
    /**
     * 文本范围列表
     * 
     * 表示诊断在源代码中的位置
     */
    val textRanges: List<TextRange>
    
    /**
     * 诊断是否有效
     * 
     * 表示诊断是否可用于报告
     */
    val isValid: Boolean
}
