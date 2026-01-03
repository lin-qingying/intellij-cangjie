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

package org.cangnova.cangjie.resolve

import com.google.common.collect.Lists
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.CangJieBuiltIns.Companion.isNothing
import org.cangnova.cangjie.builtins.CangJieBuiltIns.Companion.isUnit
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.AnnotationSplitter
import org.cangnova.cangjie.descriptors.annotations.AnnotationUseSiteTarget
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.annotations.CompositeAnnotations
import org.cangnova.cangjie.descriptors.impl.*
import org.cangnova.cangjie.descriptors.impl.TypeParameterDescriptorImpl.Companion.createForFurtherModification
import org.cangnova.cangjie.descriptors.impl.ValueParameterDescriptorImpl.Companion.createWithDestructuringDeclarations
import org.cangnova.cangjie.descriptors.impl.VariableDescriptorImpl.Companion.create
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.ClassId.Companion.fromString
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames.anonymousParameterName
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.elements.getAllBindings
import org.cangnova.cangjie.resolve.DescriptorUtils.getDispatchReceiverParameterIfNeeded
import org.cangnova.cangjie.resolve.DescriptorUtils.getParentOfType
import org.cangnova.cangjie.resolve.DescriptorUtils.isAnonymousObject
import org.cangnova.cangjie.resolve.DescriptorUtils.isLocal
import org.cangnova.cangjie.resolve.DescriptorUtils.isSubclass
import org.cangnova.cangjie.resolve.ModifiersChecker.Companion.resolveMemberModalityFromModifiers
import org.cangnova.cangjie.resolve.ModifiersChecker.Companion.resolveVisibilityFromModifiers
import org.cangnova.cangjie.resolve.VariableAsPropertyInfo.Companion.createFromProperty
import org.cangnova.cangjie.resolve.VariableTypeAndInitializerResolver.Companion.getTypeForVariableWithoutReturnType
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfoFactory
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.calls.util.isSingleUnderscore
import org.cangnova.cangjie.resolve.lazy.ForceResolveUtil.forceResolveAllContents
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyTypeAliasDescriptor.Companion.create
import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.resolve.scopes.ScopeUtils.makeScopeForPropertyInitializer
import org.cangnova.cangjie.resolve.scopes.ScopeUtils.makeScopeForVariableInitializer
import org.cangnova.cangjie.resolve.source.toSourceElement
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.ErrorUtils.createErrorType
import org.cangnova.cangjie.types.ErrorUtils.invalidType
import org.cangnova.cangjie.types.TypeUtils.equalTypes
import org.cangnova.cangjie.types.TypeUtils.getClassDescriptor
import org.cangnova.cangjie.types.checker.TypeIntersector
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.error.MultipleSupertypeTypeInferenceFailure
import org.cangnova.cangjie.types.expressions.ExpressionTypingServices
import org.cangnova.cangjie.types.expressions.FunctionsTypingVisitor
import org.cangnova.cangjie.types.expressions.PreliminaryDeclarationVisitor
import java.util.*

/**
 * 描述符解析器
 *
 * 负责将 PSI 元素转换为描述符（Descriptor），是仓颉语言语义分析的核心组件。
 *
 * 主要职责：
 * - 解析变量和属性声明为对应的描述符
 * - 解析函数参数和构造函数参数
 * - 解析类型参数和泛型约束
 * - 解析类型别名
 * - 解析超类型关系
 * - 推断表达式体的返回类型
 * - 检查类型上下界约束
 * - 处理匿名类型转换
 *
 * @param annotationResolver 注解解析器
 * @param builtIns 内置类型提供者
 * @param storageManager 存储管理器，用于创建惰性计算值
 * @param typeResolver 类型解析器
 * @param supertypeLoopsResolver 超类型循环检查器
 * @param variableTypeAndInitializerResolver 变量类型和初始化器解析器
 * @param expressionTypingServices 表达式类型推导服务
 * @param overloadChecker 重载检查器
 * @param languageVersionSettings 语言版本设置
 * @param functionsTypingVisitor 函数类型检查访问器
 * @param modifiersChecker 修饰符检查器
 * @param wrappedTypeFactory 包装类型工厂
 * @param project IntelliJ 项目
 * @param typeApproximator 类型近似器
 * @param declarationReturnTypeSanitizer 声明返回类型清理器
 * @param dataFlowValueFactory 数据流值工厂
 * @param anonymousTypeTransformers 匿名类型转换器集合
 */
