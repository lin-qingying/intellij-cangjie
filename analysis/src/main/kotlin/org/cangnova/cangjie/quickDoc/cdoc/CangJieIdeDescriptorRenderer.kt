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

package org.cangnova.cangjie.quickDoc.cdoc

import org.cangnova.cangjie.builtins.*
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import org.cangnova.cangjie.descriptors.annotations.AnnotationUseSiteTarget
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.descriptors.impl.PropertyAccessorDescriptor
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.renderer.*
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.constants.ArrayValue
import org.cangnova.cangjie.resolve.constants.ConstantValue
import org.cangnova.cangjie.resolve.declaresOrInheritsDefaultValue
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.TypeUtils.CANNOT_INFER_FUNCTION_PARAM_TYPE
import org.cangnova.cangjie.types.error.ErrorType
import org.cangnova.cangjie.utils.toLowerCaseAsciiOnly
import java.lang.reflect.Modifier
import java.util.*
import kotlin.jvm.internal.PropertyReference1Impl
import kotlin.properties.Delegates
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty


/**
 * 扩展的分类器名称策略接口。
 *
 * 该接口扩展了 [ClassifierNamePolicy]，添加了带类型信息的渲染方法。
 * 这允许渲染器根据类型的特性（如是否为非 Option 类型）来定制输出。
 *
 * ## 使用场景
 *
 * 在生成文档时，某些类型需要特殊标记。例如，非 Option 类型可能需要显示 ` & Any` 后缀。
 *
 * @see ClassifierNamePolicy
 * @see org.cangnova.cangjie.quickDoc.HtmlClassifierNamePolicy
 */
interface ClassifierNamePolicyEx : ClassifierNamePolicy {

    /**
     * 渲染带类型信息的分类器名称。
     *
     * @param classifier 分类器描述符
     * @param renderer 描述符渲染器
     * @param type 类型信息，用于特殊格式化
     * @return 渲染后的名称字符串
     */
    fun renderClassifierWithType(
        classifier: ClassifierDescriptor,
        renderer: DescriptorRenderer,
        type: CangJieType
    ): String
}

/**
 * 源码限定名称策略。
 *
 * 该对象提供与源代码一致的限定名称渲染策略，类似于 Kotlin 的 [ClassifierNamePolicy.SOURCE_CODE_QUALIFIED]。
 * 对于局部声明，会限定到函数作用域级别。
 *
 * ## 渲染规则
 *
 * - **类型参数**: 直接使用名称，不添加限定符
 * - **类描述符**: 使用完全限定名（包名.类名）
 * - **包级声明**: 使用包的完全限定名
 * - **非 Option 类型**: 在名称后添加 ` & Any` 标记
 *
 * ## 示例
 *
 * ```
 * std.collections.List          // 包级类
 * MyClass.InnerClass             // 内部类
 * T                              // 类型参数
 * SomeType & Any                 // 非 Option 类型
 * ```
 *
 * @see ClassifierNamePolicyEx
 */
object SourceCodeQualified : ClassifierNamePolicyEx {

    override fun renderClassifierWithType(
        classifier: ClassifierDescriptor,
        renderer: DescriptorRenderer,
        type: CangJieType
    ): String =
        qualifiedNameForSourceCode(classifier, type)

    override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String =
        qualifiedNameForSourceCode(classifier)

    private fun qualifiedNameForSourceCode(descriptor: ClassifierDescriptor, type: CangJieType? = null): String {
        val nameString =
            descriptor.name.render() + (type?.takeIf { it.isDefinitelyNonOptionType }?.let { " & Any" } ?: "")
        if (descriptor is TypeParameterDescriptor) {
            return nameString
        }
        val qualifier = qualifierName(descriptor.containingDeclaration)
        return if (qualifier != null && qualifier != "") "$qualifier.$nameString" else nameString
    }

    /**
     * 获取描述符的限定符名称。
     *
     * @param descriptor 声明描述符
     * @return 限定符字符串；如果不适用则返回 `null`
     */
    private fun qualifierName(descriptor: DeclarationDescriptor): String? = when (descriptor) {
        is ClassDescriptor -> qualifiedNameForSourceCode(descriptor)
        is PackageFragmentDescriptor -> descriptor.fqName.toUnsafe().render()
        else -> null
    }
}

/**
 * 仓颉 IDE 描述符渲染器的配置选项。
 *
 * 该类扩展了 [DescriptorRendererOptions]，提供了 IDE 特定的渲染配置选项。
 * 使用可锁定的属性委托模式，确保配置在使用前被锁定，防止意外修改。
 *
 * ## 核心特性
 *
 * 1. **属性锁定机制**: 通过 [lock] 方法锁定配置，防止修改
 * 2. **属性委托**: 使用 [property] 方法创建可验证的属性
 * 3. **可复制**: 通过 [copy] 方法创建配置副本
 *
 * ## 使用方式
 *
 * ```kotlin
 * val options = CangJieIdeDescriptorOptions()
 * options.bold = true
 * options.classifierNamePolicy = SourceCodeQualified
 * options.lock()  // 锁定后不可修改
 * ```
 *
 * ## IDE 特有配置
 *
 * - [bold]: 是否加粗显示
 * - [highlightingManager]: 语法高亮管理器
 * - [dataClassWithPrimaryConstructor]: 数据类是否显示主构造函数
 *
 * @see DescriptorRendererOptions
 * @see CangJieIdeDescriptorRenderer
 */
open class CangJieIdeDescriptorOptions : DescriptorRendererOptions {

    /**
     * 配置是否已锁定。
     *
     * 锁定后，所有尝试修改配置的操作都会抛出异常。
     */
    var isLocked: Boolean = false
        private set

    /**
     * 锁定配置，防止后续修改。
     *
     * @throws IllegalStateException 如果配置已经被锁定
     */
    fun lock() {
        check(!isLocked) { "options have been already locked to prevent mutability" }
        isLocked = true
    }

    /**
     * 创建一个可验证的属性委托。
     *
     * 该方法返回的属性会在修改时检查锁定状态，如果已锁定则拒绝修改。
     *
     * @param T 属性类型
     * @param initialValue 初始值
     * @return 属性委托对象
     */
    protected fun <T> property(initialValue: T): ReadWriteProperty<CangJieIdeDescriptorOptions, T> {
        return Delegates.vetoable(initialValue) { _, _, _ ->
            check(!isLocked) { "Cannot modify readonly DescriptorRendererOptions" }
            true
        }
    }

    /**
     * 复制当前配置对象。
     *
     * 该方法使用反射机制复制所有属性值到新的配置对象。
     * 新对象未被锁定，可以继续修改。
     *
     * @return 配置对象的副本
     */
    fun copy(): CangJieIdeDescriptorOptions {
        val copy = CangJieIdeDescriptorOptions()

        //TODO: use Kotlin reflection
        for (field in this::class.java.declaredFields) {
            if (field.modifiers.and(Modifier.STATIC) != 0) continue
            field.isAccessible = true
            val property = field.get(this) as? ObservableProperty<*> ?: continue
            assert(!field.name.startsWith("is")) { "Fields named is* are not supported here yet" }
            val value = property.getValue(
                this,
                PropertyReference1Impl(
                    CangJieIdeDescriptorOptions::class,
                    field.name,
                    "get" + field.name.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(
                            Locale.getDefault()
                        ) else it.toString()
                    })
            )
            field.set(copy, copy.property(value))
        }

