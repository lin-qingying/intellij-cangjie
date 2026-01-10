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

package org.cangnova.cangjie.diagnostics

import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import com.intellij.psi.PsiElement

/**
 * 废弃功能诊断工厂基类
 *
 * 用于处理语言特性废弃的诊断工厂。
 * 根据语言版本设置，自动选择使用警告（WARNING）或错误（ERROR）严重性级别。
 *
 * 设计目的：
 * - 支持语言特性的渐进式废弃
 * - 在旧版本中显示警告，在新版本中显示错误
 * - 统一管理废弃相关的诊断
 *
 * 工作流程：
 * 1. 定义废弃的语言特性（deprecatingFeature）
 * 2. 创建警告工厂（warningFactory）和错误工厂（errorFactory）
 * 3. 根据语言版本设置自动选择合适的工厂
 *
 * 使用示例：
 * ```kotlin
 * val DEPRECATED_SYNTAX = DiagnosticFactoryForDeprecation0.create(
 *     featureForError = LanguageFeature.ProhibitOldSyntax,
 *     positioningStrategy = PositioningStrategies.DEFAULT
 * )
 *
 * // 在分析器中使用
 * val diagnostic = DEPRECATED_SYNTAX.on(languageVersionSettings, element)
 * // 如果 languageVersionSettings 支持 ProhibitOldSyntax，则为 ERROR
 * // 否则为 WARNING
 * ```
 *
 * 子类实现：
 * - [DiagnosticFactoryForDeprecation0]：无参数废弃诊断
 * - [DiagnosticFactoryForDeprecation1]：带 1 个参数
 * - [DiagnosticFactoryForDeprecation2]：带 2 个参数
 * - [DiagnosticFactoryForDeprecation3]：带 3 个参数
 * - [DiagnosticFactoryForDeprecation4]：带 4 个参数
 *
 * @param E PSI 元素类型
 * @param D 诊断类型
 * @param F 诊断工厂类型
 * @param deprecatingFeature 触发错误级���的语言特性
 * @param warningFactory 警告级别的诊断工厂
 * @param errorFactory 错误级别的诊断工厂
 */
sealed class DiagnosticFactoryForDeprecation<E : PsiElement,
        D : Diagnostic,
        F : DiagnosticFactoryWithPsiElement<E, D>>(


    val deprecatingFeature: LanguageFeature,
    val warningFactory: F,
    val errorFactory: F
) {

    /**
     * 根据语言版本设置选择工厂
     *
     * 如果语言版本支持废弃特性，返回错误工厂；
     * 否则返回警告工厂。
     *
     * @return 合适的诊断工厂
     */
    fun LanguageVersionSettings.chooseFactory(): F {
        return if (supportsFeature(deprecatingFeature)) errorFactory else warningFactory
    }
}

/**
 * 无参数废弃诊断工厂
 *
 * 用于创建无参数的废弃功能诊断。
 *
 * @param E PSI 元素类型
 */
class DiagnosticFactoryForDeprecation0<E : PsiElement>(
    featureForError: LanguageFeature,
    warningFactory: DiagnosticFactory0<E>,
    errorFactory: DiagnosticFactory0<E>
) : DiagnosticFactoryForDeprecation<E, SimpleDiagnostic<E>, DiagnosticFactory0<E>>(
    featureForError,
    warningFactory,
    errorFactory
) {
    companion object {
        /**
         * 创建废弃诊断工厂
         *
         * @param E PSI 元素类型
         * @param featureForError 触发错误级别的语言特性
         * @param positioningStrategy 定位策略，默认为 DEFAULT
         * @return 废弃诊断工厂实例
         */
        
        @JvmOverloads
        fun <E : PsiElement> create(
            featureForError: LanguageFeature,
            positioningStrategy: PositioningStrategy<E> = PositioningStrategies.DEFAULT
        ): DiagnosticFactoryForDeprecation0<E> {
            return DiagnosticFactoryForDeprecation0(
                featureForError,
                warningFactory = DiagnosticFactory0.create(Severity.WARNING, positioningStrategy),
                errorFactory = DiagnosticFactory0.create(Severity.ERROR, positioningStrategy),
            )
        }
    }

    /**
     * 创建废弃诊断
     *
     * 根据语言版本设置自动选择警告或错误级别。
     *
     * @param languageVersionSettings 语言版本设置
     * @param element PSI 元素
     * @return 诊断实例
     */
    fun on(languageVersionSettings: LanguageVersionSettings, element: E): SimpleDiagnostic<E> {
        return languageVersionSettings.chooseFactory().on(element)
    }

    /**
     * 创建错误级别的诊断
     *
     * 强制使用错误级别，不考虑语言版本设置。
     *
     * @param element PSI 元素
     * @return 错误级别的诊断实例
     */
    fun onError(element: E): SimpleDiagnostic<E> = errorFactory.on(element)
}

