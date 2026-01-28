/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.diagnostics.rendering

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.diagnostics.*

/**
 * 诊断渲染器配置
 *
 * 提供 DSL 风格的诊断渲染器配置接口
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 配置默认渲染器
 * DiagnosticRendererRegistry.configureDefault {
 *     register(SOME_DIAGNOSTIC) {
 *         message { CangJieDiagnosisBundle.rawMessage(it) }
 *         renderers(RENDER_TYPE, STRING)
 *     }
 * }
 *
 * // 配置 IDE 渲染器
 * DiagnosticRendererRegistry.configureIde {
 *     register(SOME_DIAGNOSTIC) {
 *         message { IDECangJieDiagnosisBundle.rawMessage(it) }
 *         renderers(RENDER_TYPE, STRING)
 *     }
 * }
 *
 * // 复杂配置（自定义参数提取）
 * DiagnosticRendererRegistry.configureDefault {
 *     register(COMPLEX_DIAGNOSTIC) {
 *         message { "Custom message: {0}, {1}" }
 *         parameterExtractor { diagnostic ->
 *             arrayOf(diagnostic.someField, diagnostic.otherField)
 *         }
 *     }
 * }
 * ```
 */
class DiagnosticRendererConfiguration(
    /**
     * 消息Bundle
     */
    private val bundle: MessageBundle
) {
    private val registrations = mutableListOf<Registration<*>>()

    /**
     * 注册无参数诊断
     */
    fun <E : PsiElement> register(
        factory: DiagnosticFactory0<E>,
        configure: RendererBuilder0<E>.() -> Unit
    ) {
        val builder = RendererBuilder0(factory, bundle)
        builder.configure()
        registrations.add(Registration(factory, builder.build()))
    }

    /**
     * 注册单参数诊断
     */
    fun <E : PsiElement, A> register(
        factory: DiagnosticFactory1<E, A>,
        configure: RendererBuilder1<E, A>.() -> Unit
    ) {
        val builder = RendererBuilder1(factory, bundle)
        builder.configure()
        registrations.add(Registration(factory, builder.build()))
    }

    /**
     * 注册双参数诊断
     */
    fun <E : PsiElement, A, B> register(
        factory: DiagnosticFactory2<E, A, B>,
        configure: RendererBuilder2<E, A, B>.() -> Unit
    ) {
        val builder = RendererBuilder2(factory, bundle)
        builder.configure()
        registrations.add(Registration(factory, builder.build()))
    }

    /**
     * 注册三参数诊断
     */
    fun <E : PsiElement, A, B, C> register(
        factory: DiagnosticFactory3<E, A, B, C>,
        configure: RendererBuilder3<E, A, B, C>.() -> Unit
    ) {
        val builder = RendererBuilder3(factory, bundle)
        builder.configure()
        registrations.add(Registration(factory, builder.build()))
    }

    /**
     * 注册四参数诊断
     */
    fun <E : PsiElement, A, B, C, D> register(
        factory: DiagnosticFactory4<E, A, B, C, D>,
        configure: RendererBuilder4<E, A, B, C, D>.() -> Unit
    ) {
        val builder = RendererBuilder4(factory, bundle)
        builder.configure()
        registrations.add(Registration(factory, builder.build()))
    }

    /**
     * 应用配置到渲染器映射
     */
    internal fun applyTo(renderers: MutableMap<DiagnosticFactory<*>, DiagnosticRenderer<*>>) {
        for (registration in registrations) {
            registration.applyTo(renderers)
        }
    }

    private data class Registration<D : UnboundDiagnostic>(
        val factory: DiagnosticFactory<D>,
        val renderer: DiagnosticRenderer<D>
    ) {
        fun applyTo(renderers: MutableMap<DiagnosticFactory<*>, DiagnosticRenderer<*>>) {
            renderers[factory] = renderer
        }
    }
}

/**
 * 渲染器构建器基类
 */