        return copy
    }

    // IDE 特有配置

    /** 是否对元素名称使用粗体显示（HTML 模式）*/
    var bold by property(false)

    /** 数据类是否显示主构造函数 */
    var dataClassWithPrimaryConstructor by property(true)

    /** 语法高亮管理器，用于给不同的语法元素添加颜色 */
    var highlightingManager by property(CangJieIdeDescriptorRendererHighlightingManager.NO_HIGHLIGHTING)

    // 继承自 DescriptorRendererOptions 的标准配置

    /** 分类器名称策略，默认使用源码限定名称 */
    override var classifierNamePolicy: ClassifierNamePolicy by property(SourceCodeQualified)

    /** 是否显示"定义于"信息 */
    override var withDefinedIn by property(true)

    /** 是否显示顶层声明的源文件信息 */
    override var withSourceFileForTopLevel by property(true)

    /** 要渲染的修饰符集合（可见性、模态性等）*/
    override var modifiers: Set<DescriptorRendererModifier> by property(DescriptorRendererModifier.ALL_EXCEPT_ANNOTATIONS)

    /** 是否从名称开始渲染（跳过修饰符等）*/
    override var startFromName by property(false)

    /** 是否从声明关键字开始渲染 */
    override var startFromDeclarationKeyword by property(false)

    /** 是否启用调试模式（显示额外的调试信息）*/
    override var debugMode by property(false)

    /** 是否显示类的主构造函数 */
    override var classWithPrimaryConstructor by property(false)

    /** 是否显示详细信息（参数索引、重写数量等）*/
    override var verbose by property(false)

    /** 是否渲染 Unit 返回类型 */
    override var unitReturnType by property(true)

    /** 是否省略返回类型 */
    override var withoutReturnType by property(false)

    /** 是否使用增强类型（平台类型等）*/
    override var enhancedTypes by property(false)

    /** 是否规范化可见性（将内部可见性转换为标准形式）*/
    override var normalizedVisibilities by property(false)

    /** 是否渲染默认可见性（如 public）*/
    override var renderDefaultVisibility by property(true)

    /** 是否渲染默认模态性（如 final）*/
    override var renderDefaultModality by property(true)

    /** 是否渲染构造函数委托（this/super 调用）*/
    override var renderConstructorDelegation by property(false)

    /** 是否将主构造函数参数渲染为属性 */
    override var renderPrimaryConstructorParametersAsProperties by property(false)

    /** 主构造函数中是否显示 actual 属性 */
    override var actualPropertiesInPrimaryConstructor: Boolean by property(false)

    /** 是否将未推断的类型参数显示为名称 */
    override var uninferredTypeParameterAsName by property(false)

    /** 是否包含属性常量值 */
    override var includePropertyConstant by property(false)

    /** 是否省略类型参数 */
    override var withoutTypeParameters by property(false)

    /** 是否省略超类型 */
    override var withoutSuperTypes by property(false)

    /** 类型规范化器，用于转换类型 */
    override var typeNormalizer by property<(CangJieType) -> CangJieType> { it }

    /** 默认参数值渲染器 */
    override var defaultParameterValueRenderer by property<((ValueParameterDescriptor) -> String)?> { "..." }

    /** 是否将次构造函数渲染为主构造函数样式 */
    override var secondaryConstructorsAsPrimary by property(true)

    /** override 关键字渲染策略 */
    override var overrideRenderingPolicy by property(OverrideRenderingPolicy.RENDER_OPEN)

    /** 值参数处理器，用于自定义参数列表格式 */
    override var valueParametersHandler: DescriptorRenderer.ValueParametersHandler by property(DescriptorRenderer.ValueParametersHandler.DEFAULT)

    /** 文本格式（纯文本或 HTML）*/
    override var textFormat by property(RenderingFormat.PLAIN)

    /** 参数名称渲染策略 */
    override var parameterNameRenderingPolicy by property(ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED)

    /** 是否将接收者放在名称后面 */
    override var receiverAfterName by property(false)

    /** 是否渲染伴生对象名称 */
    override var renderCompanionObjectName by property(false)

    /** 属性访问器渲染策略 */
    override var propertyAccessorRenderingPolicy by property(PropertyAccessorRenderingPolicy.DEBUG)

    /** 属性常量渲染器 */
    override var propertyConstantRenderer: ((ConstantValue<*>) -> String?)? by property(null)

    /** 是否渲染默认的注解参数 */
    override var renderDefaultAnnotationArguments by property(false)

    /** 是否每个注解独占一行 */
    override var eachAnnotationOnNewLine by property(false)

    /** 要排除的注解类 */
    override var excludedAnnotationClasses by property(emptySet<FqName>())

    /** 要排除的类型注解类 */
    override var excludedTypeAnnotationClasses by property(ExcludedTypeAnnotations.internalAnnotationsForResolve)

    /** 注解过滤器 */
    override var annotationFilter: ((AnnotationDescriptor) -> Boolean)? by property(null)

    /** 注解参数渲染策略 */
    override var annotationArgumentsRenderingPolicy by property(AnnotationArgumentsRenderingPolicy.NO_ARGUMENTS)

    /** 是否总是渲染修饰符 */
    override var alwaysRenderModifiers by property(false)

    /** 是否渲染 constructor 关键字 */
    override var renderConstructorKeyword by property(true)

    /** 是否渲染非缩写类型 */
    override var renderUnabbreviatedType by property(true)

    /** 是否渲染类型展开 */
    override var renderTypeExpansions by property(false)

    /** 是否渲染缩写类型注释 */
    override var renderAbbreviatedTypeComments by property(false)

    /** 是否包含额外的修饰符 */
    override var includeAdditionalModifiers by property(true)

    /** 函数类型中是否显示参数名称 */
    override var parameterNamesInFunctionalTypes by property(true)

    /** 是否渲染函数契约 */
    override var renderFunctionContracts by property(false)

    /** 是否以易读方式显示未解析的类型 */
    override var presentableUnresolvedTypes by property(false)

    /** HTML 模式下是否只对名称加粗 */
    override var boldOnlyForNamesInHtml by property(false)

    /** 是否显示详细的错误类型信息 */
    override var informativeErrorType by property(true)
}

/**
 * 仓颉语言的 IDE 描述符渲染器。
 *
 * 该类负责将仓颉语言的描述符（类、函数、属性等）转换为人类可读的字符串表示，
 * 主要用于 IDE 的快速文档、代码补全提示等场景。
 *
 * ## 核心功能
 *
 * 1. **类型渲染**: 将类型描述符转换为源码形式（支持泛型、函数类型、Option 类型等）
 * 2. **声明渲染**: 渲染类、函数、属性、构造函数等各种声明
 * 3. **语法高亮**: 通过 [CangJieIdeDescriptorRendererHighlightingManager] 提供语法高亮
 * 4. **格式化输出**: 支持纯文本和 HTML 两种输出格式
 *
 * ## 使用方式
 *
 * ```kotlin
 * val renderer = CangJieIdeDescriptorRenderer.withOptions {
 *     textFormat = RenderingFormat.HTML
 *     classifierNamePolicy = HtmlClassifierNamePolicy(ClassifierNamePolicy.SHORT)
 *     highlightingManager = createHighlightingManager(project)
 * }
 * val description = renderer.render(functionDescriptor)
 * ```
 *
 * ## 类型渲染示例
 *
 * ```
 * func foo(x: Int32, y: String): Bool           // 普通函数
 * func<T>(value: T): ?T                          // 泛型函数
 * (Int32, String) -> Bool                        // 函数类型
 * Array<Int32, 10>                               // 值数组类型
 * ?String                                        // Option 类型
 * SomeType & Any                                 // 非 Option 类型
 * ```
 *
 * ## 架构设计
 *
 * - 继承自 [DescriptorRenderer]，实现核心渲染逻辑
 * - 使用访问者模式（[RenderDeclarationDescriptorVisitor]）处理不同类型的描述符
 * - 通过 [options] 配置各种渲染行为
 * - 支持嵌套渲染（如函数类型中的接收者类型）
 *
 * @property options 渲染配置选项
 *
 * @see DescriptorRenderer
 * @see CangJieIdeDescriptorOptions
 * @see CangJieIdeDescriptorRendererHighlightingManager
 */
