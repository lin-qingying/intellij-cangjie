package com.linqingying.cangjie.diagnostics.rendering

import com.linqingying.cangjie.diagnostics.*
import com.intellij.psi.PsiElement


class DiagnosticFactoryToRendererMap @JvmOverloads constructor(private val name: String = "<unnamed>") {
    private val map: MutableMap<DiagnosticFactory<*>, DiagnosticRenderer<*>> = HashMap()

    private var immutable = false

    override fun toString(): String {
        return "DiagnosticFactory#$name"
    }

    private fun checkMutability() {
        check(!immutable) { "factory to renderer map is already immutable" }
    }

    fun <E : PsiElement> put(factory: DiagnosticFactory0<E>, message: String) {
        checkMutability()
        map[factory] = SimpleDiagnosticRenderer(message)
    }

    fun <E : PsiElement> put(factory: DiagnosticFactory0<E>, message: () -> String) {
        checkMutability()
        map[factory] = SimpleDiagnosticRendererByFunction(message)
    }

    fun <E : PsiElement> put(factory: DiagnosticFactory0<E>, message: AstMsgData) {
        checkMutability()
        map[factory] = SimpleDiagnosticRendererByAstMsgData(message)
    }

    fun <E : PsiElement, A:Any> put(
        factory: DiagnosticFactory1<E, A>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?
    ) {
        checkMutability()
        map[factory] = DiagnosticWithParameters1Renderer(message, rendererA)
    }

    fun <E : PsiElement, A:Any> put(factory: DiagnosticFactory1<E, A>, message: String, rendererA: MultiRenderer<A>) {
        checkMutability()
        map[factory] =
            DiagnosticWithParametersMultiRenderer(message, rendererA)
    }

    fun <E : PsiElement, A:Any, B:Any> put(
        factory: DiagnosticFactory2<E, A, B>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>? ,
        rendererB: DiagnosticParameterRenderer<B>?
    ) {
        checkMutability()
        map[factory] =
            DiagnosticWithParameters2Renderer(
                message,
                rendererA,
                rendererB
            )
    }

    fun <E : PsiElement, A:Any, B:Any, C:Any> put(
        factory: DiagnosticFactory3<E, A, B, C>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A> ?,
        rendererB: DiagnosticParameterRenderer<B> ?,
        rendererC: DiagnosticParameterRenderer<C>?
    ) {
        checkMutability()
        map[factory] = DiagnosticWithParameters3Renderer(
            message,
            rendererA,
            rendererB,
            rendererC
        )
    }

    fun <E : PsiElement, A:Any, B:Any, C:Any, D:Any> put(
        factory: DiagnosticFactory4<E, A, B, C, D>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A> ?,
        rendererB: DiagnosticParameterRenderer<B> ?,
        rendererC: DiagnosticParameterRenderer<C> ?,
        rendererD: DiagnosticParameterRenderer<D>?
    ) {
        checkMutability()
        map[factory] = DiagnosticWithParameters4Renderer(
            message,
            rendererA,
            rendererB,
            rendererC,
            rendererD
        )
    }

    fun put(factory: DiagnosticFactory<*>, renderer: DiagnosticRenderer<*>) {
        checkMutability()
        map[factory] = renderer
    }

    fun <E : PsiElement> put(factory: DiagnosticFactoryForDeprecation0<E>, message: String) {
        checkMutability()
        map[factory.errorFactory] = SimpleDiagnosticRenderer(message)
        map[factory.warningFactory] = SimpleDiagnosticRenderer(
            deprecationMessage(
                factory,
                message
            )
        )
    }

    fun <E : PsiElement, A:Any> put(
        factory: DiagnosticFactoryForDeprecation1<E, A>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?
    ) {
        checkMutability()
        map[factory.errorFactory] =
            DiagnosticWithParameters1Renderer(message, rendererA)
        map[factory.warningFactory] = DiagnosticWithParameters1Renderer(
            deprecationMessage(
                factory,
                message
            ), rendererA
        )
    }

    fun <E : PsiElement, A:Any> put(
        factory: DiagnosticFactoryForDeprecation1<E, A>,
        message: String,
        rendererA: MultiRenderer<A>
    ) {
        checkMutability()
        map[factory.errorFactory] =
            DiagnosticWithParametersMultiRenderer(message, rendererA)
        map[factory.warningFactory] = DiagnosticWithParametersMultiRenderer(
            deprecationMessage(
                factory,
                message
            ), rendererA
        )
    }

    fun <E : PsiElement, A:Any, B:Any> put(
        factory: DiagnosticFactoryForDeprecation2<E, A, B>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>? ,
        rendererB: DiagnosticParameterRenderer<B>?
    ) {
        checkMutability()
        map[factory.errorFactory] =
            DiagnosticWithParameters2Renderer(
                message,
                rendererA,
                rendererB
            )
        map[factory.warningFactory] = DiagnosticWithParameters2Renderer(
            deprecationMessage(
                factory,
                message
            ), rendererA, rendererB
        )
    }

    fun <E : PsiElement, A:Any, B:Any, C:Any> put(
        factory: DiagnosticFactoryForDeprecation3<E, A, B, C>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A> ?,
        rendererB: DiagnosticParameterRenderer<B>? ,
        rendererC: DiagnosticParameterRenderer<C>?
    ) {
        checkMutability()
        map[factory.errorFactory] = DiagnosticWithParameters3Renderer(
            message,
            rendererA,
            rendererB,
            rendererC
        )
        map[factory.warningFactory] =
            DiagnosticWithParameters3Renderer(
                deprecationMessage(
                    factory,
                    message
                ), rendererA, rendererB, rendererC
            )
    }

    fun <E : PsiElement, A:Any, B : Any, C:Any, D:Any> put(
        factory: DiagnosticFactoryForDeprecation4<E, A, B, C, D>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A> ?,
        rendererB: DiagnosticParameterRenderer<B> ?,
        rendererC: DiagnosticParameterRenderer<C>? ,
        rendererD: DiagnosticParameterRenderer<D>?
    ) {
        checkMutability()
        map[factory.errorFactory] =
            DiagnosticWithParameters4Renderer(
                message,
                rendererA,
                rendererB,
                rendererC,
                rendererD
            )
        map[factory.warningFactory] =
            DiagnosticWithParameters4Renderer(
                deprecationMessage(
                    factory,
                    message
                ), rendererA, rendererB, rendererC, rendererD
            )
    }

    fun get(factory: DiagnosticFactory<*>): DiagnosticRenderer<*>? {
        return map[factory]
    }

    fun setImmutable() {
        immutable = false
    }




    companion object {
        private fun deprecationMessage(
            factory: DiagnosticFactoryForDeprecation<*, *, *>,
            errorMessage: String
        ): String {
            val sinceVersion = factory.deprecatingFeature.sinceVersion
            val builder = StringBuilder()
            builder.append(errorMessage).append(". This will become an error")
            if (sinceVersion != null) {
                builder.append(" in CangJie ").append(sinceVersion.versionString)
            } else {
                builder.append(" in a future release")
            }
            return builder.toString()
        }
    }
}
