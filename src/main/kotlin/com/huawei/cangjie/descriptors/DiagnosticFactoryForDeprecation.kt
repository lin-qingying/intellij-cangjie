package com.huawei.cangjie.descriptors

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.diagnostics.*
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
class DiagnosticFactoryForDeprecation2<E : PsiElement, A : Any, B : Any>(
    featureForError: LanguageFeature,
    warningFactory: DiagnosticFactory2<E, A, B>,
    errorFactory: DiagnosticFactory2<E, A, B>
) : DiagnosticFactoryForDeprecation<E, DiagnosticWithParameters2<E, A, B>, DiagnosticFactory2<E, A, B>>(featureForError, warningFactory, errorFactory) {
    companion object {
        @JvmStatic
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

    fun on(languageVersionSettings: LanguageVersionSettings, element: E, a: A, b: B): ParametrizedDiagnostic<E> {
        return languageVersionSettings.chooseFactory().on(element, a, b)
    }
}
