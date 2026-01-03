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

import org.cangnova.cangjie.extensions.DeclarationAttributeAltererExtension
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.DiagnosticFactory1
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.caches.DeclarationChecker
import org.cangnova.cangjie.resolve.caches.DeclarationCheckerContext
import org.cangnova.cangjie.resolve.deprecation.DeprecationResolver

/**
 * 修饰符检查器
 *
 * 负责检查和解析仓颉语言中的各种修饰符，包括：
 * - 可见性修饰符（public、private、protected、internal）
 * - 修饰性（modality）（open、abstract、sealed、final）
 * - 其他修饰符（override、operator 等）
 *
 * @param declarationCheckers 声明检查器集合
 * @param moduleDescriptor 模块描述符
 * @param deprecationResolver 废弃特性解析器
 * @param missingSupertypesResolver 缺失超类型解析器
 * @param languageVersionSettings 语言版本设置
 */
class ModifiersChecker(
    val declarationCheckers: Iterable<DeclarationChecker>,
    val moduleDescriptor: ModuleDescriptor,
    val deprecationResolver: DeprecationResolver,

//val expectActualTracker:ExpectActualTracker,
    val missingSupertypesResolver: MissingSupertypesResolver,
    val languageVersionSettings: LanguageVersionSettings
) {

    /**
     * 创建带追踪的修饰符检查过程
     *
     * @param trace 绑定追踪器，用于记录诊断信息
     * @return 修饰符检查过程实例
     */
    fun withTrace(trace: BindingTrace): ModifiersCheckingProcedure {
        return ModifiersCheckingProcedure(trace)
    }

    /**
     * 修饰符检查过程
     *
     * 提供一系列方法用于检查声明的修饰符是否合法，并报告相关错误
     *
     * @param trace 绑定追踪器，用于记录诊断信息
     */
    inner class ModifiersCheckingProcedure(val trace: BindingTrace) {

        /**
         * 检查参数不能有 let 或 var 关键字
         *
         * 用于检查函数参数、构造函数参数等场景，确保参数没有使用 let/var 修饰
         *
         * @param parameter 参数 PSI 元素
         * @param diagnosticFactory 诊断工厂，用于报告错误
         */
        fun checkParameterHasNoLetOrVar(
            parameter: CjLetVarKeywordOwner,
            diagnosticFactory: DiagnosticFactory1<PsiElement, CjKeywordToken>
        ) {
            val valOrVar = parameter.letOrVarKeyword
            if (valOrVar != null) {
                trace.report(
                    diagnosticFactory.on(
                        valOrVar,
                        (valOrVar.node.elementType as CjKeywordToken)
                    )
                )
            }
        }


        /**
         * 检查声明的修饰符
         *
         * 对类、函数、属性等顶层或成员声明进行修饰符检查，包括：
         * - 嵌套类是否允许
         * - 类型参数修饰符
         * - 修饰符列表通用检查
         * - 非法 header 修饰符
         *
         * @param modifierListOwner 修饰符列表拥有者（声明 PSI 元素）
         * @param descriptor 对应的成员描述符
         */
        fun checkModifiersForDeclaration(
            modifierListOwner: CjDeclaration,
            descriptor: MemberDescriptor
        ) {
            checkNestedClassAllowed(modifierListOwner, descriptor)
            checkTypeParametersModifiers(modifierListOwner)
            checkModifierListCommon(modifierListOwner, descriptor)

            checkIllegalHeader(modifierListOwner, descriptor)
        }

        /**
         * 检查局部声明的修饰符
         *
         * 对局部变量、局部函数等局部声明进行修饰符检查
         *
         * @param modifierListOwner 修饰符列表拥有者（声明 PSI 元素）
         * @param descriptor 对应的声明描述符
         */
        fun checkModifiersForLocalDeclaration(
            modifierListOwner: CjDeclaration,
            descriptor: DeclarationDescriptor
        ) {
            checkModifierListCommon(modifierListOwner, descriptor)
        }

        /**
         * 检查非法的 header 修饰符
         *
         * 检查嵌套类等不应使用 header/expect 修饰符的场景
         *
         * @param modifierListOwner 修饰符列表拥有者
         * @param descriptor 对应的声明描述符
         */
        private fun checkIllegalHeader(
            modifierListOwner: CjModifierListOwner,
            descriptor: DeclarationDescriptor
        ) {
            // Most cases are already handled by ModifierCheckerCore, only check nested classes here
//            val modifierList:  CjModifierList = modifierListOwner.getModifierList()
//            var keyword: PsiElement? =
//                if (modifierList != null) modifierList.getModifier( CjTokens.HEADER_KEYWORD) else null
//            if (keyword != null &&
//                descriptor is  ClassDescriptor && descriptor.getContainingDeclaration() is  ClassDescriptor
//            ) {
//                trace.report(
//                    Errors.WRONG_MODIFIER_TARGET.on(
//                        keyword,
//                         CjTokens.HEADER_KEYWORD,
//                        "nested class"
//                    )
//                )
//            } else if (keyword == null && modifierList != null) {
//                keyword = modifierList.getModifier( CjTokens.EXPECT_KEYWORD)
//                if (keyword != null &&
//                    descriptor is  ClassDescriptor && descriptor.getContainingDeclaration() is  ClassDescriptor
//                ) {
//                    trace.report(
//                        Errors.WRONG_MODIFIER_TARGET.on(
//                            keyword,
//                             CjTokens.EXPECT_KEYWORD,
//                            "nested class"
//                        )
//                    )
//                }
//            }
        }

        /**
         * 检查类型参数的修饰符
         *
         * 检查泛型类型参数上的修饰符是否合法
         *
         * @param modifierListOwner 修饰符列表拥有者（通常是泛型类或函数）
         */
        fun checkTypeParametersModifiers(modifierListOwner: CjModifierListOwner) {
            if (modifierListOwner !is CjTypeParameterListOwner) return
            val typeParameters: List<CjTypeParameter> =
                modifierListOwner.typeParameters
            for (typeParameter in typeParameters) {
                ModifierCheckerCore.check(
                    typeParameter,
                    trace,
                    null,
                    languageVersionSettings
                )
            }
        }

        /**
         * 运行声明检查器
         *
         * 对声明运行所有已注册的声明检查器，包括操作符修饰符检查等
         *
         * @param declaration 声明 PSI 元素
         * @param descriptor 对应的声明描述符
         */
        fun runDeclarationCheckers(
            declaration: CjDeclaration,
            descriptor: DeclarationDescriptor
        ) {
            val context: DeclarationCheckerContext = DeclarationCheckerContext(
                trace, languageVersionSettings, deprecationResolver, moduleDescriptor, /*expectActualTracker,*/
                missingSupertypesResolver
            )
            for (checker in declarationCheckers) {
                ProgressManager.checkCanceled()
                checker.check(declaration, descriptor, context)
            }
            OperatorModifierChecker.check(declaration, descriptor, trace, languageVersionSettings)
//          PublishedApiUsageChecker.check(declaration, descriptor, trace)
//          OptionalExpectationChecker.check(declaration, descriptor, trace)
        }

        /**
         * 检查通用修饰符列表
         *
         * 执行修饰符列表的通用检查，包括运行声明检查器和核心修饰符检查
         *
         * @param modifierListOwner 修饰符列表拥有者（声明 PSI 元素）
         * @param descriptor 对应的声明描述符
         */
        private fun checkModifierListCommon(
            modifierListOwner: CjDeclaration,
            descriptor: DeclarationDescriptor
        ) {
//            AnnotationUseSiteTargetChecker.check(modifierListOwner, descriptor, trace, languageVersionSettings)
            runDeclarationCheckers(modifierListOwner, descriptor)
//            annotationChecker.check(modifierListOwner, trace, descriptor)
            ModifierCheckerCore.check(
                modifierListOwner,
                trace,
                descriptor,
                languageVersionSettings
            )
        }

        /**
         * 检查嵌套类是否允许
         *
         * 检查在特定上下文中是否允许嵌套类，例如枚举构造器中的嵌套类限制
         *
         * @param declaration 声明 PSI 元素
         * @param descriptor 对应的声明描述符
         */
        private fun checkNestedClassAllowed(
            declaration: CjDeclaration,
            descriptor: DeclarationDescriptor
        ) {
            if (declaration !is CjTypeStatement) return
            declaration
            if (descriptor !is ClassDescriptor) return
            val classDescriptor: ClassDescriptor =
                descriptor
            val containingDeclaration: DeclarationDescriptor =
                descriptor.containingDeclaration as? ClassDescriptor
                    ?: return
            containingDeclaration as ClassDescriptor

            val kind: DetailedClassKind =
                DetailedClassKind.getClassKind(classDescriptor)

//        if (kind == DetailedClassKind.ANONYMOUS_OBJECT || kind == DetailedClassKind.ENUM_CONSTRUCTOR) return

            // Local enums / objects / companion objects are handled in different checks
            if ((kind == DetailedClassKind.ENUM || kind == DetailedClassKind.STRUCT) &&
                DescriptorUtils.isLocal(classDescriptor)
            ) {
                return
            }

            // Since 1.3, enum entries can contain inner classes only.
            // Companion objects are reported in ModifierCheckerCore.
//            if (DescriptorUtils.isEnumConstructor(containingClass)   ) {
//                val diagnostic: DiagnosticFactory1<CjTypeStatement, String> =
//                    if (languageVersionSettings.supportsFeature( LanguageFeature.NestedClassesInEnumEntryShouldBeInner)
//                    )  Errors.NESTED_CLASS_NOT_ALLOWED
//                    else  Errors.NESTED_CLASS_DEPRECATED
//                trace.report(diagnostic.on(typeStatemtnt, kind.withCapitalFirstLetter))
//                return
//            }

//            if (   (containingClass.isInner() || DescriptorUtils.isLocal(
//                    containingClass
//                ))
//            ) {
//                trace.report(
//                    Errors.NESTED_CLASS_NOT_ALLOWED.on(
//                        typeStatemtnt,
//                        kind.withCapitalFirstLetter
//                    )
//                )
//            }
        }

    }

    companion object {

//

//        fun resolveVisibilityFormPackageOrImport(
//            modifierListOwner: CjModifierListOwner,
//            defaultVisibility: DescriptorVisibility
//        ): DescriptorVisibility {
//
//        }

        /**
         * 从修饰符解析可见性
         *
         * 根据修饰符列表（public、private、protected、internal）确定声明的可见性
         * 对于主函数特殊处理，始终返回 PUBLIC 可见性
         *
         * @param modifierListOwner 修饰符列表拥有者
         * @param defaultVisibility 默认可见性（当没有显式可见性修饰符时使用）
         * @return 解析得到的可见性
         */
        fun resolveVisibilityFromModifiers(
            modifierListOwner: CjModifierListOwner,
            defaultVisibility: DescriptorVisibility
        ): DescriptorVisibility {
            if (modifierListOwner is CjMainFunction) return DescriptorVisibilities.PUBLIC
            return resolveVisibilityFromModifiers(
                modifierListOwner.modifierList,
                defaultVisibility
            )
        }

        /**
         * 从修饰符解析成员的修饰性（modality）
         *
         * 解析类成员（函数、属性等）的修饰性，不允许 sealed 修饰符
         *
         * @param modifierListOwner 修饰符列表拥有者
         * @param defaultModality 默认修饰性
         * @param bindingContext 绑定上下文
         * @param containingDescriptor 包含该成员的描述符（通常是类描述符）
         * @return 解析得到的修饰性
         */
        fun resolveMemberModalityFromModifiers(
            modifierListOwner: CjModifierListOwner?,
            defaultModality: Modality,
            bindingContext: BindingContext,
            containingDescriptor: DeclarationDescriptor?
        ): Modality {
            return resolveModalityFromModifiers(
                modifierListOwner, defaultModality,
                bindingContext, containingDescriptor,  /* allowSealed = */false
            )
        }

        /**
         * 从修饰符解析修饰性（内部实现）
         *
         * 解析声明的修饰性（open、abstract、sealed、final）
         * 特殊逻辑：
         * - 抽象类中没有函数体的方法/属性自动为 abstract
         * - 处理 open、abstract、sealed 等修饰符的优先级
         *
         * @param containingDescriptor 包含该声明的描述符
         * @param modifierListOwner 修饰符列表拥有者
         * @param defaultModality 默认修饰性
         * @param allowSealed 是否允许 sealed 修饰符
         * @return 解析得到的修饰性
         */
        private fun resolveModalityFromModifiers(
            containingDescriptor: DeclarationDescriptor?,
            modifierListOwner: CjModifierListOwner?,
            defaultModality: Modality,
            allowSealed: Boolean
        ): Modality {
            val modifierList = modifierListOwner?.modifierList


            if (modifierListOwner is CjNamedFunction || modifierListOwner is CjProperty) {
                if (containingDescriptor is MemberDescriptor && containingDescriptor is ClassDescriptor) {

                    if (containingDescriptor.modality == Modality.ABSTRACT && containingDescriptor.kind == ClassKind.CLASS) {
                        if (modifierListOwner is CjProperty && !modifierListOwner.hasBody()) {

                            return Modality.ABSTRACT

                        } else if (modifierListOwner is CjNamedFunction && !modifierListOwner.hasBody()) {
                            return Modality.ABSTRACT

                        }
                    }
                }


            }
            if (modifierList == null) {
                return defaultModality
            }
            val hasAbstractModifier = modifierList.hasModifier(CjTokens.ABSTRACT_KEYWORD)
            modifierList.hasModifier(CjTokens.OVERRIDE_KEYWORD)

            if (allowSealed && modifierList.hasModifier(CjTokens.SEALED_KEYWORD)) {
                return Modality.SEALED
            }
            if (modifierList.hasModifier(CjTokens.OPEN_KEYWORD)) {
                if (containingDescriptor is ClassDescriptor) {
                    val classOrInterface: ClassDescriptor =
                        containingDescriptor
                    if (classOrInterface.kind == ClassKind.INTERFACE/* && classOrInterface.isExpect()*/) {
                        return Modality.OPEN
                    }
                }
                if (hasAbstractModifier || defaultModality == Modality.ABSTRACT) {
                    return Modality.ABSTRACT
                }
                return Modality.OPEN
            }
            if (hasAbstractModifier) {
                return Modality.ABSTRACT
            }
//            val hasFinalModifier = modifierList.hasModifier(CjTokens.FINAL_KEYWORD)
//            if (hasOverrideModifier /*&& !hasFinalModifier*/ && defaultModality != Modality.ABSTRACT) {
//                return Modality.OPEN
//            }
//            if (hasFinalModifier) {
//                return Modality.FINAL
//            }
            return defaultModality

        }

        /**
         * 从修饰符解析修饰性（公共接口）
         *
         * 解析声明的修饰性，支持扩展点修改修饰性
         *
         * @param modifierListOwner 修饰符列表拥有者
         * @param defaultModality 默认修饰性
         * @param bindingContext 绑定上下文
         * @param containingDescriptor 包含该声明的描述符
         * @param allowSealed 是否允许 sealed 修饰符
         * @return 解析得到的修饰性
         */
        fun resolveModalityFromModifiers(
            modifierListOwner: CjModifierListOwner?,
            defaultModality: Modality,
            bindingContext: BindingContext,
            containingDescriptor: DeclarationDescriptor?,
            allowSealed: Boolean
        ): Modality {

            var modality =
                resolveModalityFromModifiers(
                    containingDescriptor,
                    modifierListOwner,
                    defaultModality,
                    allowSealed
                )

            if (modifierListOwner != null) {
                val extensions: Collection<DeclarationAttributeAltererExtension> =
                    DeclarationAttributeAltererExtension.EP_NAME.extensionList

                val descriptor =
                    bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, modifierListOwner]
                for (extension in extensions) {
                    val newModality = extension.refineDeclarationModality(
                        modifierListOwner, descriptor, containingDescriptor, modality, false
                    )

                    if (newModality != null) {
                        modality = newModality
                        break
                    }
                }
            }
//
            return modality
        }

        /**
         * 从修饰符列表解析可见性
         *
         * 根据修饰符列表中的可见性关键字确定可见性级别
         * 优先级：sealed > private > public > protected > internal
         *
         * @param modifierList 修饰符列表
         * @param defaultVisibility 默认可见性
         * @return 解析得到的可见性
         */
        fun resolveVisibilityFromModifiers(
            modifierList: CjModifierList?,
            defaultVisibility: DescriptorVisibility
        ): DescriptorVisibility {
            if (modifierList == null) return defaultVisibility
            if (modifierList.hasModifier(CjTokens.SEALED_KEYWORD)) return DescriptorVisibilities.PUBLIC

            if (modifierList.hasModifier(CjTokens.PRIVATE_KEYWORD)) return DescriptorVisibilities.PRIVATE
            if (modifierList.hasModifier(CjTokens.PUBLIC_KEYWORD)) return DescriptorVisibilities.PUBLIC
            if (modifierList.hasModifier(CjTokens.PROTECTED_KEYWORD)) return DescriptorVisibilities.PROTECTED
            if (modifierList.hasModifier(CjTokens.INTERNAL_KEYWORD)) return DescriptorVisibilities.INTERNAL

            return defaultVisibility
        }
    }

    /**
     * 详细类类型枚举
     *
     * 用于区分不同类型的类声明，以便在检查时应用不同的规则
     *
     * @param withCapitalFirstLetter 类型名称（首字母大写）
     */
    private enum class DetailedClassKind(val withCapitalFirstLetter: String) {

        /**
         * 枚举构造器
         */
        ENUM_ENTRY("Enum constructor"),

        /**
         * 接口
         */
        INTERFACE("Interface"),

        /**
         * 枚举类
         */
        ENUM("Enum"),

        /**
         * 普通类
         */
        CLASS("Class"),

        /**
         * 结构体
         */
        STRUCT("Struct")
        ;

        companion object {
            /**
             * 根据类描述符获取详细类类型
             *
             * @param descriptor 类描述符
             * @return 对应的详细类类型
             */
            fun getClassKind(descriptor: ClassDescriptor): DetailedClassKind {
                if (DescriptorUtils.isEnumConstructor(descriptor)) return ENUM_ENTRY


                if (DescriptorUtils.isInterface(descriptor)) return INTERFACE

                return CLASS
            }
        }
    }

}