open class CangJieIdeDescriptorRenderer(
    open val options: CangJieIdeDescriptorOptions
) : DescriptorRenderer(), DescriptorRendererOptions by options {
    /**
     * 判断注解是否为参数名称注解。
     *
     * @receiver AnnotationDescriptor 注解描述符
     * @return 如果是 `@ParameterName` 注解则返回 `true`
     */
    protected fun AnnotationDescriptor.isParameterName(): Boolean {
        return fqName == StandardNames.FqNames.parameterName
    }

    /**
     * 判断类型是否包含修饰符或注解。
     *
     * @receiver CangJieType 类型
     * @return 如果类型包含注解则返回 `true`
     */
    protected fun CangJieType.hasModifiersOrAnnotations() =
        !annotations.isEmpty()

    /**
     * 渲染箭头符号（用于函数类型）。
     *
     * @return 高亮显示的箭头字符串 `->`
     */
    protected fun arrow(): String {
        return highlight(escape("->")) { asArrow }
    }

    /**
     * 使用新的 IDE 选项创建渲染器副本。
     *
     * 该方法会复制当前渲染器的配置，应用新的选项，然后锁定配置并返回新的渲染器实例。
     *
     * @param changeOptions 配置修改函数
     * @return 新的渲染器实例
     */
    fun withIdeOptions(changeOptions: CangJieIdeDescriptorOptions.() -> Unit): CangJieIdeDescriptorRenderer {
        val options = this.options.copy()
        options.changeOptions()
        options.lock()
        return CangJieIdeDescriptorRenderer(options)
    }

    /**
     * 伴生对象，包含渲染器工厂方法。
     */
    companion object {
        /**
         * 使用配置选项创建渲染器。
         *
         * 这是创建渲染器的推荐方式。
         *
         * ## 示例
         *
         * ```kotlin
         * val renderer = CangJieIdeDescriptorRenderer.withOptions {
         *     textFormat = RenderingFormat.HTML
         *     classifierNamePolicy = HtmlClassifierNamePolicy(ClassifierNamePolicy.SHORT)
         * }
         * ```
         *
         * @param changeOptions 配置修改函数
         * @return 新的渲染器实例
         */
        fun withOptions(changeOptions: CangJieIdeDescriptorOptions.() -> Unit): CangJieIdeDescriptorRenderer {
            val options = CangJieIdeDescriptorOptions()
            options.changeOptions()
            options.lock()
            return CangJieIdeDescriptorRenderer(options)
        }

    }

    /**
     * 可覆盖的高亮管理器。
     *
     * 如果未设置，则使用 [options] 中的高亮管理器。
     */
    private var overriddenHighlightingManager: CangJieIdeDescriptorRendererHighlightingManager<CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes>? =
        null
        get() = field ?: options.highlightingManager

    /**
     * 使用指定属性高亮显示值。
     *
     * @param value 要高亮的字符串
     * @param attributesBuilder 属性构建器函数
     * @return 高亮后的字符串
     */
    private fun highlight(
        value: String,
        attributesBuilder: CangJieIdeDescriptorRendererHighlightingManager<CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes>.() -> CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes
    ): String {
        return with(overriddenHighlightingManager!!) { buildString { appendHighlighted(value, attributesBuilder()) } }
    }

    /**
     * 渲染消息文本（如注释、提示信息等）。
     *
     * 在 HTML 模式下会使用斜体显示。
     *
     * @param message 消息字符串
     * @return 格式化后的消息
     */
    override fun renderMessage(message: String): String {
        val highlighted = highlight(message) { asInfo }
        return when (textFormat) {
            RenderingFormat.PLAIN -> highlighted
            RenderingFormat.HTML -> "<i>$highlighted</i>"
        }
    }

    /**
     * 渲染类型为字符串。
     *
     * 这是类型渲染的主入口方法，会对类型进行规范化后再渲染。
     *
     * @param type 要渲染的类型
     * @return 类型的字符串表示
     */
    override fun renderType(type: CangJieType): String = buildString {
        appendNormalizedType(typeNormalizer(type))
    }

    /**
     * 将类型按原样追加到字符串构建器，不进行额外处理
     *
     * 根据解包后的类型分发到具体的渲染方法:
     * - VArrayType: 变长数组类型
     * - BasicType: 基本类型（如 Int32, Bool 等）
     * - FlexibleType: 灵活类型
     * - SimpleType: 简单类型
     *
     * @param type 要渲染的类型
     */
    private fun StringBuilder.appendNormalizedTypeAsIs(type: CangJieType) {
        if (type is WrappedType && debugMode && !type.isComputed()) {
            appendHighlighted("<Not computed yet>") { asInfo }
            return
        }
        when (val unwrappedType = type.unwrap()) {
            is VArrayType -> appendVArrayType(unwrappedType)
            is BasicType -> appendHighlighted(unwrappedType.typeName.toString()) { asKeyword }

            is FlexibleType -> append(
                unwrappedType.render(
                    this@CangJieIdeDescriptorRenderer,
                    this@CangJieIdeDescriptorRenderer
                )
            )

            is SimpleType -> appendSimpleType(unwrappedType)
        }
    }

    /**
     * 渲染变长数组类型
     *
     * 格式: `Array<元素类型, 大小>`
     *
     * @param vArrayType 变长数组类型
     */
    private fun StringBuilder.appendVArrayType(vArrayType: VArrayType) {
        appendHighlighted(vArrayType.typeName) { asKeyword }
        appendHighlighted(renderTypeArguments(vArrayType.arguments) {
            it.append(",")

            it.append(vArrayType.size)
        }) { asError }


    }

    protected fun shouldRenderAsPrettyFunctionType(type: CangJieType): Boolean {
        return type.isBuiltinFunctionalType
    }

    protected fun renderError(message: String): String {
        val highlighted = highlight(message) { asError }
        return when (textFormat) {
            RenderingFormat.PLAIN -> highlighted
            RenderingFormat.HTML -> if (options.bold) "<b>$highlighted</b>" else highlighted
        }
    }

    /**
     * 渲染简单类型
     *
     * 处理以下情况:
     * - 无法推断的函数参数类型 → `???`
     * - 占位符类型 → `???`
     * - 未推断的类型变量 → 根据配置渲染名称或 `???`
     * - 错误类型 → 使用默认类型渲染
     * - 函数类型 → 使用函数类型语法
     * - 缩写类型 → 展开或使用缩写形式
     * - 其他类型 → 渲染类型构造器和参数
     *
     * @param type 要渲染的简单类型
     */
    private fun StringBuilder.appendSimpleType(type: SimpleType) {
        if (type == CANNOT_INFER_FUNCTION_PARAM_TYPE || TypeUtils.isDontCarePlaceholder(type)) {
            appendHighlighted("???") { asError }
            return
        }
        if (ErrorUtils.isUninferredTypeVariable(type)) {
            if (uninferredTypeParameterAsName) {
                append(renderError((type.constructor as ErrorTypeConstructor).getParam(0)))
            } else {
                appendHighlighted("???") { asError }
            }
            return
        }

        if (type.isError) {
            appendDefaultType(type)
            return
        }
        if (shouldRenderAsPrettyFunctionType(type)) {
            appendFunctionType(type)
        } else {
            appendDefaultType(type)
        }
    }

    /**
     * 渲染关键字
     *
     * 关键字会被高亮显示，并根据文本格式可能添加粗体样式
     *
     * @param keyword 要渲染的关键字
     * @return 高亮后的关键字字符串
     */
    private fun renderKeyword(keyword: String): String {
        val highlighted = highlight(keyword) { asKeyword }
        return when (textFormat) {
            RenderingFormat.PLAIN -> highlighted
            RenderingFormat.HTML -> if (options.bold && !boldOnlyForNamesInHtml) "<b>$highlighted</b>" else highlighted
        }
    }

    /**
     * 添加修饰符到字符串构建器
     *
     * 只有当 value 为 true 时才会添加修饰符关键字和空格
     *
     * @param value 是否添加修饰符
     * @param modifier 修饰符关键字
     */
    private fun StringBuilder.appendModifier(value: Boolean, modifier: String) {
        if (value) {
            append(renderKeyword(modifier))
            append(" ")
        }
    }

    /**
     * 渲染函数类型
     *
     * 函数类型格式: `(参数类型列表) -> 返回类型`
     *
     * 示例:
     * - `(Int32) -> Bool` - 单参数函数
     * - `(Int32, String) -> Unit` - 多参数函数
     * - `() -> Int32` - 无参数函数
     *
     * @param type 要渲染的函数类型
     */
    private fun StringBuilder.appendFunctionType(type: CangJieType) {
        val lengthBefore = length
        // 仓颉不需要跳过扩展函数类型注解（因为没有这个特性）
//        with(functionTypeAnnotationsRenderer) {
//            appendAnnotations(type)
//        }
        val hasAnnotations = length != lengthBefore

        val isNullable = type.isOption

        // 仓颉没有扩展函数类型的接收器，简化括号逻辑
        val needParenthesis = isNullable
        if (needParenthesis) {
            if (hasAnnotations) {
                assert(last().isWhitespace())
                if (get(lastIndex - 1) != ')') {
                    insert(lastIndex, highlight("()") { asParentheses })
                }
            }
            appendHighlighted("(") { asParentheses }
        }

        appendHighlighted("(") { asParentheses }

        val parameterTypes = type.getValueParameterTypesFromFunctionType()
        for ((index, typeProjection) in parameterTypes.withIndex()) {
            if (index > 0) appendHighlighted(", ") { asComma }

            val name =
                if (parameterNamesInFunctionalTypes) typeProjection.type.extractParameterNameFromFunctionTypeArgument() else null
            if (name != null) {
                appendHighlighted(renderName(name, false)) { asParameter }
                appendHighlighted(": ") { asColon }
            }

            append(renderTypeProjection(typeProjection))
        }

        appendHighlighted(") ") { asParentheses }
        append(arrow())
        append(" ")
        appendNormalizedType(type.getReturnTypeFromFunctionType())

        if (needParenthesis) appendHighlighted(")") { asParentheses }

        if (isNullable) appendHighlighted("?") { asNullityMarker }
    }

    //    private fun StringBuilder.appendTypeConstructorAndArguments(
//        type: CangJieType,
//        typeConstructor: TypeConstructor = type.constructor
//    ) {
//        val possiblyInnerType = type.buildPossiblyInnerType()
//        if (possiblyInnerType == null) {
//            append(renderTypeConstructorOfType(typeConstructor, type))
//            append(renderTypeArguments(type.arguments))
//            return
//        }
//
//        appendPossiblyInnerType(possiblyInnerType)
//    }

    private fun StringBuilder.appendPossiblyInnerType(possiblyInnerType: PossiblyInnerType) {
        possiblyInnerType.outerType?.let {
            appendPossiblyInnerType(it)
            appendHighlighted(".") { asDot }
            appendHighlighted(renderName(possiblyInnerType.classifierDescriptor.name, false)) { asClassName }
        } ?: append(renderTypeConstructor(possiblyInnerType.classifierDescriptor.typeConstructor))

        append(renderTypeArguments(possiblyInnerType.arguments))
    }

    private fun StringBuilder.appendTypeConstructorAndArguments(
        type: CangJieType,
        typeConstructor: TypeConstructor = type.constructor
    ) {
//        if (type is OptionType) {
//            append(renderTypeConstructorOfType(type.getType().constructor, type.getType()))
//            append(renderTypeArguments(type.getType().arguments))
//        } else {
//            append(renderTypeConstructorOfType(typeConstructor, type))
//            append(renderTypeArguments(type.arguments))
//
//        }
        val possiblyInnerType = type.buildPossiblyInnerType()
        if (possiblyInnerType == null) {
            append(renderTypeConstructorOfType(typeConstructor, type))
            append(renderTypeArguments(type.arguments))
            return
        }

        appendPossiblyInnerType(possiblyInnerType)

    }

    private fun StringBuilder.appendDefaultType(type: CangJieType) {

        if (type is OptionType) {
            appendHighlighted("?") { asNullityMarker }
            appendTypeConstructorAndArguments(type.innerType)

        } else if (type.isError) {
            if (isUnresolvedType(type) && presentableUnresolvedTypes) {
                appendHighlighted(ErrorUtils.unresolvedTypeAsItIs(type)) { asError }
            } else {
                if (type is ErrorType && !informativeErrorType) {
                    appendHighlighted(type.debugMessage) { asError }
                } else {
                    appendHighlighted(type.constructor.toString()) { asError } // Debug name of an error type is more informative
                }
                appendHighlighted(renderTypeArguments(type.arguments)) { asError }
            }
        } else {
            appendTypeConstructorAndArguments(type)
        }


        if (classifierNamePolicy !is ClassifierNamePolicyEx) {
            if (type.isDefinitelyNonOptionType) {
                appendHighlighted(" & Any") { asNonNullAssertion }
            }
        }
    }

    private fun StringBuilder.appendHighlighted(
        value: String,
        attributesBuilder: CangJieIdeDescriptorRendererHighlightingManager<CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes>.() -> CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes
    ) {
        return with(overriddenHighlightingManager!!) {
            this@appendHighlighted.appendHighlighted(
                value,
                attributesBuilder()
            )
        }
    }

    private fun StringBuilder.appendNormalizedType(type: CangJieType) {
        val abbreviated = type.unwrap() as? AbbreviatedType
        if (abbreviated != null) {
            if (renderTypeExpansions) {
                appendNormalizedTypeAsIs(abbreviated.expandedType)
            } else {
                // TODO optionality is lost for abbreviated type?
                appendNormalizedTypeAsIs(abbreviated.abbreviation)
                if (renderUnabbreviatedType) {
                    appendAbbreviatedTypeExpansion(abbreviated)
                }
            }
            return
        }

        appendNormalizedTypeAsIs(type)
    }

    private inline fun <R> withNoHighlighting(action: () -> R): R {
        val old = overriddenHighlightingManager
        overriddenHighlightingManager = CangJieIdeDescriptorRendererHighlightingManager.NO_HIGHLIGHTING
        val result = action()
        overriddenHighlightingManager = old
        return result
    }

    protected fun escape(string: String) = textFormat.escape(string)

    override fun renderFqName(fqName: FqNameUnsafe) = renderFqName(fqName.pathSegments())

    private fun renderFqName(pathSegments: List<Name>): String {
        val rendered = buildString {
            for (element in pathSegments) {
                if (isNotEmpty()) {
                    appendHighlighted(".") { asDot }
                }
                appendHighlighted(element.render()) { asClassName }
            }
        }
        return escape(rendered)
    }

    private fun StringBuilder.appendAbbreviatedTypeExpansion(abbreviated: AbbreviatedType) {

        if (textFormat == RenderingFormat.HTML) {
            append("<i>")
        }
        val expandedType = withNoHighlighting { buildString { appendNormalizedTypeAsIs(abbreviated.expandedType) } }
        appendHighlighted(" /* = $expandedType */") { asInfo }
        if (textFormat == RenderingFormat.HTML) {
            append("</i></font>")
        }
    }

    override fun renderFlexibleType(lowerRendered: String, upperRendered: String, builtIns: CangJieBuiltIns): String {
        TODO("Not yet implemented")
    }

    protected fun lt() = highlight(escape("<")) { asOperationSign }
    protected fun gt() = highlight(escape(">")) { asOperationSign }
    private fun StringBuilder.appendTypeProjections(typeProjections: List<TypeProjection>) {
        typeProjections.joinTo(this, highlight(", ") { asComma }) {

            val type = renderType(it.type)
            if (it.projectionKind == Variance.INVARIANT) type
            else "${highlight(it.projectionKind.toString()) { asKeyword }} $type"

        }
    }

    override fun renderTypeArguments(typeArguments: List<TypeProjection>, other: (StringBuilder) -> Unit): String {
        return if (typeArguments.isEmpty()) ""
        else buildString {
            append(lt())
            appendTypeProjections(typeArguments)


            other(this)
            append(gt())
        }
    }

    override fun renderTypeProjection(typeProjection: TypeProjection): String = buildString {
        appendTypeProjections(listOf(typeProjection))
    }

    override fun renderTypeConstructor(typeConstructor: TypeConstructor): String =
        when (val cd = typeConstructor.declarationDescriptor) {
            is TypeParameterDescriptor -> highlight(renderClassifierName(cd)) { asTypeParameterName }
            is ClassDescriptor -> highlight(renderClassifierName(cd)) { asClassName }
            is TypeAliasDescriptor -> highlight(renderClassifierName(cd)) { asTypeAlias }
            null -> highlight(escape(typeConstructor.toString())) { asClassName }
            else -> error("Unexpected classifier: " + cd::class.java)
        }

    fun renderClassifierNameWithType(klass: ClassifierDescriptor, type: CangJieType): String =
        if (ErrorUtils.isError(klass)) {
            klass.typeConstructor.toString()
        } else {
            val policy = classifierNamePolicy
            if (policy is ClassifierNamePolicyEx) {
                policy.renderClassifierWithType(klass, this, type)
            } else {
                policy.renderClassifier(klass, this)
            }
        }

    private fun renderTypeConstructorOfType(typeConstructor: TypeConstructor, type: CangJieType): String =
        when (val cd = typeConstructor.declarationDescriptor) {
            is TypeParameterDescriptor -> highlight(renderClassifierNameWithType(cd, type)) { asTypeParameterName }
            is ClassDescriptor -> highlight(renderClassifierNameWithType(cd, type)) { asClassName }
            is TypeAliasDescriptor -> highlight(renderClassifierNameWithType(cd, type)) { asTypeAlias }
            null -> highlight(escape(typeConstructor.toString())) { asClassName }
            else -> error("Unexpected classifier: " + cd::class.java)
        }

    override fun renderClassifierName(cclass: ClassifierDescriptor): String = if (ErrorUtils.isError(cclass)) {
        cclass.typeConstructor.toString()
    } else
        classifierNamePolicy.renderClassifier(cclass, this)

    override fun renderAnnotation(annotation: AnnotationDescriptor, target: AnnotationUseSiteTarget?): String {
        return ""
    }

    private fun StringBuilder.appendDefinedIn(descriptor: DeclarationDescriptor) {
        if (descriptor is PackageFragmentDescriptor || descriptor is PackageViewDescriptor) {
            return
        }
        if (descriptor is ModuleDescriptor) {
            append(renderMessage(" is a module"))
            return
        }

        val containingDeclaration = descriptor.containingDeclaration
        if (containingDeclaration != null && containingDeclaration !is ModuleDescriptor) {
            append(renderMessage(" defined in "))
            val fqName = DescriptorUtils.getFqName(containingDeclaration)
            append(if (fqName.isRoot) "root package" else renderFqName(fqName))

            if (withSourceFileForTopLevel &&
                containingDeclaration is PackageFragmentDescriptor &&
                descriptor is DeclarationDescriptorWithSource
            ) {
                descriptor.source.containingFile.name?.let { sourceFileName ->
                    append(renderMessage(" in file "))
                    append(sourceFileName)
                }
            }
        }
    }

    private fun StringBuilder.appendTypeParameter(typeParameter: TypeParameterDescriptor, topLevel: Boolean) {
//        if (topLevel) {
//            append(lt())
//        }

        if (verbose) {
            appendHighlighted("/*${typeParameter.index}*/ ") { asInfo }
        }

        val variance = typeParameter.variance.label
        appendModifier(variance.isNotEmpty(), variance)

//        appendAnnotations(typeParameter)

        appendName(typeParameter, topLevel) { asTypeParameterName }
//        val upperBoundsCount = typeParameter.upperBounds.size
//        if ((upperBoundsCount > 1 && !topLevel) || upperBoundsCount == 1) {
//            val upperBound = typeParameter.upperBounds.iterator().next()
//            if (!CangJieBuiltIns.isDefaultBound(upperBound)) {
//                appendHighlighted(" : ") { asColon }
//                append(renderType(upperBound))
//            }
//        } else if (topLevel) {
//            var first = true
//            for (upperBound in typeParameter.upperBounds) {
//                if (CangJieBuiltIns.isDefaultBound(upperBound)) {
//                    continue
//                }
//                if (first) {
//                    appendHighlighted(" : ") { asColon }
//                } else {
//                    appendHighlighted(" & ") { asOperationSign }
//                }
//                append(renderType(upperBound))
//                first = false
//            }
//        } else {
//            // rendered with "where"
//        }

//        if (topLevel) {
//            append(gt())
//        }
    }

    private fun StringBuilder.appendTypeParameterList(typeParameters: List<TypeParameterDescriptor>) {
        val iterator = typeParameters.iterator()
        while (iterator.hasNext()) {
            val typeParameterDescriptor = iterator.next()
            appendTypeParameter(typeParameterDescriptor, false)
            if (iterator.hasNext()) {
                appendHighlighted(", ") { asComma }
            }
        }
    }

    private fun StringBuilder.appendTypeParameters(typeParameters: List<TypeParameterDescriptor>, withSpace: Boolean) {
        if (withoutTypeParameters) return

        if (typeParameters.isNotEmpty()) {
            append(lt())
            appendTypeParameterList(typeParameters)
            append(gt())
            if (withSpace) {
                append(" ")
            }
        }
    }

    private fun StringBuilder.appendVariable(property: VariableDescriptor) {
        if (!startFromName) {
            if (!startFromDeclarationKeyword) {

                appendVisibility(property.visibility)
                appendStatic(property.isStatic)

//                appendModifier(DescriptorRendererModifier.CONST in modifiers && property.isConst, "const")
                appendMemberModifiers(property)


            }
            appendLetVarPrefix(property)
            appendTypeParameters(property.typeParameters, true)
        }

        appendName(property, true) { asInstanceProperty }
        appendHighlighted(": ") { asColon }
        append(renderType(property.type))


        appendInitializer(property)

        appendWhereSuffix(property.typeParameters)
    }

    private fun StringBuilder.appendConst(isConst: Boolean) {
        if (isConst) {
            appendHighlighted("const ") { asConst }
        }
    }

    private fun StringBuilder.appendStatic(isStatic: Boolean) {
        if (isStatic) {
            appendHighlighted("static ") { asStatic }
        }
    }

    private fun StringBuilder.appendVariable(
        variable: VariableDescriptor,
        includeName: Boolean,
        topLevel: Boolean,
        isInPrimaryConstructor: Boolean = false
    ) {
        val realType = variable.type

        val varargElementType = (variable as? ValueParameterDescriptor)?.varargElementType
        val typeToRender = varargElementType ?: realType
        appendModifier(varargElementType != null, "vararg")

        if (isInPrimaryConstructor || topLevel && !startFromName) {
            appendLetVarPrefix(variable, isInPrimaryConstructor)
        }

        if (includeName) {
            appendName(variable, topLevel) { asLocalVarOrLet }
            appendHighlighted(": ") { asColon }
        }

        append(renderType(typeToRender))

        appendInitializer(variable)

        if (verbose && varargElementType != null) {
            val expandedType = withNoHighlighting { renderType(realType) }
            appendHighlighted(" /*${expandedType}*/") { asInfo }
        }
    }

    private fun StringBuilder.appendProperty(property: PropertyDescriptor) {
        if (!startFromName) {
            if (!startFromDeclarationKeyword) {
                appendPropertyAnnotations(property)
                appendVisibility(property.visibility)
                appendStatic(property.isStatic)
//                appendModifier(DescriptorRendererModifier.CONST in modifiers && property.isConst, "const")
                appendMemberModifiers(property)
                appendModalityForCallable(property)
                appendOverride(property)

                appendMemberKind(property)

            }

            appendMutPropPrefix(property)
            appendTypeParameters(property.typeParameters, true)
        }

        appendName(property, true) { asInstanceProperty }
        appendHighlighted(": ") { asColon }
        append(renderType(property.type))


        appendInitializer(property)

        appendWhereSuffix(property.typeParameters)
    }

    private fun StringBuilder.appendPropertyAnnotations(property: PropertyDescriptor) {
        if (DescriptorRendererModifier.ANNOTATIONS !in modifiers) return

//        appendAnnotations(property, eachAnnotationOnNewLine)

//        property.backingField?.let { appendAnnotations(it, target = AnnotationUseSiteTarget.FIELD) }
//        property.delegateField?.let { appendAnnotations(it, target = AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD) }

        if (propertyAccessorRenderingPolicy == PropertyAccessorRenderingPolicy.NONE) {
            property.getter?.let {
//                appendAnnotations(it, target = AnnotationUseSiteTarget.PROPERTY_GETTER)
            }
            property.setter?.let { setter ->
//                appendAnnotations(setter, target = AnnotationUseSiteTarget.PROPERTY_SETTER)
                setter.valueParameters.single().let {
//                    appendAnnotations(it, target = AnnotationUseSiteTarget.SETTER_PARAMETER)
                }
            }
        }
    }



    private fun StringBuilder.appendVisibility(visibility: DescriptorVisibility): Boolean {
        @Suppress("NAME_SHADOWING")
        var visibility = visibility
        if (DescriptorRendererModifier.VISIBILITY !in modifiers) return false
        if (normalizedVisibilities) {
            visibility = visibility.normalize()
        }
        if (!renderDefaultVisibility && visibility == DescriptorVisibilities.DEFAULT_VISIBILITY) return false
        append(renderKeyword(visibility.internalDisplayName))
        append(" ")
        return true
    }

    /**
     * 添加成员修饰符
     *
     * 当前实现为空，可能在将来添加 static, mut 等修饰符
     *
     * @param descriptor 成员描述符
     */
    private fun StringBuilder.appendMemberModifiers(descriptor: MemberDescriptor) {
    }

    /**
     * 添加 override 修饰符
     *
     * 只有在成员重写了父类/接口的成员时才会添加 override 关键字
     *
     * @param callableMember 可调用成员描述符
     */
    private fun StringBuilder.appendOverride(callableMember: CallableMemberDescriptor) {
        if (DescriptorRendererModifier.OVERRIDE !in modifiers) return
        if (overridesSomething(callableMember)) {
            if (overrideRenderingPolicy != OverrideRenderingPolicy.RENDER_OPEN) {
                appendModifier(true, "override")
                if (verbose) {
                    appendHighlighted("/*${callableMember.overriddenDescriptors.size}*/ ") { asInfo }
                }
            }
        }
    }

    /**
     * 判断成员是否重写了父类/接口成员
     *
     * @param callable 可调用成员描述符
     * @return 如果重写了父类/接口成员则返回 true
     */
    private fun overridesSomething(callable: CallableMemberDescriptor) = !callable.overriddenDescriptors.isEmpty()

    /**
     * 添加模态修饰符
     *
     * 模态包括: open, sealed, final 等
     * 如果模态是默认值且不渲染默认模态，则不添加
     *
     * @param modality 模态值
     * @param defaultModality 默认模态值
     */
    private fun StringBuilder.appendModality(modality: Modality, defaultModality: Modality) {
        if (!renderDefaultModality && modality == defaultModality) return
        appendModifier(DescriptorRendererModifier.MODALITY in modifiers, modality.name.toLowerCaseAsciiOnly())
    }

    /**
     * 计算成员描述符的隐式模态（不考虑扩展）
     *
     * 计算规则:
     * - 类描述符:
     *   - Interface 默认为 OPEN（天然可被实现）
     *   - 其他类默认为 FINAL
     * - 可调用成员:
     *   - 如果重写了父类成员且父类不是 final，则为 OPEN
     *   - 如果在 interface 中且不是 private，则为 ABSTRACT 或 OPEN
     *   - 其他情况为 FINAL
     *
     * @return 隐式模态
     */
    private fun MemberDescriptor.implicitModalityWithoutExtensions(): Modality {
        if (this is ClassDescriptor) {
            // 与编译器保持一致：Interface 默认为 OPEN (天然可被实现)
            return if (kind == ClassKind.INTERFACE) Modality.OPEN else Modality.FINAL
        }
        val containingClassDescriptor = containingDeclaration as? ClassDescriptor ?: return Modality.FINAL
        if (this !is CallableMemberDescriptor) return Modality.FINAL
        if (this.overriddenDescriptors.isNotEmpty()) {
            if (containingClassDescriptor.modality != Modality.FINAL) return Modality.OPEN
        }
        return if (containingClassDescriptor.kind == ClassKind.INTERFACE && this.visibility != DescriptorVisibilities.PRIVATE) {
            if (this.modality == Modality.ABSTRACT) Modality.ABSTRACT else Modality.OPEN
        } else
            Modality.FINAL
    }

    private fun StringBuilder.appendModalityForCallable(callable: CallableMemberDescriptor) {
        if (!DescriptorUtils.isTopLevelDeclaration(callable) || callable.modality != Modality.FINAL) {
            if (overrideRenderingPolicy == OverrideRenderingPolicy.RENDER_OVERRIDE && callable.modality == Modality.OPEN &&
                overridesSomething(callable)
            ) {
                return
            }
            if (callable.modality != Modality.FINAL) {
                appendModality(callable.modality, callable.implicitModalityWithoutExtensions())

            }
        }
    }

    private fun StringBuilder.appendMemberKind(callableMember: CallableMemberDescriptor) {
        if (DescriptorRendererModifier.MEMBER_KIND !in modifiers) return
        if (verbose && callableMember.kind != CallableMemberDescriptor.Kind.DECLARATION) {
            appendHighlighted("/*${callableMember.kind.name.toLowerCaseAsciiOnly()}*/ ") { asInfo }
        }
    }

    private fun StringBuilder.appendFunction(function: FunctionDescriptor) {
        if (!startFromName) {
            if (!startFromDeclarationKeyword) {
//                appendAnnotations(function, eachAnnotationOnNewLine)
                appendVisibility(function.visibility)
                appendModalityForCallable(function)

                if (includeAdditionalModifiers) {
                    appendMemberModifiers(function)
                }
                appendConst(function.isConst)
                appendStatic(function.isStatic)


                appendOverride(function)

//                if (includeAdditionalModifiers) {
//                    appendAdditionalModifiers(function)
//                } else {
//                    appendSuspendModifier(function)
//                }

                appendMemberKind(function)

                if (verbose) {
                    if (function.isHiddenToOvercomeSignatureClash) {
                        appendHighlighted("/*isHiddenToOvercomeSignatureClash*/ ") { asInfo }
                    }

                    if (function.isHiddenForResolutionEverywhereBesideSupercalls) {
                        appendHighlighted("/*isHiddenForResolutionEverywhereBesideSupercalls*/ ") { asInfo }
                    }
                }
            }



            if (function is MacroDescriptor) {
                append(renderKeyword("macro"))
            } else {
                append(renderKeyword("func"))
            }
            append(" ")

        }

        appendName(function, true) { asFunDeclaration }
        appendTypeParameters(function.typeParameters, true)
        appendValueParameters(function.valueParameters, function.hasSynthesizedParameterNames())


        val returnType = function.returnType
        if (!withoutReturnType && (unitReturnType || (returnType == null || !CangJieBuiltIns.isUnit(returnType)))) {
            appendHighlighted(": ") { asColon }
            append(if (returnType == null) highlight("[NULL]") { asError } else renderType(returnType))
        }

        appendWhereSuffix(function.typeParameters)
    }

    private fun StringBuilder.appendWhereSuffix(typeParameters: List<TypeParameterDescriptor>) {
        if (withoutTypeParameters) return

        val upperBoundStrings = ArrayList<String>(0)

        for (typeParameter in typeParameters) {
            typeParameter.upperBounds

                .mapTo(upperBoundStrings) {
                    buildString {
                        appendHighlighted(renderName(typeParameter.name, false)) { asTypeParameterName }
                        appendHighlighted(" <: ") { asColon }
                        append(renderType(it))
                    }
                }
        }

        if (upperBoundStrings.isNotEmpty()) {
            append(" ")
            append(renderKeyword("where"))
            append(" ")
            upperBoundStrings.joinTo(this, highlight(", ") { asComma })
        }
    }


    private fun StringBuilder.appendPackageView(packageView: PackageViewDescriptor) {
        appendPackageHeader(packageView.fqName, "package")
        if (debugMode) {
            appendHighlighted(" in context of ") { asInfo }
            appendName(packageView.module, false) { asPackageName }
        }
    }

    private fun StringBuilder.appendPackageHeader(fqName: FqName, fragmentOrView: String) {
        append(renderKeyword(fragmentOrView))
        val fqNameString = renderFqName(fqName.toUnsafe())
        if (fqNameString.isNotEmpty()) {
            append(" ")
            append(fqNameString)
        }
    }

    private fun StringBuilder.appendConstructor(constructor: ConstructorDescriptor) {
//        appendAnnotations(constructor)
        val visibilityRendered =
            (options.renderDefaultVisibility || constructor.constructedClass.modality != Modality.SEALED)
                    && appendVisibility(constructor.visibility)
        appendMemberKind(constructor)

        val constructorKeywordRendered = renderConstructorKeyword || !constructor.isPrimary || visibilityRendered
        if (constructorKeywordRendered) {
            append(renderKeyword("constructor"))
        }
        val classDescriptor = constructor.containingDeclaration
        if (secondaryConstructorsAsPrimary) {
            if (constructorKeywordRendered) {
                append(" ")
            }
            appendName(classDescriptor, true) { asClassName }
            appendTypeParameters(constructor.typeParameters, false)
        }

        appendValueParameters(constructor.valueParameters, constructor.hasSynthesizedParameterNames())

        if (renderConstructorDelegation && !constructor.isPrimary && classDescriptor is ClassDescriptor) {
            val primaryConstructor = classDescriptor.unsubstitutedPrimaryConstructor
            if (primaryConstructor != null) {
                val parametersWithoutDefault = primaryConstructor.valueParameters.filter {
                    !it.declaresDefaultValue && it.varargElementType == null
                }
                if (parametersWithoutDefault.isNotEmpty()) {
                    appendHighlighted(" : ") { asColon }
                    append(renderKeyword("this"))
                    append(
                        parametersWithoutDefault.joinToString(
                            prefix = highlight("(") { asParentheses },
                            separator = highlight(", ") { asComma },
                            postfix = highlight(")") { asParentheses }
                        ) { "" }
                    )
                }
            }
        }

        if (secondaryConstructorsAsPrimary) {
            appendWhereSuffix(constructor.typeParameters)
        }
    }

    /**
     * 渲染枚举构造器
     *
     * 枚举构造器可以是：
     * 1. 简单构造器（无关联值）：如 `Red`, `Green`
     * 2. 函数构造器（有关联值）：如 `Success(T)`, `Error(String)`
     */
    private fun StringBuilder.appendEnumConstructor(constructor: EnumConstructorDescriptor) {
        // 渲染 "enum constructor" 前缀并高亮
        append(renderKeyword("enum"))
        append(" ")
        append(renderKeyword("constructor"))
        append(" ")

        // 渲染构造器名称
        appendName(constructor, true) { asInstanceProperty  }

        // 如果有参数，渲染参数列表
        if (constructor.hasArguments) {
            appendValueParameters(constructor.valueParameters, true)
        }
    }


    /**
     * 添加类种类前缀关键字
     *
     * 根据描述符类型添加对应的关键字:
     * - class: `class`
     * - interface: `interface`
     * - struct: `struct`
     * - enum: `enum`
     *
     * @param cclass 类或枚举描述符
     */
    private fun StringBuilder.appendClassKindPrefix(cclass: ClassAndEnumDescriptor) {
        append(renderKeyword(getClassifierKindPrefix(cclass)))
    }



    /**
     * 渲染类或枚举描述符
     *
     * 完整的类声明包括:
     * 1. 可见性修饰符
     * 2. 模态修饰符（open/sealed/final）
     * 3. 成员修饰符（static/mut等）
     * 4. 类种类前缀（class/interface/struct/enum）
     * 5. 类名和类型参数
     * 6. 主构造器（如果有）
     * 7. 父类型列表
     *
     * @param cclass 类或枚举描述符
     */
    private fun StringBuilder.appendClass(cclass: ClassAndEnumDescriptor) {


        if (!startFromName) {
//            appendAnnotations(cclass, eachAnnotationOnNewLine)

            appendVisibility(cclass.visibility)

            if (!(cclass.kind == ClassKind.INTERFACE && cclass.modality == Modality.ABSTRACT) && cclass.modality != Modality.FINAL
            ) {
                appendModality(cclass.modality, cclass.implicitModalityWithoutExtensions())
            }
            appendMemberModifiers(cclass)


            appendClassKindPrefix(cclass)
        }



        append(" ")
        appendName(cclass, true) { asClassName }

        val typeParameters = cclass.declaredTypeParameters
        appendTypeParameters(typeParameters, false)


        appendSuperTypes(cclass, prefix = "\n    ")


        appendWhereSuffix(typeParameters)
    }

    private fun StringBuilder.appendSuperTypes(cclass: ClassAndEnumDescriptor, prefix: String = " ", indent: String = "    ") {
        if (withoutSuperTypes) return

        if (CangJieBuiltIns.isNothing(cclass.defaultType)) return

        val supertypes = cclass.typeConstructor.supertypes.toMutableList()

        if (supertypes.isEmpty() || supertypes.size == 1 && CangJieBuiltIns.isAny(
                supertypes.iterator().next()
            )
        ) return

        append(prefix)
        appendHighlighted("&lt;: ") { asLtColon }
        val separator = when {
            supertypes.size <= 3 -> ", "
            else -> ",\n$indent  "
        }
        supertypes.joinTo(this, highlight(separator) { asComma }) { renderType(it) }
    }

    private fun StringBuilder.appendTypeAlias(typeAlias: TypeAliasDescriptor) {
//        appendAnnotations(typeAlias, eachAnnotationOnNewLine)
        appendVisibility(typeAlias.visibility)
        appendMemberModifiers(typeAlias)
        append(renderKeyword("type"))
        append(" ")
        appendName(typeAlias, true) { asTypeAlias }

        appendTypeParameters(typeAlias.declaredTypeParameters, false)

        appendHighlighted(" = ") { asOperationSign }
        append(renderType(typeAlias.underlyingType))
    }

    private fun StringBuilder.appendAccessorModifiers(descriptor: PropertyAccessorDescriptor) {
        appendMemberModifiers(descriptor)
    }

    private fun StringBuilder.appendPackageFragment(fragment: PackageFragmentDescriptor) {
        appendPackageHeader(fragment.fqName, "package-fragment")
        if (debugMode) {
            appendHighlighted(" in ") { asInfo }
            appendName(fragment.containingDeclaration, false) { asPackageName }
        }
    }

    /**
     * 声明描述符渲染访问者。
     *
     * 该内部类实现了访问者模式，用于根据描述符的具体类型分发到对应的渲染方法。
     * 每种描述符类型都有专门的 visit 方法进行处理。
     *
     * ## 支持的描述符类型
     *
     * - **ValueParameterDescriptor**: 值参数
     * - **VariableDescriptor**: 变量
     * - **PropertyDescriptor**: 属性
     * - **PropertyGetterDescriptor / PropertySetterDescriptor**: 属性访问器
     * - **FunctionDescriptor**: 函数
     * - **ConstructorDescriptor**: 构造函数
     * - **ClassDescriptor**: 类
     * - **TypeAliasDescriptor**: 类型别名
     * - **TypeParameterDescriptor**: 类型参数
     * - **PackageFragmentDescriptor / PackageViewDescriptor**: 包
     * - **ModuleDescriptor**: 模块
     *
     * ## 工作原理
     *
     * 每个 visit 方法接收描述符和 StringBuilder，在 StringBuilder 中构建渲染结果。
     * 渲染过程中会调用各种 `appendXxx` 辅助方法来构建完整的输出。
     *
     * @see DeclarationDescriptorVisitor
     */
    private inner class RenderDeclarationDescriptorVisitor : DeclarationDescriptorVisitor<Unit, StringBuilder> {
        override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, builder: StringBuilder?) {
            builder?.appendValueParameter(descriptor, true, true)
        }

        override fun visitVariableDescriptor(descriptor: VariableDescriptor, builder: StringBuilder?) {
            builder?.appendVariable(descriptor)
        }

        override fun visitVariableDescriptorBase(descriptor: VariableDescriptor, builder: StringBuilder?) {

            builder?.appendVariable(descriptor)
        }

        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, builder: StringBuilder?) {
            builder?.appendProperty(descriptor)
        }

        override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, builder: StringBuilder?) {
            builder?.let { visitPropertyAccessorDescriptor(descriptor, it, "getter") }
        }

        override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, builder: StringBuilder?) {
            builder?.let { visitPropertyAccessorDescriptor(descriptor, it, "setter") }
        }

        private fun visitPropertyAccessorDescriptor(
            descriptor: PropertyAccessorDescriptor,
            builder: StringBuilder,
            kind: String
        ) {
            when (propertyAccessorRenderingPolicy) {
                PropertyAccessorRenderingPolicy.PRETTY -> {
                    builder.appendAccessorModifiers(descriptor)
                    builder.appendHighlighted("$kind for ") { asInfo }
                    builder.appendProperty(descriptor.correspondingProperty)
                }

                PropertyAccessorRenderingPolicy.DEBUG -> {
                    visitFunctionDescriptor(descriptor, builder)
                }

                PropertyAccessorRenderingPolicy.NONE -> {
                }
            }
        }

        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, builder: StringBuilder?) {
            builder?.appendFunction(descriptor)
        }

        override fun visitReceiverParameterDescriptor(
            descriptor: ReceiverParameterDescriptor,
            builder: StringBuilder?
        ) {
            builder?.append(descriptor.name) // renders <this>
        }

        /**
         * 访问并渲染枚举描述符
         *
         * 将枚举作为类进行渲染,包括枚举关键字、名称、构造器等
         *
         * @param descriptor 枚举描述符
         * @param builder 字符串构建器
         */
        override fun visitEnumDescriptor(
            descriptor: EnumDescriptor,
            builder: StringBuilder?
        ) {
            builder?.appendClass(descriptor)

        }

        /**
         * 访问并渲染枚举构造器描述符
         *
         * 渲染格式: `enum constructor 名称(参数列表)`
         *
         * @param descriptor 枚举构造器描述符
         * @param builder 字符串构建器
         */
        override fun visitEnumConstructorDescriptor(
            descriptor: EnumConstructorDescriptor,
            builder: StringBuilder?
        ) {
            builder?.appendEnumConstructor(descriptor)
        }

        override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, builder: StringBuilder?) {
            builder?.appendConstructor(constructorDescriptor)
        }

        override fun visitExtendDescriptor(
            descriptor: ExtendDescriptor,
            builder: StringBuilder?
        ) {
            TODO("Not yet implemented")
        }

        override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, builder: StringBuilder?) {
            builder?.appendTypeParameter(descriptor, true)
        }

        override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, builder: StringBuilder?) {
            builder?.appendPackageFragment(descriptor)
        }

        override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, builder: StringBuilder?) {
            builder?.appendPackageView(descriptor)
        }

        override fun visitModuleDeclaration(descriptor: ModuleDescriptor, builder: StringBuilder?) {
            builder?.appendName(descriptor, true) { asPackageName }
        }

        override fun visitClassDescriptor(descriptor: ClassDescriptor, builder: StringBuilder?) {
            builder?.appendClass(descriptor)
        }

        override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, builder: StringBuilder?) {
            builder?.appendTypeAlias(descriptor)
        }





    }


    /**
     * 渲染声明描述符为字符串。
     *
     * 这是渲染器的主入口方法，使用访问者模式分发到特定的渲染方法。
     *
     * ## 渲染内容
     *
     * 根据描述符类型，会渲染以下内容：
     * - **类**: 修饰符、类关键字、类名、类型参数、超类型
     * - **函数**: 修饰符、函数关键字、函数名、类型参数、参数列表、返回类型
     * - **属性**: 修饰符、属性关键字、属性名、类型
     * - **构造函数**: 修饰符、构造函数关键字、参数列表
     *
     * ## 示例输出
     *
     * ```
     * public class MyClass<T> <: BaseClass
     * public func foo(x: Int32, y: String): Bool
     * public prop name: String
     * constructor(value: Int32)
     * ```
     *
     * @param declarationDescriptor 声明描述符
     * @return 渲染后的字符串
     */
    override fun render(declarationDescriptor: DeclarationDescriptor): String {
        return buildString {
            declarationDescriptor.accept(RenderDeclarationDescriptorVisitor(), this)

            if (withDefinedIn) {
                appendDefinedIn(declarationDescriptor)
            }
        }
    }

    /**
     * 渲染值参数列表。
     *
     * @param parameters 值参数描述符集合
     * @param synthesizedParameterNames 是否为合成的参数名称
     * @return 渲染后的参数列表字符串
     */
    override fun renderValueParameters(
        parameters: Collection<ValueParameterDescriptor>,
        synthesizedParameterNames: Boolean
    ) = buildString {
        appendValueParameters(parameters, synthesizedParameterNames)
    }

    /**
     * 判断是否应该渲染参数名称
     *
     * 根据参数名称渲染策略和是否为合成参数名称决定:
     * - ALL: 总是渲染参数名称
     * - ONLY_NON_SYNTHESIZED: 只渲染非合成的参数名称
     * - NONE: 不渲染参数名称
     *
     * @param synthesizedParameterNames 是否为合成的参数名称
     * @return 是否应该渲染参数名称
     */
    private fun shouldRenderParameterNames(synthesizedParameterNames: Boolean): Boolean =
        when (parameterNameRenderingPolicy) {
            ParameterNameRenderingPolicy.ALL -> true
            ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED -> !synthesizedParameterNames
            ParameterNameRenderingPolicy.NONE -> false
        }

    /**
     * 追加单个值参数到字符串构建器
     *
     * 格式: `[修饰符] 名称: 类型`
     *
     * 如果是主构造器参数且配置为渲染为属性，则会添加 var/val/let 前缀
     *
     * @param valueParameter 值参数描述符
     * @param includeName 是否包含参数名称
     * @param topLevel 是否为顶层参数（添加 value-parameter 关键字）
     */
    private fun StringBuilder.appendValueParameter(
        valueParameter: ValueParameterDescriptor,
        includeName: Boolean,
        topLevel: Boolean
    ) {
        if (topLevel) {
            append(renderKeyword("value-parameter"))
            append(" ")
        }

        if (verbose) {
            appendHighlighted("/*${valueParameter.index}*/ ") { asInfo }
        }


        val isPrimaryConstructor = renderPrimaryConstructorParametersAsProperties &&
                (valueParameter.containingDeclaration as? ClassConstructorDescriptor)?.isPrimary == true
        if (isPrimaryConstructor) {
            appendModifier(actualPropertiesInPrimaryConstructor, "actual")
        }

        appendVariable(valueParameter, includeName, topLevel, isPrimaryConstructor)

        val withDefaultValue =
            defaultParameterValueRenderer != null &&
                    (if (debugMode) valueParameter.declaresDefaultValue else valueParameter.declaresOrInheritsDefaultValue())
        if (withDefaultValue) {
            appendHighlighted(" = ") { asOperationSign }
            append(highlightByLexer(defaultParameterValueRenderer!!(valueParameter)))
        }
    }

    private fun StringBuilder.appendMutPropPrefix(
        variable: VariableDescriptor,
        isInPrimaryConstructor: Boolean = false
    ) {
        if (isInPrimaryConstructor || variable !is ValueParameterDescriptor) {
            if (variable.isVar) {
                appendHighlighted("mut ") { asMut }
                appendHighlighted("prop") { asProp }

            } else {
                appendHighlighted("prop") { asProp }
            }
            append(" ")
        }
    }

    /**
     * 追加 let/var 前缀到字符串构建器
     *
     * 根据变量是否可变选择关键字:
     * - 可变变量 → `var`
     * - 不可变变量 → `let`
     *
     * @param variable 变量描述符
     * @param isInPrimaryConstructor 是否在主构造器中
     */
    private fun StringBuilder.appendLetVarPrefix(
        variable: VariableDescriptor,
        isInPrimaryConstructor: Boolean = false
    ) {
        if (isInPrimaryConstructor || variable !is ValueParameterDescriptor) {
            if (variable.isVar) {
                appendHighlighted("var") { asVar }
            } else {
                appendHighlighted("let") { asLet }
            }
            append(" ")
        }
    }


    protected fun StringBuilder.appendName(descriptor: DeclarationDescriptor, rootRenderedElement: Boolean) {
        append(renderName(descriptor.name, rootRenderedElement))
    }

    /**
     * 追加描述符名称到字符串构建器（带高亮）
     *
     * 使用提供的高亮属性构建器来设置名称的高亮样式
     *
     * @param descriptor 描述符
     * @param rootRenderedElement 是否为根渲染元素
     * @param attributesBuilder 高亮属性构建器
     */
    private fun StringBuilder.appendName(
        descriptor: DeclarationDescriptor,
        rootRenderedElement: Boolean,
        attributesBuilder: CangJieIdeDescriptorRendererHighlightingManager<CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes>.() -> CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes
    ) {

        return with(options.highlightingManager) {
            this@appendName.appendHighlighted(renderName(descriptor.name, rootRenderedElement), attributesBuilder())
        }
    }


    /**
     * 追加变量初始化器到字符串构建器
     *
     * 如果配置包含属性常量且变量有编译时常量值，则渲染初始化器
     *
     * @param variable 变量描述符
     */
    private fun StringBuilder.appendInitializer(variable: VariableDescriptor) {
        if (includePropertyConstant) {
            variable.getCompileTimeInitializer()?.let { constant ->
                appendHighlighted(" = ") { asOperationSign }
                append(escape(renderConstant(constant)))
            }
        }
    }

    /**
     * 使用词法分析器对代码片段进行高亮
     *
     * @param value 要高亮的代码片段
     * @return 高亮后的字符串
     */
    private fun highlightByLexer(value: String): String {
        return with(overriddenHighlightingManager!!) { buildString { appendCodeSnippetHighlightedByLexer(value) } }
    }

    /**
     * 渲染常量值
     *
     * 支持的常量类型:
     * - ArrayValue: 渲染为 `{元素1, 元素2, ...}`
     * - 其他: 使用词法分析器高亮
     *
     * @param value 常量值
     * @return 渲染后的常量字符串
     */
    private fun renderConstant(value: ConstantValue<*>): String {
        return when (value) {
            is ArrayValue -> {
                buildString {
                    appendHighlighted("{") { asBraces }
                    value.value.joinTo(this, highlight(", ") { asComma }) { renderConstant(it) }
                    appendHighlighted("}") { asBraces }
                }
            }

            else -> highlightByLexer(value.toString())
        }
    }

    /**
     * 追加值参数列表到字符串构建器
     *
     * 格式: `(参数1, 参数2, ...)`
     *
     * @param parameters 值参数描述符集合
     * @param synthesizedParameterNames 是否为合成的参数名称
     */
    private fun StringBuilder.appendValueParameters(
        parameters: Collection<ValueParameterDescriptor>,
        synthesizedParameterNames: Boolean
    ) {
        val includeNames = shouldRenderParameterNames(synthesizedParameterNames)
        val parameterCount = parameters.size
        valueParametersHandler.appendBeforeValueParameters(parameterCount, this)
        for ((index, parameter) in parameters.withIndex()) {
            valueParametersHandler.appendBeforeValueParameter(parameter, index, parameterCount, this)
            appendValueParameter(parameter, includeNames, false)
            valueParametersHandler.appendAfterValueParameter(parameter, index, parameterCount, this)
        }
        valueParametersHandler.appendAfterValueParameters(parameterCount, this)
    }

    override fun renderName(name: Name, rootRenderedElement: Boolean): String {
        val escaped = escape(name.render())
        return if (options.bold && boldOnlyForNamesInHtml && textFormat == RenderingFormat.HTML && rootRenderedElement) {
            "<b>$escaped</b>"
        } else
            escaped
    }


}