/**
 * 带 2 个参数的废弃诊断工厂
 *
 * @param E PSI 元素类型
 * @param A 第一个参数类型
 * @param B 第二个参数类型
 */
class DiagnosticFactoryForDeprecation2<E : PsiElement, A : Any, B : Any>(
    featureForError: LanguageFeature,
    warningFactory: DiagnosticFactory2<E, A, B>,
    errorFactory: DiagnosticFactory2<E, A, B>
) : DiagnosticFactoryForDeprecation<E, DiagnosticWithParameters2<E, A, B>, DiagnosticFactory2<E, A, B>>(
    featureForError,
    warningFactory,
    errorFactory
) {
    companion object {
        
        @JvmOverloads
        fun <E : PsiElement, A : Any, B : Any> create(
            featureForError: LanguageFeature,
            positioningStrategy: PositioningStrategy<E> = PositioningStrategies.DEFAULT
        ): DiagnosticFactoryForDeprecation2<E, A, B> {
            return DiagnosticFactoryForDeprecation2(
                featureForError,
                warningFactory = DiagnosticFactory2.create(Severity.WARNING, positioningStrategy),
                errorFactory = DiagnosticFactory2.create(Severity.ERROR, positioningStrategy),
            )
        }
    }

    /**
     * 创建带参数的废弃诊断
     *
     * @param languageVersionSettings 语言版本设置
     * @param element PSI 元素
     * @param a 第一个参数
     * @param b 第二个参数
     * @return 诊断实例
     */
    fun on(languageVersionSettings: LanguageVersionSettings, element: E, a: A, b: B): ParametrizedDiagnostic<E> {
        return languageVersionSettings.chooseFactory().on(element, a, b)
    }
}

/**
 * 带 1 个参数的废弃诊断工厂
 *
 * @param E PSI 元素类型
 * @param A 参数类型
 */
class DiagnosticFactoryForDeprecation1<E : PsiElement, A : Any>(
    featureForError: LanguageFeature,
    warningFactory: DiagnosticFactory1<E, A>,
    errorFactory: DiagnosticFactory1<E, A>
) : DiagnosticFactoryForDeprecation<E, DiagnosticWithParameters1<E, A>, DiagnosticFactory1<E, A>>(
    featureForError,
    warningFactory,
    errorFactory
) {
    companion object {
        
        @JvmOverloads
        fun <E : PsiElement, A : Any> create(
            featureForError: LanguageFeature,
            positioningStrategy: PositioningStrategy<E> = PositioningStrategies.DEFAULT
        ): DiagnosticFactoryForDeprecation1<E, A> {
            return DiagnosticFactoryForDeprecation1(
                featureForError,
                warningFactory = DiagnosticFactory1.create(Severity.WARNING, positioningStrategy),
                errorFactory = DiagnosticFactory1.create(Severity.ERROR, positioningStrategy),
            )
        }
    }

    /**
     * 创建带参数的废弃诊断
     *
     * @param languageVersionSettings 语言版本设置
     * @param element PSI 元素
     * @param a 参数
     * @return 诊断实例
     */
    fun on(languageVersionSettings: LanguageVersionSettings, element: E, a: A): ParametrizedDiagnostic<E> {
        return languageVersionSettings.chooseFactory().on(element, a)
    }
}

