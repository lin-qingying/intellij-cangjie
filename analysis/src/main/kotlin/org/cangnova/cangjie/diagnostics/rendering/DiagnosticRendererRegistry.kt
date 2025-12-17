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

import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.diagnostics.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 诊断渲染器注册中心
 *
 * 负责管理所有诊断工厂到渲染器的映射，支持：
 * 1. 自动推断：基于参数类型自动选择合适的渲染器
 * 2. 显式配置：复杂诊断可以手动配置渲染逻辑
 * 3. 分包管理：每个诊断包维护自己的渲染器配置
 *
 * ## 初始化机制
 *
 * **延迟初始化**：为避免在类加载时（静态初始化阶段）依赖服务，
 * 本注册中心采用延迟初始化策略：
 * - 不在 `init` 块或静态字段中进行初始化
 * - 通过 [org.cangnova.cangjie.diagnostics.DiagnosticInitializerStartupActivity]
 *   在项目启动后调用 [ensureInitialized]
 * - 避免 IntelliJ 平台的 "Class initialization must not depend on services" 错误
 *
 * ## 自动推断规则
 *
 * - 消息：使用 `DiagnosticFactory.name` 查找 `CangJieDiagnosisBundle`
 * - 参数渲染器：基于参数类型自动匹配（String → STRING, CangJieType → RENDER_TYPE 等）
 * - 弃用诊断：自动处理 ERROR 和 WARNING 两个变体
 *
 * ## 使用示例
 *
 * ### 自动推断（无需配置）
 * ```kotlin
 * // 这些诊断会自动推断渲染器
 * val UNRESOLVED_REFERENCE: DiagnosticFactory1<CjSimpleNameExpression, String> = ...
 * val TYPE_MISMATCH: DiagnosticFactory2<PsiElement, CangJieType, CangJieType> = ...
 * ```
 *
 * ### 显式配置（复杂诊断）
 * ```kotlin
 * DiagnosticRendererRegistry.configure {
 *     register(INVALID_BINARY_OPERATOR) {
 *         message { CangJieDiagnosisBundle.rawMessage(it) }
 *         parameterExtractor { diagnostic ->
 *             arrayOf(
 *                 diagnostic.operatorString,
 *                 diagnostic.leftType,
 *                 diagnostic.rightType
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * @see org.cangnova.cangjie.diagnostics.DiagnosticInitializerStartupActivity
 */
object DiagnosticRendererRegistry {
    private val explicitRenderers = ConcurrentHashMap<DiagnosticFactory<*>, DiagnosticRenderer<*>>()
    private val configurations = mutableListOf<DiagnosticRendererConfiguration>()
    private val LOG = logger<DiagnosticRendererRegistry>()

    @Volatile
    private var initialized = false

    /**
     * 确保渲染器注册中心已初始化
     *
     * 通过扩展点自动加载所有 [DiagnosticRendererProvider] 实现。
     *
     * **延迟初始化**：不在类加载时初始化，而是在项目启动后通过
     * [org.cangnova.cangjie.diagnostics.DiagnosticInitializerStartupActivity] 调用，
     * 避免在静态初始化阶段依赖服务。
     *
     * 此方法是线程安全的，可以被多次调用（后续调用会立即返回）。
     */
    fun ensureInitialized() {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            // 通过扩展点加载所有渲染器提供者
            DiagnosticRendererProvider.EP_NAME.extensionList.forEach { provider ->
                try {
                    provider.register()
                } catch (e: Exception) {
                    // 记录错误但继续加载其他提供者
                    LOG.warn("Failed to register diagnostic renderer provider: ${provider.javaClass.name}", e)
                }
            }

            initialized = true
        }
    }

    /**
     * 配置渲染器
     *
     * @param block 配置块
     */
    fun configure(block: DiagnosticRendererConfiguration.() -> Unit) {
        val config = DiagnosticRendererConfiguration()
        config.block()
        configurations.add(config)
        config.applyTo(this)
    }

    /**
     * 注册渲染器
     *
     * @param factory 诊断工厂
     * @param renderer 渲染器
     */
    fun <D : UnboundDiagnostic> register(
        factory: DiagnosticFactory<D>,
        renderer: DiagnosticRenderer<D>
    ) {
        explicitRenderers[factory] = renderer
    }

    /**
     * 获取渲染器
     *
     * 优先级：
     * 1. 显式注册的渲染器
     * 2. 自动推断的渲染器
     * 3. 工厂的默认渲染器
     *
     * @param factory 诊断工厂
     * @return 渲染器，如果未找到返回 null
     */
    fun <D : UnboundDiagnostic> getRenderer(factory: DiagnosticFactory<D>): DiagnosticRenderer<D>? {
        // 1. 查找显式注册的渲染器
        @Suppress("UNCHECKED_CAST")
        explicitRenderers[factory]?.let { return it as DiagnosticRenderer<D> }

        // 2. 自动推断渲染器
        return inferRenderer(factory)
    }

    /**
     * 渲染诊断
     *
     * @param diagnostic 诊断对象
     * @return 渲染后的消息文本
     */
    fun render(diagnostic: UnboundDiagnostic): String {
        val renderer = getRenderer(diagnostic.factory)
        if (renderer != null) {
            @Suppress("UNCHECKED_CAST")
            return (renderer as DiagnosticRenderer<UnboundDiagnostic>).render(diagnostic)
        }

        // 降级到工厂的默认渲染器
        diagnostic.factory.defaultRenderer?.let {
            @Suppress("UNCHECKED_CAST")
            return (it as DiagnosticRenderer<UnboundDiagnostic>).render(diagnostic)
        }

        return "$diagnostic (error: could not render message)"
    }

    /**
     * 自动推断渲染器
     *
     * 根据诊断工厂类型和参数类型自动推断合适的渲染器
     *
     * @param factory 诊断工厂
     * @return 推断的渲染器，如果无法推断返回 null
     */
    @Suppress("UNCHECKED_CAST")
    private fun <D : UnboundDiagnostic> inferRenderer(
        factory: DiagnosticFactory<D>
    ): DiagnosticRenderer<D>? {
        return when (factory) {
            is DiagnosticFactory0<*> -> inferRenderer0(factory as DiagnosticFactory0<*>)
            is DiagnosticFactory1<*, *> -> inferRenderer1(factory as DiagnosticFactory1<*, *>)
            is DiagnosticFactory2<*, *, *> -> inferRenderer2(factory as DiagnosticFactory2<*, *, *>)
            is DiagnosticFactory3<*, *, *, *> -> inferRenderer3(factory as DiagnosticFactory3<*, *, *, *>)
            is DiagnosticFactory4<*, *, *, *, *> -> inferRenderer4(factory as DiagnosticFactory4<*, *, *, *, *>)
            else -> null
        } as? DiagnosticRenderer<D>
    }

    /**
     * 推断无参数诊断的渲染器
     */
    private fun <E : PsiElement> inferRenderer0(
        factory: DiagnosticFactory0<E>
    ): DiagnosticRenderer<*> {
        return SimpleDiagnosticRenderer {
            CangJieDiagnosisBundle.rawMessage(factory.name)
        }
    }

    /**
     * 推断单参数诊断的渲染器
     *
     * 使用默认渲染器（TO_STRING），复杂情况需要显式配置
     */
    @Suppress("UNCHECKED_CAST")
    private fun <E : PsiElement, A : Any> inferRenderer1(
        factory: DiagnosticFactory1<E, A>
    ): DiagnosticRenderer<*> {
        return DiagnosticWithParameters1Renderer(
            message = { CangJieDiagnosisBundle.rawMessage(factory.name) },
            rendererForA = Renderers.TO_STRING as DiagnosticParameterRenderer<A>
        )
    }

    /**
     * 推断双参数诊断的渲染器
     *
     * 使用默认渲染器（TO_STRING），复杂情况需要显式配置
     */
    @Suppress("UNCHECKED_CAST")
    private fun <E : PsiElement, A , B> inferRenderer2(
        factory: DiagnosticFactory2<E, A, B>
    ): DiagnosticRenderer<*> {
        return DiagnosticWithParameters2Renderer(
            message = { CangJieDiagnosisBundle.rawMessage(factory.name) },
            rendererForA = Renderers.TO_STRING as DiagnosticParameterRenderer<A>,
            rendererForB = Renderers.TO_STRING as DiagnosticParameterRenderer<B>
        )
    }

    /**
     * 推断三参数诊断的渲染器
     *
     * 使用默认渲染器（TO_STRING），复杂情况需要显式配置
     */
    @Suppress("UNCHECKED_CAST")
    private fun <E : PsiElement, A : Any, B : Any, C : Any> inferRenderer3(
        factory: DiagnosticFactory3<E, A, B, C>
    ): DiagnosticRenderer<*> {
        return DiagnosticWithParameters3Renderer(
            message = { CangJieDiagnosisBundle.rawMessage(factory.name) },
            rendererForA = Renderers.TO_STRING as DiagnosticParameterRenderer<A>,
            rendererForB = Renderers.TO_STRING as DiagnosticParameterRenderer<B>,
            rendererForC = Renderers.TO_STRING as DiagnosticParameterRenderer<C>
        )
    }

    /**
     * 推断四参数诊断的渲染器
     *
     * 使用默认渲染器（TO_STRING），复杂情况需要显式配置
     */
    @Suppress("UNCHECKED_CAST")
    private fun <E : PsiElement, A : Any, B : Any, C : Any, D : Any> inferRenderer4(
        factory: DiagnosticFactory4<E, A, B, C, D>
    ): DiagnosticRenderer<*> {
        return DiagnosticWithParameters4Renderer(
            message = { CangJieDiagnosisBundle.rawMessage(factory.name) },
            rendererForA = Renderers.TO_STRING as DiagnosticParameterRenderer<A>,
            rendererForB = Renderers.TO_STRING as DiagnosticParameterRenderer<B>,
            rendererForC = Renderers.TO_STRING as DiagnosticParameterRenderer<C>,
            rendererForD = Renderers.TO_STRING as DiagnosticParameterRenderer<D>
        )
    }
}
