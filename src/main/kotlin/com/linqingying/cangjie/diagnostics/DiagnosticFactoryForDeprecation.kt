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

package com.linqingying.cangjie.diagnostics

import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.PositioningStrategies
import com.intellij.psi.PsiElement

sealed class DiagnosticFactoryForDeprecation<E : PsiElement, D : Diagnostic, F : DiagnosticFactoryWithPsiElement<E, D>>(
    val deprecatingFeature: LanguageFeature,
    val warningFactory: F,
    val errorFactory: F
)
{
    fun LanguageVersionSettings.chooseFactory(): F {
        return if (supportsFeature(deprecatingFeature)) errorFactory else warningFactory
    }
}

class DiagnosticFactoryForDeprecation0<E : PsiElement>(
    featureForError: LanguageFeature,
    warningFactory: DiagnosticFactory0<E>,
    errorFactory: DiagnosticFactory0<E>
) : DiagnosticFactoryForDeprecation<E, SimpleDiagnostic<E>, DiagnosticFactory0<E>>(featureForError, warningFactory, errorFactory) {
    companion object {
        @JvmStatic
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

    fun on(languageVersionSettings: LanguageVersionSettings, element: E): SimpleDiagnostic<E> {
        return languageVersionSettings.chooseFactory().on(element)
    }

    fun onError(element: E): SimpleDiagnostic<E> = errorFactory.on(element)
}
class DiagnosticFactoryForDeprecation2<E : PsiElement, A:Any  , B:Any  >(
    featureForError: LanguageFeature,
    warningFactory: DiagnosticFactory2<E, A, B>,
    errorFactory: DiagnosticFactory2<E, A, B>
) : DiagnosticFactoryForDeprecation<E, DiagnosticWithParameters2<E, A, B>, DiagnosticFactory2<E, A, B>>(featureForError, warningFactory, errorFactory) {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun <E : PsiElement, A  :Any, B:Any  > create(
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

    fun on(languageVersionSettings: LanguageVersionSettings, element: E, a: A, b: B): ParametrizedDiagnostic<E> {
        return languageVersionSettings.chooseFactory().on(element, a, b)
    }
}

class DiagnosticFactoryForDeprecation1<E : PsiElement, A:Any >(
    featureForError: LanguageFeature,
    warningFactory: DiagnosticFactory1<E, A>,
    errorFactory: DiagnosticFactory1<E, A>
) : DiagnosticFactoryForDeprecation<E, DiagnosticWithParameters1<E, A>, DiagnosticFactory1<E, A>>(featureForError, warningFactory, errorFactory) {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun <E : PsiElement, A :Any > create(
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

    fun on(languageVersionSettings: LanguageVersionSettings, element: E, a: A): ParametrizedDiagnostic<E> {
        return languageVersionSettings.chooseFactory().on(element, a)
    }
}

class DiagnosticFactoryForDeprecation3<E : PsiElement, A:Any  , B :Any , C:Any  >(
    featureForError: LanguageFeature,
    warningFactory: DiagnosticFactory3<E, A, B, C>,
    errorFactory: DiagnosticFactory3<E, A, B, C>
) : DiagnosticFactoryForDeprecation<E, DiagnosticWithParameters3<E, A, B, C>, DiagnosticFactory3<E, A, B, C>>(featureForError, warningFactory, errorFactory) {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun <E : PsiElement, A  :Any, B:Any  , C :Any > create(
            featureForError: LanguageFeature,
            positioningStrategy: PositioningStrategy<E> = PositioningStrategies.DEFAULT
        ): DiagnosticFactoryForDeprecation3<E, A, B, C> {
            return DiagnosticFactoryForDeprecation3(
                featureForError,
                warningFactory = DiagnosticFactory3.create(Severity.WARNING, positioningStrategy),
                errorFactory = DiagnosticFactory3.create(Severity.ERROR, positioningStrategy),
            )
        }
    }

    fun on(languageVersionSettings: LanguageVersionSettings, element: E, a: A, b: B, c: C): ParametrizedDiagnostic<E> {
        return languageVersionSettings.chooseFactory().on(element, a, b, c)
    }
}
class DiagnosticFactoryForDeprecation4<E : PsiElement, A  :Any, B :Any , C:Any  , D:Any  >(
    featureForError: LanguageFeature,
    warningFactory: DiagnosticFactory4<E, A, B, C, D>,
    errorFactory: DiagnosticFactory4<E, A, B, C, D>
) : DiagnosticFactoryForDeprecation<E, DiagnosticWithParameters4<E, A, B, C, D>, DiagnosticFactory4<E, A, B, C, D>>(featureForError, warningFactory, errorFactory) {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun <E : PsiElement, A  :Any, B:Any  , C:Any  , D :Any > create(
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

    fun on(languageVersionSettings: LanguageVersionSettings, element: E, a: A, b: B, c: C, d: D): ParametrizedDiagnostic<E> {
        return languageVersionSettings.chooseFactory().on(element, a, b, c, d)
    }
}
