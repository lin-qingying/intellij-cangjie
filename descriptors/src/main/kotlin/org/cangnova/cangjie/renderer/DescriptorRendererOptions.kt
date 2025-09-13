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

package org.cangnova.cangjie.renderer

import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.resolve.constants.ConstantValue
import org.cangnova.cangjie.types.CangJieType

/**
 * 描述符渲染器选项接口
 * 定义了控制描述符渲染行为的各种配置选项
 */
interface DescriptorRendererOptions {
    /** 分类器名称策略 */
    var classifierNamePolicy: ClassifierNamePolicy
    /** 是否包含定义位置信息 */
    var withDefinedIn: Boolean
    /** 顶级声明是否包含源文件信息 */
    var withSourceFileForTopLevel: Boolean
    /** 要渲染的修饰符集合 */
    var modifiers: Set<DescriptorRendererModifier>
    /** 是否从名称开始渲染 */
    var startFromName: Boolean
    /** 是否从声明关键字开始渲染 */
    var startFromDeclarationKeyword: Boolean
    /** 是否开启调试模式 */
    var debugMode: Boolean
    /** 类是否包含主构造函数 */
    var classWithPrimaryConstructor: Boolean
    /** 是否开启详细模式 */
    var verbose: Boolean
    /** 是否渲染Unit返回类型 */
    var unitReturnType: Boolean
    /** 是否使用增强类型 */
    var enhancedTypes: Boolean
    /** 是否省略返回类型 */
    var withoutReturnType: Boolean
    /** 是否规范化可见性 */
    var normalizedVisibilities: Boolean
    /** 是否渲染默认可见性 */
    var renderDefaultVisibility: Boolean
    /** 是否渲染默认模态性 */
    var renderDefaultModality: Boolean
    /** 是否渲染构造函数委托 */
    var renderConstructorDelegation: Boolean
    /** 是否将主构造函数参数渲染为属性 */
    var renderPrimaryConstructorParametersAsProperties: Boolean
    /** 主构造函数中的实际属性 */
    var actualPropertiesInPrimaryConstructor: Boolean
    /** 是否将未推断的类型参数作为名称 */
    var uninferredTypeParameterAsName: Boolean

    /** 重写渲染策略 */
    var overrideRenderingPolicy: OverrideRenderingPolicy
    /** 值参数处理器 */
    var valueParametersHandler: DescriptorRenderer.ValueParametersHandler
    /** 文本格式 */
    var textFormat: RenderingFormat
    /** 排除的注解类集合 */
    var excludedAnnotationClasses: Set<FqName>

    /** 排除的类型注解类集合 */
    var excludedTypeAnnotationClasses: Set<FqName>
    /** 注解过滤器 */
    var annotationFilter: ((AnnotationDescriptor) -> Boolean)?
    /** 每个注解是否单独一行 */
    var eachAnnotationOnNewLine: Boolean

    /** 注解参数渲染策略 */
    var annotationArgumentsRenderingPolicy: AnnotationArgumentsRenderingPolicy
    /** 是否包含注解参数 */
    val includeAnnotationArguments: Boolean get() = annotationArgumentsRenderingPolicy.includeAnnotationArguments
    /** 是否包含空注解参数 */
    val includeEmptyAnnotationArguments: Boolean get() = annotationArgumentsRenderingPolicy.includeEmptyAnnotationArguments

    /** HTML中是否仅对名称加粗 */
    var boldOnlyForNamesInHtml: Boolean

    /** 是否包含属性常量 */
    var includePropertyConstant: Boolean
    /** 属性常量渲染器 */
    var propertyConstantRenderer: ((ConstantValue<*>) -> String?)?
    /** 参数名称渲染策略 */
    var parameterNameRenderingPolicy: ParameterNameRenderingPolicy
    /** 是否省略类型参数 */
    var withoutTypeParameters: Boolean
    /** 接收者是否在名称后面 */
    var receiverAfterName: Boolean
    /** 是否渲染伴生对象名称 */
    var renderCompanionObjectName: Boolean
    /** 是否省略父类型 */
    var withoutSuperTypes: Boolean
    /** 类型规范化器 */
    var typeNormalizer: (CangJieType) -> CangJieType
    /** 默认参数值渲染器 */
    var defaultParameterValueRenderer: ((ValueParameterDescriptor) -> String)?
    /** 是否将次构造函数作为主构造函数 */
    var secondaryConstructorsAsPrimary: Boolean
    /** 属性访问器渲染策略 */
    var propertyAccessorRenderingPolicy: PropertyAccessorRenderingPolicy
    /** 是否渲染默认注解参数 */
    var renderDefaultAnnotationArguments: Boolean
    /** 是否总是渲染修饰符 */
    var alwaysRenderModifiers: Boolean
    /** 是否渲染构造函数关键字 */
    var renderConstructorKeyword: Boolean
    /** 是否渲染未缩写类型 */
    var renderUnabbreviatedType: Boolean
    /** 是否渲染类型展开 */
    var renderTypeExpansions: Boolean

    /**
     * 如果 [renderTypeExpansions] 为 `true`，则额外将缩写类型作为注释渲染在展开类型后面
     */
    var renderAbbreviatedTypeComments: Boolean

    /** 是否包含额外修饰符 */
    var includeAdditionalModifiers: Boolean
    /** 函数类型中是否包含参数名称 */
    var parameterNamesInFunctionalTypes: Boolean
    /** 是否渲染函数契约 */
    var renderFunctionContracts: Boolean
    /** 是否展示未解析类型 */
    var presentableUnresolvedTypes: Boolean
    /** 是否使用信息性错误类型 */
    var informativeErrorType: Boolean
}

