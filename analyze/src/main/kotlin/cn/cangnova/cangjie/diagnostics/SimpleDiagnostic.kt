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

import com.intellij.psi.PsiElement

/**
 * 简单诊断类
 * 
 * 表示不带参数的基本诊断信息
 *
 * @param E PSI元素类型
 * @property psiElement 与诊断相关的PSI元素
 * @property factory 创建此诊断的工厂
 * @property severity 诊断的严重性级别
 */
class SimpleDiagnostic<E : PsiElement >(
    psiElement: E,
    factory: DiagnosticFactory0<E>,
    severity: Severity
) : AbstractDiagnostic<E>(psiElement, factory, severity) {
    /**
     * 获取诊断工厂
     * 
     * 覆盖父类方法，确保返回正确的工厂类型
     *
     * @return 创建此诊断的工厂
     */
    override val factory: DiagnosticFactory0<E>
        get() = super.factory as DiagnosticFactory0<E>
}