/**
 * 带 3 个参数的废弃诊断工厂
 *
 * @param E PSI 元素类型
 * @param A 第一个参数类型
 * @param B 第二个参数类型
 * @param C 第三个参数类型
 */
class DiagnosticFactoryForDeprecation3<E : PsiElement, A : Any, B : Any, C : Any>(
    featureForError: LanguageFeature,
    warningFactory: DiagnosticFactory3<E, A, B, C>,
    errorFactory: DiagnosticFactory3<E, A, B, C>
) : DiagnosticFactoryForDeprecation<E, DiagnosticWithParameters3<E, A, B, C>, DiagnosticFactory3<E, A, B, C>>(
    featureForError,
    warningFactory,
    errorFactory
) {
    companion object {
        
        @JvmOverloads
        fun <E : PsiElement, A : Any, B : Any, C : Any> create(
            featureForError: LanguageFeature,
            positioningStrategy: PositioningStrategy<E> = PositioningStrategies.DEFAULT
        ): DiagnosticFactoryForDeprecation3<E, A, B, C> {
            return DiagnosticFactoryForDeprecation3(
                featureForError,
                warningFactory = DiagnosticFactory3.Companion.create(Severity.WARNING, positioningStrategy),
                errorFactory = DiagnosticFactory3.Companion.create(Severity.ERROR, positioningStrategy),
            )
        }
    }

    /**
     * 创建带参数的废弃诊断
     *
     * @param languageVersionSettings 语言版本设置
     * @param element PSI 元素
     * @param a 第一个参数
     * @param b 第二个参数
     * @param c 第三个参数
     * @return 诊断实例
     */
    fun on(languageVersionSettings: LanguageVersionSettings, element: E, a: A, b: B, c: C): ParametrizedDiagnostic<E> {
        return languageVersionSettings.chooseFactory().on(element, a, b, c)
    }
}

/**
 * 带 4 个参数的废弃诊断工厂
 *
 * @param E PSI 元素类型
 * @param A 第一个参数类型
 * @param B 第二个参数类型
 * @param C 第三个参数类型
 * @param D 第四个参数类型
 */
class DiagnosticFactoryForDeprecation4<E : PsiElement, A : Any, B : Any, C : Any, D : Any>(
    featureForError: LanguageFeature,
    warningFactory: DiagnosticFactory4<E, A, B, C, D>,
    errorFactory: DiagnosticFactory4<E, A, B, C, D>
) : DiagnosticFactoryForDeprecation<E, DiagnosticWithParameters4<E, A, B, C, D>, DiagnosticFactory4<E, A, B, C, D>>(
    featureForError,
    warningFactory,
    errorFactory
) {
    companion object {
        
        @JvmOverloads
        fun <E : PsiElement, A : Any, B : Any, C : Any, D : Any> create(
            featureForError: LanguageFeature,
            positioningStrategy: PositioningStrategy<E> = PositioningStrategies.DEFAULT
        ): DiagnosticFactoryForDeprecation4<E, A, B, C, D> {
            return DiagnosticFactoryForDeprecation4(
                featureForError,
                warningFactory = DiagnosticFactory4.create(Severity.WARNING, positioningStrategy),
                errorFactory = DiagnosticFactory4.create(Severity.ERROR, positioningStrategy),
            )
        }
    }

    /**
     * 创建带参数的废弃诊断
     *
     * @param languageVersionSettings 语言版本设置
     * @param element PSI 元素
     * @param a 第一个参数
     * @param b 第二个参数
     * @param c 第三个参数
     * @param d 第四个参数
     * @return 诊断实例
     */
    fun on(
        languageVersionSettings: LanguageVersionSettings,
        element: E,
        a: A,
        b: B,
        c: C,
        d: D
    ): ParametrizedDiagnostic<E> {
        return languageVersionSettings.chooseFactory().on(element, a, b, c, d)
    }
}
