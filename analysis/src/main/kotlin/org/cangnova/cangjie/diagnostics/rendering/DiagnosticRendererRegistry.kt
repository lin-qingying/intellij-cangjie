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
 * 1. **默认渲染器**：基础的纯文本渲染
 * 2. **可扩展架构**：支持注册任意类型的渲染器（如 IDE、Web 等）
 * 3. **自动推断**：基于参数类型自动选择合适的渲染器
 * 4. **自动回退**：扩展渲染器找不到时自动回退到默认渲染器
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
 * ## 渲染器类型
 *
 * - **DEFAULT**：默认渲染器，使用 [CangJieDiagnosisBundle]，纯文本格式
 * - **IDE**：IDE渲染器，使用 [IDECangJieDiagnosisBundle]，HTML富文本格式
 * - **自定义类型**：可通过 [configure] 注册任意类型的渲染器
 *
 * ## 使用示例
 *
 * ### 自动推断（无需配置）
 * ```kotlin
 * // 这些诊断会自动推断默认和IDE两种渲染器
 * val UNRESOLVED_REFERENCE: DiagnosticFactory1<CjSimpleNameExpression, String> = ...
 * val TYPE_MISMATCH: DiagnosticFactory2<PsiElement, CangJieType, CangJieType> = ...
 * ```
 *
 * ### 显式配置（复杂诊断）
 * ```kotlin
 * class ErrorRenderers : DiagnosticRendererProvider {
 *     override fun register() {
 *         // 配置默认渲染器
 *         DiagnosticRendererRegistry.configureDefault {
 *             register(INVALID_BINARY_OPERATOR) {
 *                 message { CangJieDiagnosisBundle.rawMessage(it) }
 *                 parameterExtractor { diagnostic -> arrayOf(...) }
 *             }
 *         }
 *
 *         // 配置IDE渲染器
 *         DiagnosticRendererRegistry.configureIde {
 *             register(INVALID_BINARY_OPERATOR) {
 *                 message { IDECangJieDiagnosisBundle.rawMessage(it) }
 *                 parameterExtractor { diagnostic -> arrayOf(...) }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @see org.cangnova.cangjie.diagnostics.DiagnosticInitializerStartupActivity
 * @see CangJieDiagnosisBundle
 * @see IDECangJieDiagnosisBundle
 */
object DiagnosticRendererRegistry {
    /**
     * 按类型存储渲染器映射： type -> (factory -> renderer)
     */
    private val renderers = ConcurrentHashMap<String, ConcurrentHashMap<DiagnosticFactory<*>, DiagnosticRenderer<*>>>()
    private val LOG = logger<DiagnosticRendererRegistry>()

    @Volatile
    private var initialized = false

    /**
     * 默认渲染器类型
     */
    const val DEFAULT = "default"

    /**
     * IDE 渲染器类型
     */
    const val IDE = "ide"

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
     * @param type 渲染器类型（如 [DEFAULT]、[IDE] 或自定义类型）
     * @param bundle 消息Bundle
     * @param block 配置块
     */
    fun configure(
        type: String = DEFAULT,
        bundle: MessageBundle,
        block: DiagnosticRendererConfiguration.() -> Unit
    ) {
        val config = DiagnosticRendererConfiguration(bundle)
        config.block()
        val typeRenderers = renderers.computeIfAbsent(type) { ConcurrentHashMap() }
        config.applyTo(typeRenderers)
    }

    /**
     * 配置默认渲染器（便捷方法）
     *
     * @param block 配置块
     */
    fun configureDefault(block: DiagnosticRendererConfiguration.() -> Unit) {
        configure(DEFAULT, CangJieDiagnosisBundle, block)
    }

    /**
     * 配置 IDE 渲染器（便捷方法）
     *
     * @param block 配置块
     */
    fun configureIde(block: DiagnosticRendererConfiguration.() -> Unit) {
        configure(IDE, IDECangJieDiagnosisBundle, block)
    }

    /**
     * 注册渲染器
     *
     * @param type 渲染器类型
     * @param factory 诊断工厂
     * @param renderer 渲染器
     */
    fun <D : UnboundDiagnostic> register(
        type: String,
        factory: DiagnosticFactory<D>,
        renderer: DiagnosticRenderer<D>
    ) {
        val typeRenderers = renderers.computeIfAbsent(type) { ConcurrentHashMap() }
        typeRenderers[factory] = renderer
    }

    /**
     * 获取渲染器
     *
     * 优先级：
     * 1. 显式注册的渲染器（指定类型）
     * 2. 自动推断的渲染器（指定类型）
     * 3. 显式注册的渲染器（DEFAULT 类型，作为回退）
     * 4. 自动推断的渲染器（DEFAULT 类型，作为回退）
     * 5. 工厂的默认渲染器
     *
     * @param factory 诊断工厂
     * @param type 渲染器类型
     * @return 渲染器，如果未找到返回 null
     */
    fun <D : UnboundDiagnostic> getRenderer(
        factory: DiagnosticFactory<D>,
        type: String = DEFAULT
    ): DiagnosticRenderer<D>? {
        // 1. 查找显式注册的渲染器（指定类型）
        @Suppress("UNCHECKED_CAST")
        renderers[type]?.get(factory)?.let { return it as DiagnosticRenderer<D> }

        // 2. 自动推断渲染器（指定类型）
        val bundle = getBundleForType(type)
        val inferred = inferRenderer(factory, bundle)
        if (inferred != null) {
            // 缓存推断的渲染器
            val typeRenderers = renderers.computeIfAbsent(type) { ConcurrentHashMap() }
            typeRenderers[factory] = inferred
            return inferred
        }

        // 3. 回退到 DEFAULT 类型（如果不是 DEFAULT）
        if (type != DEFAULT) {
            @Suppress("UNCHECKED_CAST")
            renderers[DEFAULT]?.get(factory)?.let { return it as DiagnosticRenderer<D> }

            // 4. 自动推断 DEFAULT 渲染器
            val defaultInferred = inferRenderer(factory, CangJieDiagnosisBundle)
            if (defaultInferred != null) {
                val defaultRenderers = renderers.computeIfAbsent(DEFAULT) { ConcurrentHashMap() }
                defaultRenderers[factory] = defaultInferred
                return defaultInferred
            }
        }

        return null
    }

    /**
     * 渲染诊断
     *
     * @param diagnostic 诊断对象
     * @param type 渲染器类型
     * @return 渲染后的消息文本
     */
    fun render(
        diagnostic: UnboundDiagnostic,
        type: String = DEFAULT
    ): String {
        val renderer = getRenderer(diagnostic.factory, type)
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
     * 根据类型获取对应的Bundle
     *
     * @param type 渲染器类型
     * @return 对应的MessageBundle
     */
    private fun getBundleForType(type: String): MessageBundle {
        return when (type) {
            IDE -> IDECangJieDiagnosisBundle
            else -> CangJieDiagnosisBundle
        }
    }

    /**
     * 自动推断渲染器
     *
     * 根据诊断工厂类型和参数类型自动推断合适的渲染器
     *
     * @param factory 诊断工厂
     * @param bundle 消息Bundle
     * @return 推断的渲染器，如果无法推断返回 null
     */
    @Suppress("UNCHECKED_CAST")
    private fun <D : UnboundDiagnostic> inferRenderer(
        factory: DiagnosticFactory<D>,
        bundle: MessageBundle
    ): DiagnosticRenderer<D>? {
        return when (factory) {
            is DiagnosticFactory0<*> -> inferRenderer0(factory as DiagnosticFactory0<*>, bundle)
            is DiagnosticFactory1<*, *> -> inferRenderer1(factory as DiagnosticFactory1<*, *>, bundle)
            is DiagnosticFactory2<*, *, *> -> inferRenderer2(factory as DiagnosticFactory2<*, *, *>, bundle)
            is DiagnosticFactory3<*, *, *, *> -> inferRenderer3(factory as DiagnosticFactory3<*, *, *, *>, bundle)
            is DiagnosticFactory4<*, *, *, *, *> -> inferRenderer4(factory as DiagnosticFactory4<*, *, *, *, *>, bundle)
            else -> null
        } as? DiagnosticRenderer<D>
    }

    /**
     * 推断无参数诊断的渲染器
     */
    private fun <E : PsiElement> inferRenderer0(
        factory: DiagnosticFactory0<E>,
        bundle: MessageBundle
    ): DiagnosticRenderer<*> {
        return SimpleDiagnosticRenderer {
            bundle.getMessage(factory.name)
        }
    }

    /**
     * 推断单参数诊断的渲染器
     *
     * 使用默认渲染器（TO_STRING），复杂情况需要显式配置
     */
    @Suppress("UNCHECKED_CAST")
    private fun <E : PsiElement, A : Any> inferRenderer1(
        factory: DiagnosticFactory1<E, A>,
        bundle: MessageBundle
    ): DiagnosticRenderer<*> {
        return DiagnosticWithParameters1Renderer(
            message = { bundle.getMessage(factory.name) },
            rendererForA = Renderers.TO_STRING as DiagnosticParameterRenderer<A>
        )
    }

    /**
     * 推断双参数诊断的渲染器
     *
     * 使用默认渲染器（TO_STRING），复杂情况需要显式配置
     */
    @Suppress("UNCHECKED_CAST")
    private fun <E : PsiElement, A, B> inferRenderer2(
        factory: DiagnosticFactory2<E, A, B>,
        bundle: MessageBundle
    ): DiagnosticRenderer<*> {
        return DiagnosticWithParameters2Renderer(
            message = { bundle.getMessage(factory.name) },
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
        factory: DiagnosticFactory3<E, A, B, C>,
        bundle: MessageBundle
    ): DiagnosticRenderer<*> {
        return DiagnosticWithParameters3Renderer(
            message = { bundle.getMessage(factory.name) },
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
        factory: DiagnosticFactory4<E, A, B, C, D>,
        bundle: MessageBundle
    ): DiagnosticRenderer<*> {
        return DiagnosticWithParameters4Renderer(
            message = { bundle.getMessage(factory.name) },
            rendererForA = Renderers.TO_STRING as DiagnosticParameterRenderer<A>,
            rendererForB = Renderers.TO_STRING as DiagnosticParameterRenderer<B>,
            rendererForC = Renderers.TO_STRING as DiagnosticParameterRenderer<C>,
            rendererForD = Renderers.TO_STRING as DiagnosticParameterRenderer<D>
        )
    }
}