sealed class RendererBuilder<D : UnboundDiagnostic, F : DiagnosticFactory<D>>(
    protected val factory: F,
    protected val defaultBundle: MessageBundle
) {
    protected var messageProvider: ((F) -> String)? = null

    /**
     * 设置消息提供器
     */
    fun message(provider: (F) -> String) {
        messageProvider = provider
    }

    /**
     * 设置消息（固定字符串）
     */
    fun message(text: String) {
        messageProvider = { text }
    }

    protected fun getMessageProvider(): () -> String {
        val provider = messageProvider ?: { defaultBundle.getMessage(factory.name) }
        return { provider(factory) }
    }

    abstract fun build(): DiagnosticRenderer<D>
}

/**
 * 无参数诊断渲染器构建器
 */
class RendererBuilder0<E : PsiElement>(
    factory: DiagnosticFactory0<E>,
    defaultBundle: MessageBundle
) : RendererBuilder<SimpleDiagnostic<E>, DiagnosticFactory0<E>>(factory, defaultBundle) {

    override fun build(): DiagnosticRenderer<SimpleDiagnostic<E>> {
        return SimpleDiagnosticRenderer(getMessageProvider())
    }
}

/**
 * 单参数诊断渲染器构建器
 */
class RendererBuilder1<E : PsiElement, A>(
    factory: DiagnosticFactory1<E, A>,
    defaultBundle: MessageBundle
) : RendererBuilder<DiagnosticWithParameters1<E, A>, DiagnosticFactory1<E, A>>(factory, defaultBundle) {

    private var rendererA: DiagnosticParameterRenderer<A>? = null
    private var parameterExtractor: ((DiagnosticWithParameters1<E, A>) -> Array<Any?>)? = null

    /**
     * 设置参数渲染器
     */
    fun renderers(rendererA: DiagnosticParameterRenderer<A>) {
        this.rendererA = rendererA
    }

    /**
     * 设置自定义参数提取器
     */
    fun parameterExtractor(extractor: (DiagnosticWithParameters1<E, A>) -> Array<Any?>) {
        parameterExtractor = extractor
    }

    override fun build(): DiagnosticRenderer<DiagnosticWithParameters1<E, A>> {
        return if (parameterExtractor != null) {
            CustomParameterRenderer(getMessageProvider(), parameterExtractor!!)
        } else {
            DiagnosticWithParameters1Renderer(
                message = getMessageProvider(),
                rendererForA = rendererA
            )
        }
    }
}

/**
 * 双参数诊断渲染器构建器
 */
class RendererBuilder2<E : PsiElement, A, B>(
    factory: DiagnosticFactory2<E, A, B>,
    defaultBundle: MessageBundle
) : RendererBuilder<DiagnosticWithParameters2<E, A, B>, DiagnosticFactory2<E, A, B>>(factory, defaultBundle) {

    private var rendererA: DiagnosticParameterRenderer<A>? = null
    private var rendererB: DiagnosticParameterRenderer<B>? = null
    private var parameterExtractor: ((DiagnosticWithParameters2<E, A, B>) -> Array<Any?>)? = null

    /**
     * 设置参数渲染器
     */
    fun renderers(
        rendererA: DiagnosticParameterRenderer<A>,
        rendererB: DiagnosticParameterRenderer<B>
    ) {
        this.rendererA = rendererA
        this.rendererB = rendererB
    }

    /**
     * 设置自定义参数提取器
     */
    fun parameterExtractor(extractor: (DiagnosticWithParameters2<E, A, B>) -> Array<Any?>) {
        parameterExtractor = extractor
    }

    override fun build(): DiagnosticRenderer<DiagnosticWithParameters2<E, A, B>> {
        return if (parameterExtractor != null) {
            CustomParameterRenderer(getMessageProvider(), parameterExtractor!!)
        } else {
            DiagnosticWithParameters2Renderer(
                message = getMessageProvider(),
                rendererForA = rendererA,
                rendererForB = rendererB
            )
        }
    }
}

/**
 * 三参数诊断渲染器构建器
 */