class DescriptorResolver(
    private val annotationResolver: AnnotationResolver,
    private val builtIns: CangJieBuiltIns,
    private val storageManager: StorageManager,
    private val typeResolver: TypeResolver,
    private val supertypeLoopsResolver: SupertypeLoopChecker,
    private val variableTypeAndInitializerResolver: VariableTypeAndInitializerResolver,
    private val expressionTypingServices: ExpressionTypingServices,
    private val overloadChecker: OverloadChecker,
    private val languageVersionSettings: LanguageVersionSettings,

    private val functionsTypingVisitor: FunctionsTypingVisitor,

    private val modifiersChecker: ModifiersChecker,
    private val wrappedTypeFactory: WrappedTypeFactory,
    project: Project,
    private val typeApproximator: TypeApproximator,
    private val declarationReturnTypeSanitizer: DeclarationReturnTypeSanitizer,
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val anonymousTypeTransformers: Iterable<DeclarationSignatureAnonymousTypeTransformer>,

    ) {




    /**
     * 解析主构造函数参数为变量
     *
     * 将主构造函数的参数转换为类的成员变量描述符。
     * 这用于处理主构造函数参数声明为类属性的情况（使用 let/var 关键字）。
     *
     * @param classDescriptor 包含该参数的类描述符
     * @param valueParameter 值参数描述符
     * @param scope 解析作用域
     * @param parameter PSI 参数元素
     * @param trace 绑定追踪器
     * @return 解析得到的变量描述符
     */
    fun resolvePrimaryConstructorParameterToAVariable(
        classDescriptor: ClassAndEnumDescriptor,
        valueParameter: ValueParameterDescriptor,
        scope: LexicalScope,
        parameter: CjParameter,
        trace: BindingTrace
    ): VariableDescriptor {
        val type = resolveParameterType(scope, parameter, trace)
        val name = parameter.nameAsSafeName
        val isMutable = parameter.isMutable
        val modifierList = parameter.modifierList

        if (modifierList != null) {
            if (modifierList.hasModifier(CjTokens.ABSTRACT_KEYWORD)) {
                trace.report(ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS.on(parameter))
            }
        }

        val allAnnotations = annotationResolver.resolveAnnotationsWithoutArguments(scope, parameter.annotations, trace)
        val targetSet: MutableSet<AnnotationUseSiteTarget> = EnumSet.of(
            AnnotationUseSiteTarget.PROPERTY,
            AnnotationUseSiteTarget.PROPERTY_GETTER,
            AnnotationUseSiteTarget.FIELD,
            AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER,
            AnnotationUseSiteTarget.PROPERTY_SETTER
        )
        if (isMutable) {
            targetSet.add(AnnotationUseSiteTarget.PROPERTY_SETTER)
            targetSet.add(AnnotationUseSiteTarget.SETTER_PARAMETER)
        }
        AnnotationSplitter(
            storageManager, allAnnotations, targetSet
        )

        //        Annotations propertyAnnotations = new CompositeAnnotations(
//                annotationSplitter.getAnnotationsForTarget(PROPERTY),
//                annotationSplitter.getOtherAnnotations()
//        );
        val variableDescriptor = create(
            classDescriptor,
            name,
            resolveVisibilityFromModifiers(parameter, getDefaultVisibility(parameter, classDescriptor)),
            isMutable,


            parameter.toSourceElement()


        )
        variableDescriptor.setType(
            type, emptyList(), getDispatchReceiverParameterIfNeeded(classDescriptor)
        )


        //
//        Annotations setterAnnotations = annotationSplitter.getAnnotationsForTarget(PROPERTY_SETTER);
//        Annotations getterAnnotations = new CompositeAnnotations(CollectionsKt.listOf(
//                annotationSplitter.getAnnotationsForTarget(PROPERTY_GETTER)));
//
        trace.record(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter, variableDescriptor)
        trace.record(BindingContext.VALUE_PARAMETER_AS_VARIABLE, valueParameter, variableDescriptor)
        return variableDescriptor
    }

    /**
     * 解析局部变量描述符（指定类型）
     *
     * 根据给定的类型创建局部变量描述符。
     * 类型会被近似为声明类型，以确保类型安全。
     *
     * @param parameter 参数 PSI 元素
     * @param type 变量类型
     * @param trace 绑定追踪器
     * @param scope 变量所在的词法作用域
     * @return 解析得到的局部变量描述符
     */
    fun resolveLocalVariableDescriptor(
        parameter: CjParameterBase,
        type: CangJieType,
        trace: BindingTrace,
        scope: LexicalScope
    ): VariableDescriptor {
        val approximatedType = typeApproximator.approximateDeclarationType(type, true)
        val variableDescriptor: VariableDescriptor = LocalVariableDescriptor(
            scope.ownerDescriptor,
            annotationResolver.resolveAnnotationsWithArguments(scope, parameter.annotations, trace),
            CjPsiUtil.safeName(parameter.name),
            approximatedType,
            false,
            parameter.toSourceElement()
        )
        trace.record(BindingContext.VALUE_PARAMETER, parameter, variableDescriptor)
        // Type annotations also should be resolved
        forceResolveAllContents(type.annotations)
        return variableDescriptor
    }

    /**
     * 解析局部变量描述符（从表达式推断类型）
     *
     * 通过表达式推断局部变量类型，如果表达式不存在或推断失败则使用错误类型。
     *
     * @param scope 变量所在的词法作用域
     * @param parameter 参数 PSI 元素
     * @param expression 初始化表达式（可为 null）
     * @param trace 绑定追踪器
     * @return 解析得到的局部变量描述符
     */
    fun resolveLocalVariableDescriptor(
        scope: LexicalScope,
        parameter: CjParameterBase,
        expression: CjExpression?,
        trace: BindingTrace
    ): VariableDescriptor {
        var type: CangJieType? = null
        if (expression != null) {
            type = expressionTypingServices.getTypeInfo(
                scope, expression, trace
            ).type
        }
        if (type == null) {
            type = invalidType
        }
        return resolveLocalVariableDescriptor(parameter, type, trace, scope)
    }

    /**
     * 解析局部变量描述符（从类型引用）
     *
     * 从参数的类型引用解析类型，创建局部变量描述符。
     *
     * @param scope 变量所在的词法作用域
     * @param parameter 参数 PSI 元素
     * @param trace 绑定追踪器
     * @return 解析得到的局部变量描述符
     */
    fun resolveLocalVariableDescriptor(
        scope: LexicalScope,
        parameter: CjParameterBase,
        trace: BindingTrace
    ): VariableDescriptor {
        val type = resolveParameterType(scope, parameter, trace)
        return resolveLocalVariableDescriptor(parameter, type, trace, scope)
    }

    /**
     * 解析参数类型
     *
     * 从参数的类型引用或 catch 参数类型列表解析参数类型。
     * 如果没有类型引用则创建错误类型。
     * 对于多重超类型推断失败的情况会报告相应错误。
     *
     * @param scope 解析作用域
     * @param parameter 参数 PSI 元素
     * @param trace 绑定追踪器
     * @return 解析得到的类型
     */
    private fun resolveParameterType(
        scope: LexicalScope,
        parameter: CjParameterBase,
        trace: BindingTrace
    ): CangJieType {
        val typeReference = parameter.typeReference
        val type = if (typeReference != null) {
            typeResolver.resolveType(scope, typeReference, trace, true)
        } else if (parameter is CjCatchParameter && !parameter.typeReferences.isEmpty()) {
            typeResolver.resolveType(scope, parameter.typeReferences, trace, true)
        } else {
            // Error is reported by the parser
            createErrorType(ErrorTypeKind.NO_TYPE_SPECIFIED, parameter.text)
        }
        if (type is MultipleSupertypeTypeInferenceFailure) {
            trace.report(
                TYPE_MISMATCH_MULTIPLE_SUPERTYPES.on(
                    parameter,
                    type.intersectedTypes
                )
            )
        }
        return type
    }

    /**
     * 获取默认超类型
     *
     * 根据仓颉继承规则为类描述符提供默认超类型：
     * 1. 如果类型为 Class 且没有类型为 class 的父类，则默认继承 Object
     * 2. 如果类型为 Interface/Struct/Enum 且没有类型为 interface 的父类，则默认继承 Any
     * 3. Object 类本身继承 Any
     * 4. Any 类没有默认超类型
     *
     * @param classDescriptor 类描述符
     * @param supertypes 已声明的超类型列表
     * @param classId 类 ID（用于判断是否为 Object 类）
     * @return 默认超类型，如果不需要默认超类型则返回 null
     */
    private fun getDefaultSupertype(
        classDescriptor: ClassAndEnumDescriptor,
        supertypes: List<CangJieType>,
        classId: ClassId?
    ): CangJieType? {
        //        根据仓颉继承规则，做如下配置
//        1. 如果类型为Class且没有类型为class的父类，则默认继承为Object
//        2. 如果类型为Interface,Struct,enum且没有类型为interface的父类，则默认继承为Any

        if (classDescriptor.kind == ClassKind.CLASS) {
            if (fromString("std/core/Object").equals(classId)) {
                return builtIns.stdlibTypes.anyType
            }
            if (supertypes.isEmpty()) {
                return builtIns.stdlibTypes.objectType
            }

            for (supertype in supertypes) {
                if (supertype.classKind == ClassKind.CLASS) {
                    return null
                }
            }
            return builtIns.stdlibTypes.objectType
        }

        //        if (classDescriptor.getKind() == ClassKind.ENUM_CONSTRUCTOR) {
//            return ((ClassDescriptor) classDescriptor.getContainingDeclaration()).getDefaultType();
//        } else if (classDescriptor.getKind() == ClassKind.CLASS) {
//            return builtIns.getObjectType();
//        }

//当没有父类时，返回any接口
// TODO 注：当获取超类型时，并且为可扩展的类型，考虑到扩展接口，需按情况去除any接口，以保证类型推导正常
        if (supertypes.isEmpty()) {
            return builtIns.stdlibTypes.anyType
        }
        return null
    }

    /**
     * 解析超类型
     *
     * 解析类声明的超类型列表，包括：
     * 1. 解析 PSI 中显式声明的超类型
     * 2. 为没有显式超类型的类添加默认超类型（Object 或 Any）
     * 3. Any 类不添加默认超类型
     *
     * @param scope 解析作用域
     * @param classDescriptor 类描述符
     * @param typeStatement 类型声明 PSI 元素
     * @param trace 绑定追踪器
     * @return 解析得到的超类型列表
     */
    fun resolveSupertypes(
        scope: LexicalScope,
        classDescriptor: ClassAndEnumDescriptor,
        typeStatement: CjPureTypeStatement?,
        trace: BindingTrace
    ): List<CangJieType> {
//        builtIns.setSourcesModuleDescriptor(DescriptorUtilsKt.getModule(scope.getOwnerDescriptor()));

        val supertypes: MutableList<CangJieType> = Lists.newArrayList()
        val delegationSpecifiers =
            typeStatement?.superTypeListEntries ?: emptyList()
        val declaredSupertypes = resolveSuperTypeListEntries(
            scope,
            delegationSpecifiers,
            typeResolver, trace, false
        )

        for (declaredSupertype in declaredSupertypes) {
            addValidSupertype(supertypes, declaredSupertype)
        }


        val classId = (typeStatement as CjClassLikeDeclaration).getClassId()


        //不为Any类型默认继承
        if (!fromString("std/core/Any").equals(classId)) {
            val defualtType = getDefaultSupertype(classDescriptor, supertypes, classId)
            if (defualtType != null) {
                addValidSupertype(supertypes, defualtType)
            }
        }


        return supertypes
    }

    /**
     * 解析为属性描述符（内部实现）
     *
     * 将变量声明解析为完整的属性描述符，包括：
     * - 解析可见性、修饰性和注解
     * - 处理泛型类型参数
     * - 创建 getter 和 setter 访问器
     * - 解析属性类型和初始化器
     * - 设置常量值（如果适用）
     *
     * @param container 包含该属性的描述符（类或包）
     * @param scopeForDeclarationResolution 用于声明解析的作用域
     * @param scopeForInitializerResolution 用于初始化器解析的作用域
     * @param variableDeclaration 变量声明 PSI 元素
     * @param trace 绑定追踪器
     * @param dataFlowInfo 数据流信息
     * @param inferenceSession 类型推断会话
     * @param propertyInfo 属性信息（包含 getter/setter 和类型）
     * @return 解析得到的属性描述符
     */
    private fun resolveAsPropertyDescriptor(
        container: DeclarationDescriptor,
        scopeForDeclarationResolution: LexicalScope,
        scopeForInitializerResolution: LexicalScope,
        variableDeclaration: CjVariableDeclaration,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession?,
        propertyInfo: VariableAsPropertyInfo
    ): PropertyDescriptor {
        val annotations =
            variableDeclaration.annotations
        variableDeclaration.modifierList
        val isVar = variableDeclaration.isVar

        val visibility =
            resolveVisibilityFromModifiers(variableDeclaration, getDefaultVisibility(variableDeclaration, container))
        val modality = if (container is ClassDescriptor)
            resolveMemberModalityFromModifiers(
                variableDeclaration,
                getDefaultModality(container, visibility, propertyInfo.hasBody),
                trace.bindingContext, container
            )
        else
            Modality.FINAL

        val allAnnotations =
            annotationResolver.resolveAnnotationsWithoutArguments(scopeForDeclarationResolution, annotations, trace)
        val targetSet: MutableSet<AnnotationUseSiteTarget> = EnumSet.of(
            AnnotationUseSiteTarget.PROPERTY,
            AnnotationUseSiteTarget.PROPERTY_GETTER,
            AnnotationUseSiteTarget.FIELD
        )
        if (isVar) {
            targetSet.add(AnnotationUseSiteTarget.PROPERTY_SETTER)
            targetSet.add(AnnotationUseSiteTarget.SETTER_PARAMETER)
        }

        val annotationSplitter = AnnotationSplitter(
            storageManager, allAnnotations, targetSet
        )

        CompositeAnnotations(
            listOf(
                annotationSplitter.getAnnotationsForTarget(AnnotationUseSiteTarget.PROPERTY),
                annotationSplitter.getOtherAnnotations()
            )
        )

        val propertyDescriptor = PropertyDescriptorImpl.create(
            container,  /*      propertyAnnotations,*/
            Annotations.EMPTY,
            modality,
            visibility,
            isVar,
            CjPsiUtil.safeName(variableDeclaration.name),
            CallableMemberDescriptor.Kind.DECLARATION,
            variableDeclaration.toSourceElement() //                modifierList != null && modifierList.hasModifier(CjTokens.LATEINIT_KEYWORD),
            //                modifierList != null && modifierList.hasModifier(CjTokens.CONST_KEYWORD),
            //                modifierList != null && PsiUtilsKt.hasExpectModifier(modifierList) && container instanceof PackageFragmentDescriptor ||
            //                        container instanceof ClassDescriptor && ((ClassDescriptor) container).isExpect(),
            //                modifierList != null && PsiUtilsKt.hasActualModifier(modifierList),
            //                modifierList != null && modifierList.hasModifier(CjTokens.EXTERNAL_KEYWORD),
            //                propertyInfo.getHasDelegate()
        )

        var typeParameterDescriptors: List<TypeParameterDescriptorImpl>
        var scopeForDeclarationResolutionWithTypeParameters: LexicalScope
        var scopeForInitializerResolutionWithTypeParameters: LexicalScope?
        var receiverType: CangJieType? = null

        run {
            val typeParameters = variableDeclaration.typeParameters
            if (typeParameters.isEmpty()) {
                scopeForDeclarationResolutionWithTypeParameters = scopeForDeclarationResolution
                scopeForInitializerResolutionWithTypeParameters = scopeForInitializerResolution
                typeParameterDescriptors = emptyList()
            } else {
                val writableScopeForDeclarationResolution = LexicalWritableScope(
                    scopeForDeclarationResolution, container, false, TraceBasedLocalRedeclarationChecker(
                        trace,
                        overloadChecker
                    ),
                    LexicalScopeKind.PROPERTY_HEADER
                )
                val writableScopeForInitializerResolution = LexicalWritableScope(
                    scopeForInitializerResolution, container, false, LocalRedeclarationChecker.DO_NOTHING,
                    LexicalScopeKind.PROPERTY_HEADER
                )
                typeParameterDescriptors = resolveTypeParametersForDescriptor(
                    propertyDescriptor,
                    scopeForDeclarationResolution, typeParameters, trace
                )
                for (descriptor in typeParameterDescriptors) {
                    writableScopeForDeclarationResolution.addClassifierDescriptor(descriptor)
                    writableScopeForInitializerResolution.addClassifierDescriptor(descriptor)
                }
                writableScopeForDeclarationResolution.freeze()
                writableScopeForInitializerResolution.freeze()
                resolveGenericBounds(
                    variableDeclaration,
                    propertyDescriptor,
                    writableScopeForDeclarationResolution,
                    typeParameterDescriptors,
                    trace
                )
                scopeForDeclarationResolutionWithTypeParameters = writableScopeForDeclarationResolution
                scopeForInitializerResolutionWithTypeParameters = writableScopeForInitializerResolution
            }
        }





        val scopeForInitializer = makeScopeForPropertyInitializer(
            scopeForInitializerResolutionWithTypeParameters!!, propertyDescriptor
        )
        val propertyType = propertyInfo.variableType
        val typeIfKnown = propertyType
            ?: variableTypeAndInitializerResolver.resolveTypeOptional(
                propertyDescriptor, scopeForInitializer,
                variableDeclaration, dataFlowInfo, inferenceSession!!,
                trace,  /* local = */false
            )

        val getter = resolvePropertyGetterDescriptor(
            scopeForDeclarationResolutionWithTypeParameters,
            variableDeclaration,
            propertyDescriptor,
            annotationSplitter,
            trace,
            typeIfKnown,
            propertyInfo.propertyGetter,

            inferenceSession
        )
        //
        val type = checkNotNull(
            typeIfKnown ?: getter.returnType
        ) { "At least getter type must be initialized via resolvePropertyGetterDescriptor" }

        variableTypeAndInitializerResolver.setConstantForVariableIfNeeded(
            propertyDescriptor, scopeForInitializer, variableDeclaration, dataFlowInfo, type, inferenceSession!!, trace
        )

        propertyDescriptor.setType(
            type, typeParameterDescriptors, getDispatchReceiverParameterIfNeeded(container)
        )

        val setter = resolvePropertySetterDescriptor(
            scopeForDeclarationResolutionWithTypeParameters,
            variableDeclaration,
            propertyDescriptor,
            annotationSplitter,
            trace,
            propertyInfo.propertySetter,

            inferenceSession
        )

        propertyDescriptor.initialize(
            getter, setter

        )
        trace.record(BindingContext.VARIABLE, variableDeclaration, propertyDescriptor)
        return propertyDescriptor
    }

    /**
     * 解析属性 setter 描述符
     *
     * 为属性创建 setter 访问器描述符。
     * 如果 PSI 中提供了显式 setter，解析其参数和返回类型。
     * 否则为 var 属性创建默认 setter。
     * 对于 let 属性，如果提供了 setter 会报告错误。
     *
     * @param scopeWithTypeParameters 包含类型参数的作用域
     * @param property 属性 PSI 元素
     * @param propertyDescriptor 属性描述符
     * @param annotationSplitter 注解分割器
     * @param trace 绑定追踪器
     * @param setter setter PSI 元素（可为 null）
     * @param inferenceSession 类型推断会话
     * @return setter 描述符，let 属性返回 null
     */
    private fun resolvePropertySetterDescriptor(
        scopeWithTypeParameters: LexicalScope,
        property: CjVariableDeclaration,
        propertyDescriptor: PropertyDescriptor,
        annotationSplitter: AnnotationSplitter,
        trace: BindingTrace,
        setter: CjPropertyAccessor?,

        inferenceSession: InferenceSession?
    ): PropertySetterDescriptor? {
        var setterDescriptor: PropertySetterDescriptorImpl? = null
        val setterTargetedAnnotations =
            annotationSplitter.getAnnotationsForTarget(AnnotationUseSiteTarget.PROPERTY_SETTER)
        val parameterTargetedAnnotations =
            annotationSplitter.getAnnotationsForTarget(AnnotationUseSiteTarget.SETTER_PARAMETER)
        if (setter != null) {
            val annotations: Annotations = CompositeAnnotations(
                listOf(
                    setterTargetedAnnotations,
                    annotationResolver.resolveAnnotationsWithoutArguments(
                        scopeWithTypeParameters,
                        setter.annotations,
                        trace
                    )
                )
            )
            val parameter = setter.parameter

            setterDescriptor = PropertySetterDescriptorImpl(
                propertyDescriptor, annotations,
                resolveMemberModalityFromModifiers(
                    setter, propertyDescriptor.modality,
                    trace.bindingContext, propertyDescriptor.containingDeclaration
                ),
                resolveVisibilityFromModifiers(setter, propertyDescriptor.visibility),  /* isDefault = */

                CallableMemberDescriptor.Kind.DECLARATION, null, setter.toSourceElement()
            )
            val returnTypeReference = setter.returnTypeReference
            if (returnTypeReference != null) {
                val returnType = typeResolver.resolveType(scopeWithTypeParameters, returnTypeReference, trace, true)
                if (!isUnit(returnType)) {
                    trace.report(WRONG_SETTER_RETURN_TYPE.on(returnTypeReference))
                }
            }

            if (parameter != null) {
                // This check is redundant: the parser does not allow a default value, but we'll keep it just in case

                if (parameter.hasDefaultValue()) {
                    parameter.defaultValue?.let { trace.report(SETTER_PARAMETER_WITH_DEFAULT_VALUE.on(it)) }
                }

                val type: CangJieType
                val typeReference = parameter.typeReference
                if (typeReference == null) {
                    type = propertyDescriptor.type // TODO : this maybe unknown at this point
                } else {
                    type = typeResolver.resolveType(scopeWithTypeParameters, typeReference, trace, true)
                    val inType = propertyDescriptor.type
                    if (!equalTypes(type, inType)) {
                        trace.report(WRONG_SETTER_PARAMETER_TYPE.on(typeReference, inType, type))
                    }
                }

                val valueParameterDescriptor = resolveValueParameterDescriptor(
                    scopeWithTypeParameters,
                    setterDescriptor,
                    parameter,
                    0,
                    type,
                    trace,
                    parameterTargetedAnnotations,
                    inferenceSession
                )
                setterDescriptor.initialize(valueParameterDescriptor)
            } else {
                setterDescriptor.initializeDefault()
            }

            trace.record(BindingContext.PROPERTY_ACCESSOR, setter, setterDescriptor)
        } else if (property.isVar) {
            setterDescriptor = DescriptorFactory.createSetter(
                propertyDescriptor, setterTargetedAnnotations, parameterTargetedAnnotations,
                setterTargetedAnnotations.isEmpty() && parameterTargetedAnnotations.isEmpty(),

                propertyDescriptor.source
            )
        }

        if (!property.isVar) {
            if (setter != null) {
                trace.report(LET_WITH_SETTER.on(setter))
            }
        }
        return setterDescriptor
    }

    /**
     * 解析属性 getter 描述符
     *
     * 为属性创建 getter 访问器描述符。
     * 如果 PSI 中提供了显式 getter，解析其返回类型。
     * 否则创建默认 getter。
     * 支持从表达式体推断 getter 返回类型（例如 `let x get() = ...`）。
     *
     * @param scopeForDeclarationResolution 用于声明解析的作用域
     * @param property 属性 PSI 元素
     * @param propertyDescriptor 属性描述符
     * @param annotationSplitter 注解分割器
     * @param trace 绑定追踪器
     * @param propertyTypeIfKnown 已知的属性类型（可为 null）
     * @param getter getter PSI 元素（可为 null）
     * @param inferenceSession 类型推断会话
     * @return getter 描述符
     */
    private fun resolvePropertyGetterDescriptor(
        scopeForDeclarationResolution: LexicalScope,
        property: CjVariableDeclaration,
        propertyDescriptor: PropertyDescriptor,
        annotationSplitter: AnnotationSplitter,
        trace: BindingTrace,
        propertyTypeIfKnown: CangJieType?,
        getter: CjPropertyAccessor?,

        inferenceSession: InferenceSession?
    ): PropertyGetterDescriptorImpl {
        val getterDescriptor: PropertyGetterDescriptorImpl
        val getterType: CangJieType?
        val getterTargetedAnnotations =
            annotationSplitter.getAnnotationsForTarget(AnnotationUseSiteTarget.PROPERTY_GETTER)
        if (getter != null) {
            val getterAnnotations: Annotations = CompositeAnnotations(
                listOf(
                    getterTargetedAnnotations,
                    annotationResolver.resolveAnnotationsWithoutArguments(
                        scopeForDeclarationResolution,
                        getter.annotations,
                        trace
                    )
                )
            )

            getterDescriptor = PropertyGetterDescriptorImpl(
                propertyDescriptor, getterAnnotations,
                resolveMemberModalityFromModifiers(
                    getter, propertyDescriptor.modality,
                    trace.bindingContext, propertyDescriptor.containingDeclaration
                ),
                resolveVisibilityFromModifiers(getter, propertyDescriptor.visibility),  /* isDefault = */


                CallableMemberDescriptor.Kind.DECLARATION, null, getter.toSourceElement()
            )
            getterType = determineGetterReturnType(
                scopeForDeclarationResolution, trace, getterDescriptor, getter, propertyTypeIfKnown, inferenceSession
            )
        } else {
            getterDescriptor = DescriptorFactory.createGetter(
                propertyDescriptor, getterTargetedAnnotations,
                getterTargetedAnnotations.isEmpty()

            )
            getterType = propertyTypeIfKnown
        }

        getterDescriptor.initialize(
            getterType
                ?: getTypeForVariableWithoutReturnType(propertyDescriptor.name.asString())
        )

        if (getter != null) {
            trace.record(BindingContext.PROPERTY_ACCESSOR, getter, getterDescriptor)
        }

        return getterDescriptor
    }

    /**
     * 确定 getter 返回类型
     *
     * 根据以下优先级确定 getter 的返回类型：
     * 1. 显式的返回类型引用
     * 2. 从表达式体推断（如果没有块体且属性没有类型声明）
     * 3. 使用属性的已知类型
     *
     * 如果显式类型与属性类型不匹配会报告错误。
     *
     * @param scope 解析作用域
     * @param trace 绑定追踪器
     * @param getterDescriptor getter 描述符
     * @param getter getter PSI 元素
     * @param propertyTypeIfKnown 已知的属性类型（可为 null）
     * @param inferenceSession 类型推断会话
     * @return 解析得到的返回类型，如果无法确定则返回 null
     */
    private fun determineGetterReturnType(
        scope: LexicalScope,
        trace: BindingTrace,
        getterDescriptor: PropertyGetterDescriptor,
        getter: CjPropertyAccessor,
        propertyTypeIfKnown: CangJieType?,
        inferenceSession: InferenceSession?
    ): CangJieType? {
        val returnTypeReference = getter.returnTypeReference
        if (returnTypeReference != null) {
            val explicitReturnType = typeResolver.resolveType(scope, returnTypeReference, trace, true)
            if (propertyTypeIfKnown != null && !equalTypes(explicitReturnType, propertyTypeIfKnown)) {
                trace.report(
                    WRONG_GETTER_RETURN_TYPE.on(
                        returnTypeReference,
                        propertyTypeIfKnown,
                        explicitReturnType
                    )
                )
            }
            return explicitReturnType
        }

        // If a property has no type specified in the PSI but the getter does (or has an initializer e.g. "val x get() = ..."),
        // infer the correct type for the getter but leave the error type for the property.
        // This is useful for an IDE quick fix which would add the type to the property
        val property = getter.property
        if (property.typeReference == null &&
            getter.hasBody() && !getter.hasBlockBody()
        ) {
            return inferReturnTypeFromExpressionBody(
                trace,
                scope,
                DataFlowInfoFactory.EMPTY,
                getter,
                getterDescriptor,
                inferenceSession
            )
        }

        return propertyTypeIfKnown
    }

    /**
     * 解析属性描述符（公共接口）
     *
     * 解析属性声明为完整的属性描述符。
     * 这是 resolveAsPropertyDescriptor 的公共包装方法。
     *
     * @param containingDeclaration 包含该属性的描述符
     * @param scopeForDeclarationResolution 用于声明解析的作用域
     * @param scopeForInitializerResolution 用于初始化器解析的作用域
     * @param property 属性 PSI 元素
     * @param trace 绑定追踪器
     * @param dataFlowInfo 数据流信息
     * @param inferenceSession 类型推断会话
     * @return 解析得到的属性描述符
     */
    fun resolvePropertyDescriptor(
        containingDeclaration: DeclarationDescriptor,
        scopeForDeclarationResolution: LexicalScope,
        scopeForInitializerResolution: LexicalScope,
        property: CjProperty,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession
    ): PropertyDescriptor {
        return resolveAsPropertyDescriptor(
            containingDeclaration,
            scopeForDeclarationResolution,
            scopeForInitializerResolution,
            property,
            trace,
            dataFlowInfo,
            inferenceSession,
            createFromProperty(property)
        )
    }


    /**
     * 解析泛型约束
     *
     * 解析类型参数的上界约束，包括：
     * - 解析 `extends` 子句中的上界
     * - 解析 `where` 子句中的多个上界约束
     * - 为没有显式上界的类型参数添加默认上界（Any）
     * - 检查冲突的上界
     * - 检查约束中的名称引用是否正确
     *
     * @param declaration 声明 PSI 元素（类、函数或类型别名）
     * @param descriptor 对应的描述符
     * @param scope 解析作用域
     * @param parameters 类型参数描述符列表
     * @param trace 绑定追踪器
     */
    fun resolveGenericBounds(
        declaration: CjTypeParameterListOwner,
        descriptor: DeclarationDescriptor,
        scope: LexicalScope,
        parameters: List<TypeParameterDescriptorImpl>,
        trace: BindingTrace
    ) {
        val upperBoundCheckRequests: MutableList<UpperBoundCheckRequest> = Lists.newArrayList()

        val typeParameters = declaration.typeParameters
        val parameterByName: MutableMap<Name, TypeParameterDescriptorImpl> = HashMap()
        for (i in typeParameters.indices) {
            val cjTypeParameter = typeParameters[i]
            val typeParameterDescriptor = parameters[i]

            parameterByName[typeParameterDescriptor.name] = typeParameterDescriptor

            val extendsBound = cjTypeParameter.extendsBound
            if (extendsBound != null) {
                val type = typeResolver.resolveType(scope, extendsBound, trace, false)
                typeParameterDescriptor.addUpperBound(type)
                upperBoundCheckRequests.add(UpperBoundCheckRequest(cjTypeParameter.nameAsName, extendsBound, type))
            }
        }
        for (constraint in declaration.typeConstraints) {
            val subjectTypeParameterName = constraint.subjectTypeParameterName ?: continue
            val referencedName = subjectTypeParameterName.referencedNameAsName
            val typeParameterDescriptor = parameterByName[referencedName]


            val boundTypeReferences = constraint.boundTypeReferences
            for (boundTypeReference in boundTypeReferences) {
                var bound: CangJieType? = null

                    bound = typeResolver.resolveType(scope, boundTypeReference, trace, false)
                    upperBoundCheckRequests.add(UpperBoundCheckRequest(referencedName, boundTypeReference, bound))


                if (typeParameterDescriptor != null) {
                    trace.record(BindingContext.REFERENCE_TARGET, subjectTypeParameterName, typeParameterDescriptor)

                        typeParameterDescriptor.addUpperBound(bound)

                }
            }
        }

        for (parameter in parameters) {
            parameter.addDefaultUpperBound()
            parameter.setInitialized()
        }

        for (parameter in parameters) {
            checkConflictingUpperBounds(
                trace, parameter,
                typeParameters[parameter.index]
            )
        }

        if (declaration !is CjClass) {
            checkUpperBoundTypes(trace, upperBoundCheckRequests, declaration.hasModifier(CjTokens.OVERRIDE_KEYWORD))
            checkNamesInConstraints(declaration, descriptor, scope, trace)
        }
    }


    /**
     * 检查约束中的名称引用
     *
     * 确保 `where` 子句中引用的类型参数名称是有效的本地类型参数，
     * 不是外部类型或未声明的名称。
     *
     * @param declaration 声明 PSI 元素
     * @param descriptor 对应的描述符
     * @param scope 解析作用域
     * @param trace 绑定追踪器
     */
    fun checkNamesInConstraints(
        declaration: CjTypeParameterListOwner,
        descriptor: DeclarationDescriptor,
        scope: LexicalScope,
        trace: BindingTrace
    ) {
        for (constraint in declaration.typeConstraints) {
            val nameExpression = constraint.subjectTypeParameterName ?: continue

            val name = nameExpression.referencedNameAsName

            val classifier = scope.findClassifier(name, NoLookupLocation.FOR_NON_TRACKED_SCOPE)
            if (classifier is TypeParameterDescriptor && classifier.containingDeclaration === descriptor) continue

            if (classifier != null) {
                // To tell the user that we look only for locally defined type parameters
                trace.report(
                    NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER.on(
                        nameExpression,
                        constraint,
                        declaration
                    )
                )
                trace.record(BindingContext.REFERENCE_TARGET, nameExpression, classifier)
            } else {
                trace.report(UNRESOLVED_REFERENCE.on(nameExpression, nameExpression))
            }

            val boundTypeReference = constraint.boundTypeReference
            if (boundTypeReference != null) {
                typeResolver.resolveType(scope, boundTypeReference, trace, true)
            }
        }
    }

    /**
     * 从表达式体推断返回类型
     *
     * 对于具有表达式体的函数（例如 `func f() = expr`），推断其返回类型。
     * 该过程包括：
     * - 创建递归容错的延迟类型（防止循环依赖）
     * - 预解析函数声明
     * - 推断表达式类型
     * - 转换匿名类型（如果需要）
     * - 近似类型为声明类型
     * - 清理返回类型
     * - 检查返回语句类型一致性
     *
     * @param trace 绑定追踪器
     * @param scope 解析作用域
     * @param dataFlowInfo 数据流信息
     * @param function 带函数体的声明 PSI 元素
     * @param functionDescriptor 函数描述符
     * @param inferenceSession 类型推断会话
     * @return 推断得到的返回类型
     */
    fun inferReturnTypeFromExpressionBody(
        trace: BindingTrace,
        scope: LexicalScope,
        dataFlowInfo: DataFlowInfo,
        function: CjDeclarationWithBody,
        functionDescriptor: FunctionDescriptor,
        inferenceSession: InferenceSession?
    ): CangJieType {


        return wrappedTypeFactory.createRecursionIntolerantDeferredType(trace) {
            PreliminaryDeclarationVisitor.createForDeclaration(function, trace, languageVersionSettings)

            val type = expressionTypingServices.getBodyExpressionType(
                trace, scope, dataFlowInfo, function, functionDescriptor, inferenceSession
            )

            val publicType = transformAnonymousTypeIfNeeded(
                functionDescriptor, function, type, trace, anonymousTypeTransformers, languageVersionSettings
            )

            val approximatedType = typeApproximator.approximateDeclarationType(publicType, false)

            val sanitizedType = declarationReturnTypeSanitizer.sanitizeReturnType(
                approximatedType, wrappedTypeFactory, trace, languageVersionSettings
            )

            functionsTypingVisitor.checkTypesForReturnStatements(function, trace, sanitizedType)

            sanitizedType
        }

    }


    /**
     * 解析值参数描述符
     *
     * 将函数参数解析为值参数描述符，包括：
     * - 解析参数注解
     * - 处理单下划线匿名参数
     * - 处理主构造函数参数的 let/var 声明
     * - 创建参数描述符（包括解构声明支持）
     *
     * @param scope 解析作用域
     * @param owner 拥有该参数的函数描述符
     * @param valueParameter 参数 PSI 元素
     * @param index 参数索引
     * @param type 参数类型
     * @param trace 绑定追踪器
     * @param additionalAnnotations 额外的注解
     * @param inferenceSession 类型推断会话
     * @return 值参数描述符
     */
    fun resolveValueParameterDescriptor(
        scope: LexicalScope,
        owner: FunctionDescriptor,
        valueParameter: CjParameter,
        index: Int,
        type: CangJieType,
        trace: BindingTrace,
        additionalAnnotations: Annotations,
        inferenceSession: InferenceSession?
    ): ValueParameterDescriptorImpl {
//        CangJieType varargElementType = null;
        val variableType = type

        //        if (valueParameter.hasModifier(VARARG_KEYWORD)) {
//            varargElementType = type;
//            variableType = getVarargParameterType(type);
//        }
        val valueParameterAnnotations =
            resolveValueParameterAnnotations(scope, valueParameter, trace, additionalAnnotations)


        // NB: let/var for parameter is only allowed in primary constructors where single underscore names are still prohibited.
        // The problem with let/var is that when lazy resolveName try to find their descriptor, it searches through the member scope
        // of containing class where, it can not find a descriptor with special name.
        // Thus, to preserve behavior, we don't use a special name for let/var.
        val parameterName =
            if (!valueParameter.hasLetOrVar() && valueParameter.isSingleUnderscore)
                anonymousParameterName(index)
            else
                CjPsiUtil.safeName(valueParameter.name)

        val valueParameterDescriptor = createWithDestructuringDeclarations(
            owner,
            null,
            index,
            valueParameterAnnotations,
            parameterName,
            valueParameter.isNamed,
            variableType,
            valueParameter.hasDefaultValue(),  //                varargElementType,

            valueParameter.toSourceElement(),

        )

        trace.record(BindingContext.VALUE_PARAMETER, valueParameter, valueParameterDescriptor)
        return valueParameterDescriptor
    }

    /**
     * 解析值参数注解
     *
     * 解析参数上的注解。
     * 对于主构造函数参数（带 let/var），分割注解以区分参数注解和属性注解。
     *
     * @param scope 解析作用域
     * @param parameter 参数 PSI 元素
     * @param trace 绑定追踪器
     * @param additionalAnnotations 额外的注解（来自注解分割器）
     * @return 合并后的注解集合
     */
    private fun resolveValueParameterAnnotations(
        scope: LexicalScope,
        parameter: CjParameter,
        trace: BindingTrace,
        additionalAnnotations: Annotations
    ): Annotations {
        parameter.modifierList ?: return additionalAnnotations
        val annotations = parameter.annotations
        val allAnnotations = annotationResolver.resolveAnnotationsWithoutArguments(scope, annotations, trace)
        if (!parameter.hasLetOrVar()) {
            return CompositeAnnotations(allAnnotations, additionalAnnotations)
        }

        val splitter = AnnotationSplitter(
            storageManager, allAnnotations, setOf(AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER)
        )
        return CompositeAnnotations(
            splitter.getAnnotationsForTarget(AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER),
            additionalAnnotations
        )
    }

    /**
     * 解析类型参数描述符（扩展到可写作用域）
     *
     * 解析类型参数描述符并将其添加到可写作用域中，供后续解析使用。
     * 这个重载方法会自动将类型参数添加到扩展作用域。
     *
     * @param containingDescriptor 包含该类型参数的描述符（函数或类型别名）
     * @param extensibleScope 可写作用域，解析的类型参数将添加到此作用域
     * @param scopeForAnnotationsResolve 用于解析注解的作用域
     * @param typeParameters 类型参数 PSI 元素列表
     * @param trace 绑定追踪器
     * @return 类型参数描述符列表
     */
    fun resolveTypeParametersForDescriptor(
        containingDescriptor: DeclarationDescriptor,
        extensibleScope: LexicalWritableScope,
        scopeForAnnotationsResolve: LexicalScope,
        typeParameters: List<CjTypeParameter>,
        trace: BindingTrace
    ): List<TypeParameterDescriptorImpl> {
        val descriptors =
            resolveTypeParametersForDescriptor(containingDescriptor, scopeForAnnotationsResolve, typeParameters, trace)
        for (descriptor in descriptors) {
            extensibleScope.addClassifierDescriptor(descriptor)
        }
        return descriptors
    }

    /**
     * 解析类型参数描述符（内部实现）
     *
     * 解析类型参数描述符列表。
     * 此方法用于函数、属性和类型别名的类型参数。
     *
     * @param containingDescriptor 包含该类型参数的描述符
     * @param scopeForAnnotationsResolve 用于解析注解的作用域
     * @param typeParameters 类型参数 PSI 元素列表
     * @param trace 绑定追踪器
     * @return 类型参数描述符列表
     */
    private fun resolveTypeParametersForDescriptor(
        containingDescriptor: DeclarationDescriptor,
        scopeForAnnotationsResolve: LexicalScope,
        typeParameters: List<CjTypeParameter>,
        trace: BindingTrace
    ): List<TypeParameterDescriptorImpl> {
        assert(
            containingDescriptor is FunctionDescriptor ||  //                containingDescriptor instanceof PropertyDescriptor ||
                    containingDescriptor is TypeAliasDescriptor
        ) { "This method should be called for functions, properties, or type aliases, got $containingDescriptor" }

        val result: MutableList<TypeParameterDescriptorImpl> = ArrayList()
        var i = 0
        val typeParametersSize = typeParameters.size
        while (i < typeParametersSize) {
            val typeParameter = typeParameters[i]
            result.add(
                resolveTypeParameterForDescriptor(
                    containingDescriptor,
                    scopeForAnnotationsResolve,
                    typeParameter,
                    i,
                    trace
                )
            )
            i++
        }
        return result
    }

    /**
     * 解析单个类型参数描述符
     *
     * 创建类型参数描述符，包括：
     * - 检查型变（variance）是否合法（类型参数应为不变）
     * - 解析注解
     * - 创建支持延迟修改的类型参数描述符
     * - 设置循环泛型上界检测
     *
     * @param containingDescriptor 包含该类型参数的描述符
     * @param scopeForAnnotationsResolve 用于解析注解的作用域
     * @param typeParameter 类型参数 PSI 元素
     * @param index 类型参数在列表中的索引
     * @param trace 绑定追踪器
     * @return 类型参数描述符
     */
    private fun resolveTypeParameterForDescriptor(
        containingDescriptor: DeclarationDescriptor,
        scopeForAnnotationsResolve: LexicalScope,
        typeParameter: CjTypeParameter,
        index: Int,
        trace: BindingTrace
    ): TypeParameterDescriptorImpl {
        if (typeParameter.variance != Variance.INVARIANT) {
            trace.report(VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED.on(typeParameter))
        }

        val annotations =
            annotationResolver.resolveAnnotationsWithArguments(
                scopeForAnnotationsResolve,
                typeParameter.annotations,
                trace
            )

        val typeParameterDescriptor = createForFurtherModification(
            containingDescriptor,
            annotations,

            typeParameter.variance,
            CjPsiUtil.safeName(typeParameter.name),
            index,
            typeParameter.toSourceElement(),
            { type: CangJieType? ->
                if (containingDescriptor !is TypeAliasDescriptor) {
                    trace.report(CYCLIC_GENERIC_UPPER_BOUND.on(typeParameter))
                }
                null
            },
            supertypeLoopsResolver,
            storageManager
        )
        trace.record(BindingContext.TYPE_PARAMETER, typeParameter, typeParameterDescriptor)
        return typeParameterDescriptor
    }

    /**
     * 解析类型别名描述符
     *
     * 将类型别名声明解析为类型别名描述符，包括：
     * - 解析可见性和注解
     * - 解析类型参数（如果存在）
     * - 解析泛型约束
     * - 检查类型别名参数不能有上界（这是语言限制）
     * - 使用递归容错的惰性值解析别名类型和展开类型
     *
     * @param containingDeclaration 包含该类型别名的描述符
     * @param scope 解析作用域
     * @param typeAlias 类型别名 PSI 元素
     * @param trace 绑定追踪器
     * @return 类型别名描述符
     */
    fun resolveTypeAliasDescriptor(
        containingDeclaration: DeclarationDescriptor,
        scope: LexicalScope,
        typeAlias: CjTypeAlias,
        trace: BindingTrace
    ): TypeAliasDescriptor {
        //        if (!(containingDeclaration instanceof PackageFragmentDescriptor) &&
//                !(containingDeclaration instanceof ScriptDescriptor)) {
//            trace.report(TOPLEVEL_TYPEALIASES_ONLY.on(typeAlias));
//        }

        val annotations = typeAlias.annotations
        typeAlias.modifierList
        val visibility =
            resolveVisibilityFromModifiers(typeAlias, getDefaultVisibility(typeAlias, containingDeclaration))

        val allAnnotations = annotationResolver.resolveAnnotationsWithArguments(scope, annotations, trace)
        val name = CjPsiUtil.safeName(typeAlias.name)
        val sourceElement = typeAlias.toSourceElement()
        val typeAliasDescriptor = create(
            storageManager, trace, containingDeclaration, allAnnotations, name, sourceElement, visibility
        )

        var typeParameterDescriptors: List<TypeParameterDescriptorImpl>
        var scopeWithTypeParameters: LexicalScope?
        run {
            val typeParameters = typeAlias.typeParameters
            if (typeParameters.isEmpty()) {
                scopeWithTypeParameters = scope
                typeParameterDescriptors = emptyList()
            } else {
                val writableScope = LexicalWritableScope(
                    scope, containingDeclaration, false, TraceBasedLocalRedeclarationChecker(
                        trace,
                        overloadChecker
                    ),
                    LexicalScopeKind.TYPE_ALIAS_HEADER
                )
                typeParameterDescriptors = resolveTypeParametersForDescriptor(
                    typeAliasDescriptor, writableScope, scope, typeParameters, trace
                )
                writableScope.freeze()
                checkNoGenericBoundsOnTypeAliasParameters(typeAlias, trace)
                resolveGenericBounds(typeAlias, typeAliasDescriptor, writableScope, typeParameterDescriptors, trace)
                scopeWithTypeParameters = writableScope
            }
        }

        val typeReference = typeAlias.getTypeReference()
        if (typeReference == null) {
            typeAliasDescriptor.initialize(
                typeParameterDescriptors,
                createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_ALIAS, name.asString()),
                createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_ALIAS, name.asString())
            )
        } else {
            typeAliasDescriptor.initialize(
                typeParameterDescriptors,
                storageManager.createRecursionTolerantLazyValue(
                    { typeResolver.resolveAbbreviatedType(scopeWithTypeParameters!!, typeReference, trace) },
                    createErrorType(ErrorTypeKind.RECURSIVE_TYPE_ALIAS, typeAliasDescriptor.name.asString())
                ),
                storageManager.createRecursionTolerantLazyValue(
                    { typeResolver.resolveExpandedTypeForTypeAlias(typeAliasDescriptor) },
                    createErrorType(ErrorTypeKind.RECURSIVE_TYPE_ALIAS, typeAliasDescriptor.name.asString())
                )
            )
        }

        trace.record(BindingContext.TYPE_ALIAS, typeAlias, typeAliasDescriptor)
        return typeAliasDescriptor
    }

    /**
     * 解析模式变量描述符
     *
     * 解析模式匹配中的变量声明（例如 `let (x, y) = tuple`）。
     * 通过模式匹配类型访问器处理模式解构，然后从绑定上下文中提取变量描述符。
     *
     * @param name 要查找的变量名
     * @param container 包含该变量的描述符
     * @param scopeForDeclarationResolution 用于声明解析的作用域
     * @param variableDeclaration 模式变量 PSI 元素
     * @param trace 绑定追踪器
     * @param dataFlowInfo 数据流信息
     * @param inferenceSession 类型推断会话
     * @return 匹配给定名称的变量描述符列表（可能为多个，因为模式可能包含多个同名绑定）
     */
    fun resolveVariableDescriptorByPattern(
        name: Name,
        container: DeclarationDescriptor,
        scopeForDeclarationResolution: LexicalScope,
//        scopeForClassInitializerResolution: LexicalScope,
        variableDeclaration: CjPatternVariable,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession
    ): List<VariableDescriptor> {
        val context =
            expressionTypingServices.getNewContext(scopeForDeclarationResolution, trace, dataFlowInfo, inferenceSession)



        expressionTypingServices.expressionTypingComponents.patternMatchingTypingVisitor.visitPatternVariable(
            variableDeclaration, context
        )
////当前声明具有的所有变量声明
        val variables = variableDeclaration.pattern.getAllBindings().mapNotNull {
            trace[BindingContext.VARIABLE, it]
        }
        return variables.filter {
            it.name == name
        }

    }

    /**
     * 解析变量描述符
     *
     * 将普通变量声明（不是属性）解析为变量描述符，包括：
     * - 解析可见性和修饰性
     * - 处理类型参数（如果存在）
     * - 解析泛型约束
     * - 推断或解析变量类型
     * - 设置常量值（如果适用）
     * - 检查接口中不允许有变量声明
     *
     * @param container 包含该变量的描述符
     * @param scopeForDeclarationResolution 用于声明解析的作用域
     * @param scopeForInitializerResolution 用于初始化器解析的作用域
     * @param variableDeclaration 变量声明 PSI 元素
     * @param trace 绑定追踪器
     * @param dataFlowInfo 数据流信息
     * @param inferenceSession 类型推断会话
     * @return 变量描述符
     */
    fun resolveVariableDescriptor(
        container: DeclarationDescriptor,
        scopeForDeclarationResolution: LexicalScope,
        scopeForInitializerResolution: LexicalScope,
        variableDeclaration: CjVariable<*>,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession
    ): VariableDescriptor {
        val variableInfo = createFromProperty(variableDeclaration)

        variableDeclaration.modifierList
        val isVar = variableDeclaration.isVar

        val visibility =
            resolveVisibilityFromModifiers(variableDeclaration, getDefaultVisibility(variableDeclaration, container))

        if (container is ClassDescriptor)
            resolveMemberModalityFromModifiers(
                variableDeclaration,
                getDefaultModality(container, visibility, variableInfo.hasBody),
                trace.bindingContext, container
            )
        else
            Modality.FINAL
        //暂时使用EMPTY

        val variableDescriptor = create(
            container,
            CjPsiUtil.safeName(variableDeclaration.name),


            visibility,
            isVar,


            variableDeclaration.toSourceElement()

        )

        var typeParameterDescriptors: List<TypeParameterDescriptorImpl>
        var scopeForDeclarationResolutionWithTypeParameters: LexicalScope?
        var scopeForInitializerResolutionWithTypeParameters: LexicalScope?
        var receiverType: CangJieType? = null
        run {
            val typeParameters = variableDeclaration.typeParameters
            if (typeParameters.isEmpty()) {
                scopeForDeclarationResolutionWithTypeParameters = scopeForDeclarationResolution
                scopeForInitializerResolutionWithTypeParameters = scopeForInitializerResolution
                typeParameterDescriptors = emptyList()
            } else {
                val writableScopeForDeclarationResolution = LexicalWritableScope(
                    scopeForDeclarationResolution, container, false, TraceBasedLocalRedeclarationChecker(
                        trace,
                        overloadChecker
                    ),
                    LexicalScopeKind.PROPERTY_HEADER
                )
                val writableScopeForInitializerResolution = LexicalWritableScope(
                    scopeForInitializerResolution, container, false, LocalRedeclarationChecker.DO_NOTHING,
                    LexicalScopeKind.PROPERTY_HEADER
                )
                typeParameterDescriptors = resolveTypeParametersForDescriptor(
                    variableDescriptor,
                    scopeForDeclarationResolution, typeParameters, trace
                )
                for (descriptor in typeParameterDescriptors) {
                    writableScopeForDeclarationResolution.addClassifierDescriptor(descriptor)
                    writableScopeForInitializerResolution.addClassifierDescriptor(descriptor)
                }
                writableScopeForDeclarationResolution.freeze()
                writableScopeForInitializerResolution.freeze()
                resolveGenericBounds(
                    variableDeclaration,
                    variableDescriptor,
                    writableScopeForDeclarationResolution,
                    typeParameterDescriptors,
                    trace
                )
                scopeForDeclarationResolutionWithTypeParameters = writableScopeForDeclarationResolution
                scopeForInitializerResolutionWithTypeParameters = writableScopeForInitializerResolution
            }
        }



        val scopeForInitializer = makeScopeForVariableInitializer(
            scopeForInitializerResolutionWithTypeParameters!!, variableDescriptor
        )
        val propertyType = variableInfo.variableType
        val typeIfKnown = propertyType
            ?: variableTypeAndInitializerResolver.resolveTypeOptional(
                variableDescriptor, scopeForInitializer,
                variableDeclaration, dataFlowInfo, inferenceSession,
                trace,  /* local = */false
            )
        val type = typeIfKnown ?: builtIns.unitType
        //        assert type != null : "At least getter type must be initialized via resolvePropertyGetterDescriptor";
        variableTypeAndInitializerResolver.setConstantForVariableIfNeeded(
            variableDescriptor, scopeForInitializer, variableDeclaration, dataFlowInfo, type, inferenceSession, trace
        )

        variableDescriptor.setType(
            type, typeParameterDescriptors, getDispatchReceiverParameterIfNeeded(container)
        )


        trace.record(BindingContext.VARIABLE, variableDeclaration, variableDescriptor)


        if (container is ClassDescriptor && container.kind == ClassKind.INTERFACE) {
            trace.report(INTERFACE_BODY_NO_VARIABLES.on(variableDeclaration))
        }

        return variableDescriptor
    }

    /**
     * 上界检查请求
     *
     * 用于收集类型参数的上界约束检查请求，以便批量检查约束的合法性。
     *
     * @param typeParameterName 类型参数名称
     * @param upperBound 上界类型引用 PSI 元素
     * @param upperBoundType 解析得到的上界类型
     */
    class UpperBoundCheckRequest(
        val typeParameterName: Name?,
        val upperBound: CjTypeReference,
        val upperBoundType: CangJieType
    )

    /**
     * 伴生对象
     *
     * 包含描述符解析过程中的静态辅助方法，主要包括：
     * - 默认可见性和修饰性计算
     * - 超类型处理
     * - 类型上下界检查
     * - 匿名类型转换
     * - 外部类实例检查
     */
    companion object {
        /**
         * 添加有效超类型
         *
         * 将非错误的超类型添加到超类型列表中。
         * 用于过滤掉解析失败的超类型。
         *
         * @param supertypes 超类型列表
         * @param declaredSupertype 声明的超类型
         */
        private fun addValidSupertype(supertypes: MutableList<CangJieType>, declaredSupertype: CangJieType) {
            if (!declaredSupertype.isError) {
                supertypes.add(declaredSupertype)
            }
        }

        /**
         * 检查集合中是否包含类类型
         *
         * 检查类型集合中是否包含 CLASS、ENUM 或 STRUCT 类型（不包括 INTERFACE）。
         *
         * @param result 类型集合
         * @return 如果包含类类型则返回 true
         */
        private fun containsClass(result: Collection<CangJieType>): Boolean {
            for (type in result) {
                val descriptor = type.constructor.declarationDescriptor
                if (descriptor is ClassDescriptor && descriptor.kind != ClassKind.INTERFACE) {
                    return true
                }
            }
            return false
        }

        /**
         * 创建并记录对象的主构造函数
         *
         * 为 object 声明创建主构造函数描述符，并在绑定上下文中记录映射关系。
         * object 声明的主构造函数是隐式的，不带参数。
         *
         * @param object 对象类型声明 PSI 元素
         * @param classDescriptor 对象对应的类描述符
         * @param trace 绑定追踪器
         * @return 创建的主构造函数描述符
         */
        fun createAndRecordPrimaryConstructorForObject(
            `object`: CjPureTypeStatement?,
            classDescriptor: ClassDescriptor,
            trace: BindingTrace
        ): ClassConstructorDescriptorImpl {
            val constructorDescriptor =
                DescriptorFactory.createPrimaryConstructorForObject(classDescriptor, `object`.toSourceElement())
            if (`object` is PsiElement) {
                val primaryConstructor = `object`.primaryConstructor
                trace.record(
                    BindingContext.CONSTRUCTOR,
                    primaryConstructor ?: `object` as PsiElement, constructorDescriptor
                )
            }
            return constructorDescriptor
        }

        /**
         * 检查嵌套声明是否在外部类或其子类内部
         *
         * 递归检查嵌套声明是否在目标类或其子类的内部。
         * 这用于验证访问外部类实例的合法性。
         *
         * @param nested 嵌套声明描述符（可为 null）
         * @param outer 外部类描述符
         * @return 如果嵌套声明在外部类或其子类内部则返回 true
         */
        private fun isInsideOuterClassOrItsSubclass(nested: DeclarationDescriptor?, outer: ClassDescriptor): Boolean {
            if (nested == null) return false

            if (nested is ClassDescriptor && isSubclass(nested, outer)) return true

            return isInsideOuterClassOrItsSubclass(nested.containingDeclaration, outer)
        }

        /**
         * 检查是否有外部类实例
         *
         * 检查在给定作用域中访问目标类是否需要外部类实例。
         * 这用于验证内部类和嵌套类的实例化和访问。
         *
         * @param scope 词法作用域
         * @param trace 绑定追踪器
         * @param reportErrorsOn 报告错误的 PSI 元素
         * @param target 目标类描述符
         * @return 如果可以访问外部类实例则返回 true
         */
        fun checkHasOuterClassInstance(
            scope: LexicalScope,
            trace: BindingTrace,
            reportErrorsOn: PsiElement,
            target: ClassDescriptor
        ): Boolean {
            var classDescriptor = getContainingClass(scope)


            if (!isInsideOuterClassOrItsSubclass(classDescriptor, target)) {
                return true
            }

            while (classDescriptor != null) {
                if (isSubclass(classDescriptor, target)) {
                    return true
                }
                classDescriptor = getParentOfType(
                    classDescriptor,
                    ClassDescriptor::class.java, true
                )
            }
            return true
        }

        /**
         * 获取包含的类
         *
         * 从词法作用域中获取包含该作用域的类描述符。
         * 通过递归查找作用域的所有者描述符，直到找到 ClassDescriptor。
         *
         * @param scope 词法作用域
         * @return 包含该作用域的类描述符，如果不在类内部则返回 null
         */
        fun getContainingClass(scope: LexicalScope): ClassDescriptor? {
            return getParentOfType(
                scope.ownerDescriptor,
                ClassDescriptor::class.java, false
            )
        }

        /**
         * 获取默认可见性
         *
         * 根据声明的上下文确定默认可见性级别：
         * - 接口成员默认为 PUBLIC
         * - 其他声明默认为 INTERNAL
         *
         * @param modifierListOwner 修饰符列表拥有者（可为 null）
         * @param containingDescriptor 包含该声明的描述符（可为 null）
         * @return 默认可见性级别
         */
        fun getDefaultVisibility(
            modifierListOwner: CjModifierListOwner?,
            containingDescriptor: DeclarationDescriptor?
        ): DescriptorVisibility {


            if (containingDescriptor is ClassDescriptor && containingDescriptor.kind == ClassKind.INTERFACE) {
                return DescriptorVisibilities.PUBLIC
            }

            return DescriptorVisibilities.INTERNAL
        }

        /**
         * 获取默认修饰性
         *
         * 根据声明的上下文确定默认修饰性（modality）：
         * - 接口中没有函数体的成员默认为 ABSTRACT
         * - 接口中非 private 的成员默认为 OPEN
         * - 其他情况默认为 FINAL
         *
         * @param containingDescriptor 包含该声明的描述符（可为 null）
         * @param visibility 声明的可见性
         * @param isBodyPresent 是否存在函数体或初始化器
         * @return 默认修饰性
         */
        fun getDefaultModality(
            containingDescriptor: DeclarationDescriptor?,
            visibility: DescriptorVisibility,
            isBodyPresent: Boolean
        ): Modality {
            val defaultModality: Modality
            if (containingDescriptor is ClassDescriptor) {
                val isTrait = containingDescriptor.kind == ClassKind.INTERFACE
                val isDefinitelyAbstract = isTrait && !isBodyPresent
                val basicModality =
                    if (isTrait && !DescriptorVisibilities.isPrivate(visibility)) Modality.OPEN else Modality.FINAL
                defaultModality = if (isDefinitelyAbstract) Modality.ABSTRACT else basicModality
            } else {
                defaultModality = Modality.FINAL
            }
            return defaultModality
        }

        /**
         * 检查冲突的上界约束
         *
         * 检查类型参数的所有上界约束是否冲突，即交集是否为 Nothing 类型。
         * 如果多个上界没有公共子类型，则报告 CONFLICTING_UPPER_BOUNDS 错误。
         *
         * @param trace 绑定追踪器
         * @param parameter 类型参数描述符
         * @param typeParameter 类型参数 PSI 元素
         */
        fun checkConflictingUpperBounds(
            trace: BindingTrace,
            parameter: TypeParameterDescriptor,
            typeParameter: CjTypeParameter
        ) {
            if (isNothing(TypeIntersector.getUpperBoundsAsType(parameter))) {
                trace.report(CONFLICTING_UPPER_BOUNDS.on(typeParameter, parameter))
            }
        }

        //    @NotNull
        //    /*package*/ static CangJieType transformAnonymousTypeIfNeeded(
        //            @NotNull DeclarationDescriptorWithVisibility descriptor,
        //            @NotNull CjDeclaration declaration,
        //            @NotNull CangJieType type,
        //            @NotNull BindingTrace trace,
        //            @NotNull Iterable<DeclarationSignatureAnonymousTypeTransformer> anonymousTypeTransformers,
        //            @NotNull LanguageVersionSettings languageVersionSettings
        //    ) {
        //        for (DeclarationSignatureAnonymousTypeTransformer transformer : anonymousTypeTransformers) {
        //            CangJieType transformedType = transformer.transformAnonymousType(descriptor, type);
        //            if (transformedType != null) {
        //                return transformedType;
        //            }
        //        }
        //
        //        ClassifierDescriptor classifier = type.getConstructor().getDeclarationDescriptor();
        //        if (classifier == null /*|| !DescriptorUtils.isAnonymousObject(classifier)*/ || DescriptorUtils.isLocal(descriptor)) {
        //            return type;
        //        }
        //
        //        boolean isPrivate = DescriptorVisibilities.isPrivate(descriptor.getVisibility());
        //        boolean isInlineFunction = descriptor instanceof SimpleFunctionDescriptor && ((SimpleFunctionDescriptor) descriptor).isInline();
        //        boolean isAnonymousReturnTypesInPrivateInlineFunctionsForbidden =
        //                languageVersionSettings.supportsFeature(LanguageFeature.ApproximateAnonymousReturnTypesInPrivateInlineFunctions);
        //
        //        if (!isPrivate || (isInlineFunction && isAnonymousReturnTypesInPrivateInlineFunctionsForbidden)) {
        //            if (type.getConstructor().getSupertypes().size() == 1) {
        //                CangJieType approximatingSuperType = type.getConstructor().getSupertypes().iterator().next();
        //                CangJieType substitutedSuperType;
        //                MemberScope memberScope = type.getMemberScope();
        //
        //                if (memberScope instanceof SubstitutingScope) {
        //                    substitutedSuperType = ((SubstitutingScope) memberScope).substitute(approximatingSuperType);
        //                } else {
        //                    substitutedSuperType = approximatingSuperType;
        //                }
        //
        //                UnwrappedType unwrapped = type.unwrap();
        //                boolean lowerNullable = FlexibleTypesKt.lowerIfFlexible(unwrapped).isMarkedOption();
        //                boolean upperNullable = FlexibleTypesKt.upperIfFlexible(unwrapped).isMarkedOption();
        //                if (languageVersionSettings.supportsFeature(LanguageFeature.KeepNullabilityWhenApproximatingLocalType)) {
        //                    if (lowerNullable != upperNullable) {
        //                        return CangJieTypeFactory.flexibleType(
        //                                FlexibleTypesKt.lowerIfFlexible(substitutedSuperType),
        //                                FlexibleTypesKt.upperIfFlexible(substitutedSuperType).makeOptionalAsSpecified(true));
        //                    }
        //                    return TypeUtils.makeOptionalIfNeeded(substitutedSuperType, upperNullable);
        //                } else if (upperNullable) {
        //                    if (lowerNullable) {
        //                        trace.report(APPROXIMATED_LOCAL_TYPE_WILL_BECOME_NULLABLE.on(declaration, substitutedSuperType));
        //                    } else {
        //                        trace.report(APPROXIMATED_LOCAL_TYPE_WILL_BECOME_FLEXIBLE.on(declaration, substitutedSuperType));
        //                    }
        //                }
        //                return substitutedSuperType;
        //            }
        //            else {
        //                trace.report(AMBIGUOUS_ANONYMOUS_TYPE_INFERRED.on(declaration, type.getConstructor().getSupertypes()));
        //            }
        //        }
        //
        //        return type;
        //    }
        /**
         * 检查类型上界列表
         *
         * 批量检查类型参数的上界约束，包括：
         * - 检查重复的上界
         * - 检查是否有多个类类型上界（只允许一个）
         * - 对每个上界调用 checkUpperBoundType 进行详细检查
         *
         * @param trace 绑定追踪器
         * @param requests 上界检查请求列表
         * @param hasOverrideModifier 是否有 override 修饰符
         */
        fun checkUpperBoundTypes(
            trace: BindingTrace,
            requests: List<UpperBoundCheckRequest>,
            hasOverrideModifier: Boolean
        ) {
            if (requests.isEmpty()) return

            val classBoundEncountered: MutableSet<Name?> = HashSet()
            val allBounds: MutableSet<Pair<Name?, TypeConstructor>> = HashSet()

            for (request in requests) {
                val typeParameterName = request.typeParameterName
                val upperBound = request.upperBoundType
                val upperBoundElement = request.upperBound

                if (!upperBound.isError) {
                    if (!allBounds.add(Pair<Name?, TypeConstructor>(typeParameterName, upperBound.constructor))) {
                        trace.report(REPEATED_BOUND.on(upperBoundElement))
                    } else {
                        val classDescriptor = getClassDescriptor(upperBound)
                        if (classDescriptor != null) {
                            val kind = classDescriptor.kind
                            if (kind == ClassKind.CLASS || kind == ClassKind.ENUM || kind == ClassKind.STRUCT) {
                                if (!classBoundEncountered.add(typeParameterName)) {
                                    trace.report(ONLY_ONE_CLASS_BOUND_ALLOWED.on(upperBoundElement))
                                }
                            }
                        }
                    }
                }

                checkUpperBoundType(upperBoundElement, upperBound, trace, hasOverrideModifier)
            }
        }

        /**
         * 检查单个类型上界
         *
         * 检查单个类型参数上界的合法性。
         * 当前实现为空方法，保留用于未来扩展（如检查 final 类型、动态类型等）。
         *
         * @param upperBound 上界类型引用 PSI 元素（可为 null）
         * @param upperBoundType 解析得到的上界类型
         * @param trace 绑定追踪器（可为 null）
         * @param hasOverrideModifier 是否有 override 修饰符
         */
        fun checkUpperBoundType(
            upperBound: CjTypeReference?,
            upperBoundType: CangJieType,
            trace: BindingTrace?,
            hasOverrideModifier: Boolean
        ) {
//        if (!hasOverrideModifier && !TypeUtils.canHaveSubtypes(CangJieTypeChecker.DEFAULT, upperBoundType)) {
//            trace.report(FINAL_UPPER_BOUND.on(upperBound, upperBoundType));
//        }
//        if (DynamicTypesKt.isDynamic(upperBoundType)) {
//            trace.report(DYNAMIC_UPPER_BOUND.on(upperBound));
//        }
//        if (FunctionTypesKt.isExtensionFunctionType(upperBoundType)) {
//            trace.report(UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE.on(upperBound));
//        }
//        if (DefinitelyNonNullableTypesKt.containsIncorrectExplicitDefinitelyNonNullableType(upperBoundType)) {
//            trace.report(INCORRECT_LEFT_COMPONENT_OF_INTERSECTION.on(upperBound));
//        }
        }

        /**
         * 检查类型别名参数不能有泛型约束
         *
         * 检查类型别名的类型参数是否错误地声明了上界（extends 子句）。
         * 仓颉语言规则：类型别名的类型参数不允许有上界约束。
         *
         * @param typeAlias 类型别名 PSI 元素
         * @param trace 绑定追踪器
         */
        private fun checkNoGenericBoundsOnTypeAliasParameters(typeAlias: CjTypeAlias, trace: BindingTrace) {
            for (typeParameter in typeAlias.typeParameters) {
                val bound = typeParameter.extendsBound
                if (bound != null) {
                    trace.report(BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED.on(bound))
                }
            }
        }

        /**
         * 解析超类型列表条目
         *
         * 解析类的继承列表（extends/implements）中的所有类型引用，包括：
         * - 解析每个类型引用为 CangJieType
         * - 检查是否为动态类型（不允许）
         * - 检查可空超类型
         * - 检查类型投影（泛型参数的 in/out 修饰符）
         *
         * @param extensibleScope 解析作用域
         * @param delegationSpecifiers 超类型列表条目（PSI 元素列表）
         * @param resolver 类型解析器
         * @param trace 绑定追踪器
         * @param checkBounds 是否检查类型参数的上界约束
         * @return 解析得到的超类型集合
         */
        private fun resolveSuperTypeListEntries(
            extensibleScope: LexicalScope,
            delegationSpecifiers: List<CjSuperTypeListEntry>,
            resolver: TypeResolver,
            trace: BindingTrace,
            checkBounds: Boolean
        ): Collection<CangJieType> {
            if (delegationSpecifiers.isEmpty()) {
                return emptyList()
            }
            val result: MutableCollection<CangJieType> = Lists.newArrayList()
            for (delegationSpecifier in delegationSpecifiers) {
                val typeReference = delegationSpecifier.typeReference
                if (typeReference != null) {
                    val supertype = resolver.resolveType(extensibleScope, typeReference, trace, checkBounds)
                    if (supertype.isDynamic()) {
                        trace.report(DYNAMIC_SUPERTYPE.on(typeReference))
                    } else {
                        result.add(supertype)
                        val bareSuperType =
                            checkNullableSupertypeAndStripQuestionMarks(trace, typeReference.typeElement)
                        checkProjectionsInImmediateArguments(trace, bareSuperType, supertype)
                    }
                } else {
                    result.add(createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, delegationSpecifier.text))
                }
            }
            return result
        }

        /**
         * 检查顶层类型参数的类型投影
         *
         * 检查超类型的顶层类型参数是否使用了类型投影（in/out 修饰符）。
         * 当前实现为空方法，保留用于未来扩展类型投影检查。
         *
         * @param trace 绑定追踪器
         * @param typeElement 类型元素 PSI（可为 null）
         * @param type 解析得到的类型
         */
        private fun checkProjectionsInImmediateArguments(
            trace: BindingTrace,
            typeElement: CjTypeElement?,
            type: CangJieType
        ) {
//        if (typeElement == null) return;

//        boolean hasProjectionsInWrittenArguments = false;
//        处理类型投影
//        if (typeElement instanceof CjUserType userType) {
//            List<CjTypeProjection> typeArguments = userType.getTypeArguments();
//            for (CjTypeProjection typeArgument : typeArguments) {
//                if (typeArgument.getProjectionKind() != CjProjectionKind.NONE) {
//                    trace.report(PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE.on(typeArgument));
//                    hasProjectionsInWrittenArguments = true;
//                }
//            }
//        }

            // If we have an abbreviated type (written with a type alias), it still can contain type projections in top-level arguments.
//        if (!CangJieTypeKt.isError(type) && SpecialTypesKt.getAbbreviatedType(type) != null && !hasProjectionsInWrittenArguments) {
            // Only interface inheritance should be checked here.
            // Corresponding check for classes is performed for type alias constructor calls in CandidateResolver.
//            if (TypeUtilKt.isInterface(type) && TypeUtilKt.containsTypeProjectionsInTopLevelArguments(type)) {
//                trace.report(EXPANDED_TYPE_CANNOT_BE_INHERITED.on(typeElement, type));
//            }
//        }
        }

        /**
         * 检查并移除可空超类型的问号标记
         *
         * 检查超类型是否错误地声明为可空类型（带 ? 后缀）。
         * 当前实现为空方法，保留用于未来扩展可空超类型检查。
         *
         * @param trace 绑定追踪器
         * @param typeElement 类型元素 PSI（可为 null）
         * @return 移除可空标记后的类型元素，如果不是可空类型则返回原值
         */
        private fun checkNullableSupertypeAndStripQuestionMarks(
            trace: BindingTrace,
            typeElement: CjTypeElement?
        ): CjTypeElement? {
//        while (typeElement instanceof CjNullableType) {
//            CjNullableType nullableType = (CjNullableType) typeElement;
//            typeElement = nullableType.getInnerType();
//            // report only for innermost '?', the rest gets a 'redundant' warning
//            if (!(typeElement instanceof CjNullableType) && typeElement != null) {
//                trace.report(NULLABLE_SUPERTYPE.on(nullableType));
//            }
//        }
            return typeElement
        }

        /**
         * 按需转换匿名类型
         *
         * 对于非私有声明中的匿名对象类型，将其转换为其超类型以避免暴露匿名类型。
         * 这确保了公共 API 不会暴露内部实现细节（匿名对象）。
         *
         * 转换规则：
         * - 私有声明保持匿名类型
         * - 局部声明保持匿名类型
         * - 非私有声明且匿名对象只有一个超类型时，转换为该超类型
         * - 非私有声明且匿名对象有多个超类型时，报告歧义错误
         *
         * @param descriptor 声明描述符（必须有可见性）
         * @param declaration 声明 PSI 元素
         * @param type 原始类型
         * @param trace 绑定追踪器
         * @param anonymousTypeTransformers 自定义匿名类型转换器列表
         * @param languageVersionSettings 语言版本设置
         * @return 转换后的类型，如果不需要转换则返回原类型
         */
        fun transformAnonymousTypeIfNeeded(
            descriptor: DeclarationDescriptorWithVisibility,
            declaration: CjDeclaration,
            type: CangJieType,
            trace: BindingTrace,
            anonymousTypeTransformers: Iterable<DeclarationSignatureAnonymousTypeTransformer>,
            languageVersionSettings: LanguageVersionSettings
        ): CangJieType {
            for (transformer in anonymousTypeTransformers) {
                val transformedType = transformer.transformAnonymousType(descriptor, type)
                if (transformedType != null) {
                    return transformedType
                }
            }

            val classifier = type.constructor.declarationDescriptor
            if (classifier == null || !isAnonymousObject(classifier) || isLocal(descriptor)) {
                return type
            }

            val isPrivate = DescriptorVisibilities.isPrivate(descriptor.visibility)

            if (!isPrivate) {
//            if(type instanceof  BasicType) return type;
                if (type.constructor.supertypes.size == 1) {
                    val approximatingSuperType = type.constructor.supertypes.iterator().next()
                    val substitutedSuperType: CangJieType
                    val memberScope = type.memberScope

                    substitutedSuperType = if (memberScope is SubstitutingScope) {
                        memberScope.substitute(approximatingSuperType)
                    } else {
                        approximatingSuperType
                    }

                    //                UnwrappedType unwrapped = type.unwrap();
//                boolean lowerNullable = FlexibleTypesKt.lowerIfFlexible(unwrapped).isMarkedNullable();
//                boolean upperNullable = FlexibleTypesKt.upperIfFlexible(unwrapped).isMarkedNullable();
//                if (languageVersionSettings.supportsFeature(LanguageFeature.KeepNullabilityWhenApproximatingLocalType)) {
//                    if (lowerNullable != upperNullable) {
//                        return CangJieTypeFactory.flexibleType(
//                                FlexibleTypesKt.lowerIfFlexible(substitutedSuperType),
//                                FlexibleTypesKt.upperIfFlexible(substitutedSuperType).makeNullableAsSpecified(true));
//                    }
//                    return TypeUtils.makeOptionalIfNeeded(substitutedSuperType, upperNullable);
//                } else if (upperNullable) {
//                    if (lowerNullable) {
//                        trace.report(APPROXIMATED_LOCAL_TYPE_WILL_BECOME_NULLABLE.on(declaration, substitutedSuperType));
//                    } else {
//                        trace.report(APPROXIMATED_LOCAL_TYPE_WILL_BECOME_FLEXIBLE.on(declaration, substitutedSuperType));
//                    }
//                }
                    return substitutedSuperType
                } else {
                    trace.report(AMBIGUOUS_ANONYMOUS_TYPE_INFERRED.on(declaration, type.constructor.supertypes))
                }
            }

            return type
        }
    }
}