class RendererBuilder3<E : PsiElement, A, B, C>(
    factory: DiagnosticFactory3<E, A, B, C>,
    defaultBundle: MessageBundle
) : RendererBuilder<DiagnosticWithParameters3<E, A, B, C>, DiagnosticFactory3<E, A, B, C>>(factory, defaultBundle) {

    private var rendererA: DiagnosticParameterRenderer<A>? = null
    private var rendererB: DiagnosticParameterRenderer<B>? = null
    private var rendererC: DiagnosticParameterRenderer<C>? = null
    private var parameterExtractor: ((DiagnosticWithParameters3<E, A, B, C>) -> Array<Any?>)? = null

    /**
     * 设置参数渲染器
     */
    fun renderers(
        rendererA: DiagnosticParameterRenderer<A>,
        rendererB: DiagnosticParameterRenderer<B>,
        rendererC: DiagnosticParameterRenderer<C>
    ) {
        this.rendererA = rendererA
        this.rendererB = rendererB
        this.rendererC = rendererC
    }

    /**
     * 设置自定义参数提取器
     */
    fun parameterExtractor(extractor: (DiagnosticWithParameters3<E, A, B, C>) -> Array<Any?>) {
        parameterExtractor = extractor
    }

    override fun build(): DiagnosticRenderer<DiagnosticWithParameters3<E, A, B, C>> {
        return if (parameterExtractor != null) {
            CustomParameterRenderer(getMessageProvider(), parameterExtractor!!)
        } else {
            DiagnosticWithParameters3Renderer(
                message = getMessageProvider(),
                rendererForA = rendererA,
                rendererForB = rendererB,
                rendererForC = rendererC
            )
        }
    }
}

/**
 * 四参数诊断渲染器构建器
 */
class RendererBuilder4<E : PsiElement, A, B, C, D>(
    factory: DiagnosticFactory4<E, A, B, C, D>,
    defaultBundle: MessageBundle
) : RendererBuilder<DiagnosticWithParameters4<E, A, B, C, D>, DiagnosticFactory4<E, A, B, C, D>>(factory, defaultBundle) {

    private var rendererA: DiagnosticParameterRenderer<A>? = null
    private var rendererB: DiagnosticParameterRenderer<B>? = null
    private var rendererC: DiagnosticParameterRenderer<C>? = null
    private var rendererD: DiagnosticParameterRenderer<D>? = null
    private var parameterExtractor: ((DiagnosticWithParameters4<E, A, B, C, D>) -> Array<Any?>)? = null

    /**
     * 设置参数渲染器
     */
    fun renderers(
        rendererA: DiagnosticParameterRenderer<A>,
        rendererB: DiagnosticParameterRenderer<B>,
        rendererC: DiagnosticParameterRenderer<C>,
        rendererD: DiagnosticParameterRenderer<D>
    ) {
        this.rendererA = rendererA
        this.rendererB = rendererB
        this.rendererC = rendererC
        this.rendererD = rendererD
    }

    /**
     * 设置自定义参数提取器
     */
    fun parameterExtractor(extractor: (DiagnosticWithParameters4<E, A, B, C, D>) -> Array<Any?>) {
        parameterExtractor = extractor
    }

    override fun build(): DiagnosticRenderer<DiagnosticWithParameters4<E, A, B, C, D>> {
        return if (parameterExtractor != null) {
            CustomParameterRenderer(getMessageProvider(), parameterExtractor!!)
        } else {
            DiagnosticWithParameters4Renderer(
                message = getMessageProvider(),
                rendererForA = rendererA,
                rendererForB = rendererB,
                rendererForC = rendererC,
                rendererForD = rendererD
            )
        }
    }
}

/**
 * 自定义参数渲染器
 *
 * 用于处理需要自定义参数提取逻辑的诊断
 */
private class CustomParameterRenderer<D : UnboundDiagnostic>(
    message: () -> String,
    private val extractor: (D) -> Array<Any?>
) : AbstractDiagnosticWithParametersRenderer<D>(message) {

    override fun renderParameters(diagnostic: D): Array<out Any?> {
        return extractor(diagnostic)
    }
}
