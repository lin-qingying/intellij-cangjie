/*
 * Copyright 2026 LinQingYing. and contributors.
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

import com.google.common.collect.Maps
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.SmartHashSet
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.CallableMemberDescriptor.Kind.DELEGATION
import org.cangnova.cangjie.descriptors.CallableMemberDescriptor.Kind.FAKE_OVERRIDE
import org.cangnova.cangjie.descriptors.DescriptorVisibilityUtils.useSpecialRulesForPrivateSealedConstructors
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.diagnostics.DiagnosticFactory2
import org.cangnova.cangjie.diagnostics.DiagnosticFactoryWithPsiElement
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.infos.warnings.*
import org.cangnova.cangjie.diagnostics.rendering.DeclarationWithDiagnosticComponents
import org.cangnova.cangjie.diagnostics.rendering.PlatformSpecificDiagnosticComponents
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.lexer.CjToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.DescriptorUtils.classCanHaveAbstractFakeOverride
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.util.isOrOverridesSynthesized
import org.cangnova.cangjie.resolve.extend.ExtendVisibilityChecker
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.checker.DefaultCangJieTypeChecker
import java.util.*

//覆盖检查分析
class OverrideResolver(
    private val trace: BindingTrace,
    private val overridesBackwardCompatibilityHelper: OverridesBackwardCompatibilityHelper,
    private val languageVersionSettings: LanguageVersionSettings,
    private val cangjieTypeRefiner: CangJieTypeRefiner,
    private val platformSpecificDiagnosticComponents: PlatformSpecificDiagnosticComponents
) {

    fun check(c: TopDownAnalysisContext) {
        checkVisibility(c)
        checkOverrides(c)
        checkParameterOverridesForAllClasses(c)
    }


    private fun checkOverrides(c: TopDownAnalysisContext) {
        var index = 0
        for ((key, value) in c.declaredClasses) {
            index++
            checkOverridesInAClass(value, key)
        }

        // 检查扩展中的重写
        for ((extend, extendDescriptor) in c.extends) {
            checkOverridesInAnExtend(extendDescriptor, extend)
        }
    }

    /**
     * 检查类中的方法重写问题。
     *
     * 此函数用于检测类中方法重写的内部一致性问题，确保类的成员正确地重写了或实现了超类型的方法。它有助于维护类层次结构的一致性和正确性。
     *
     * @param classDescriptor 类的描述符，包含类的解析范围信息。
     * @param cclass 类型语句对象，表示当前类。
     */
    private fun checkOverridesInAClass(classDescriptor:  DescriptorWithResolutionScopes, cclass: CjTypeStatement) {
        // 检查类中声明的可调用成员的重写一致性
        for (member in classDescriptor.declaredCallableMembers) {
            checkOverrideForMember(member)
        }

        // 收集继承成员的错误信息
        val inheritedMemberErrors = CollectErrorInformationForInheritedMembersStrategy(cclass, classDescriptor)

        // 检查继承和委托签名
        checkInheritedAndDelegatedSignatures(
            classDescriptor, inheritedMemberErrors, inheritedMemberErrors, cangjieTypeRefiner
        )
        // 报告收集到的错误信息
        inheritedMemberErrors.doReportErrors()
    }

    /**
     * 检查扩展中的方法重写问题。
     *
     * 此函数用于检测扩展中方法重写的问题，确保扩展正确地实现了接口成员。
     *
     * @param extendDescriptor 扩展的描述符
     * @param extend 扩展的 PSI 元素
     */
    private fun checkOverridesInAnExtend(extendDescriptor: ExtendDescriptor, extend: CjExtend) {
        // 检查扩展中声明的可调用成员的重写一致性
        for (member in extendDescriptor.declaredCallableMembers) {
            checkOverrideForMember(member)
        }

        // 检查 extend 成员之间的 shadow 冲突
        checkExtendMemberShadowing(extendDescriptor, extend)

        // 收集继承成员的错误信息
        val inheritedMemberErrors = CollectErrorInformationForExtendMembersStrategy(extend, extendDescriptor)

        // 检查继承签名（抽象成员未实现等）
        checkInheritedSignaturesForExtend(extendDescriptor, inheritedMemberErrors, cangjieTypeRefiner)

        // 报告收集到的错误信息
        inheritedMemberErrors.doReportErrors()
    }

    /**
     * 检查 extend 成员是否遮蔽了其他 extend 的成员
     *
     * 当同一个类型有多个 extend 声明，且它们定义了相同签名的成员时，
     * 会产生 shadow 错误。这与编译器的 StructInheritanceChecker::CheckExtendMemberValid 实现一致。
     *
     * 只有当前 extend 可见的其他 extend 才会参与遮蔽检查，避免误报。
     *
     * @param extendDescriptor 当前扩展的描述符
     * @param extend 扩展的 PSI 元素
     */
    private fun checkExtendMemberShadowing(extendDescriptor: ExtendDescriptor, extend: CjExtend) {
        // 获取被扩展类型的类型构造器
        val extendedTypeConstructor = extendDescriptor.extendType.constructor

        // 获取 ExtendManager
        val module = DescriptorUtils.getContainingModuleOrNull(extendDescriptor) ?: return
        val extendManager = module.projectDescriptor.extendManager

        // 获取针对该类型的所有 extend 定义
        val allExtensions = extendManager.getExtensionsForType(extendedTypeConstructor)

        // 获取当前 extend 所在文件的词法作用域
        val currentScope = run {
            val file = extend.containingCjFile
            trace.get(BindingContext.LEXICAL_SCOPE, file)
        } ?: return

        // 收集其他 extend 的所有成员（排除当前 extend，且只收集可见的 extend）
        val otherExtendMembers = mutableMapOf<String, MutableList<Pair<CallableMemberDescriptor, ExtendDescriptor>>>()

        for (extensionDef in allExtensions) {
            val otherExtend = extensionDef.descriptor as? ExtendDescriptor ?: continue

            // 跳过当前 extend
            if (otherExtend.extendId == extendDescriptor.extendId) continue

            // 检查其他 extend 在当前作用域中是否可见
            if (!ExtendVisibilityChecker.isExtendAccessible(otherExtend, currentScope)) {
                continue
            }

            // 收集该 extend 的所有声明成员
            for (member in otherExtend.declaredCallableMembers) {
                val name = member.name.asString()
                otherExtendMembers.getOrPut(name) { mutableListOf() }.add(member to otherExtend)
            }
        }

        // 如果没有其他可见的 extend 成员，无需检查
        if (otherExtendMembers.isEmpty()) return

        // 检查当前 extend 的每个成员是否与其他 extend 的成员冲突
        for (member in extendDescriptor.declaredCallableMembers) {
            val name = member.name.asString()
            val conflictingMembers = otherExtendMembers[name] ?: continue

            // 检查是否有签名匹配的成员
            for ((otherMember, otherExtend) in conflictingMembers) {
                if (isSignatureConflicting(member, otherMember)) {
                    // 找到冲突，报告 shadow 错误
                    val declaration = DescriptorToSourceUtils.descriptorToDeclaration(member) as? CjDeclaration
                    if (declaration != null) {
                        // 获取被扩展类型的描述符
                        val extendedTypeDescriptor = extendedTypeConstructor.declarationDescriptor
                        if (extendedTypeDescriptor != null) {
                            trace.report(
                                EXTEND_MEMBER_CANNOT_SHADOW.on(
                                    declaration,
                                    name,
                                    extendedTypeDescriptor
                                )
                            )
                        }
                    }
                    // 每个成员只报告一次 shadow 错误
                    break
                }
            }
        }
    }
    /**
     * 检查两个成员的签名是否冲突
     *
     * @param member1 第一个成员
     * @param member2 第二个成员
     * @return 如果签名冲突返回 true
     */
    private fun isSignatureConflicting(
        member1: CallableMemberDescriptor,
        member2: CallableMemberDescriptor
    ): Boolean {
        // 名称必须相同（调用前已检查）

        // 检查类型：函数 vs 函数，属性 vs 属性
        if (member1 is FunctionDescriptor && member2 is FunctionDescriptor) {
            // 检查参数列表是否匹配
            val params1 = member1.valueParameters
            val params2 = member2.valueParameters
            if (params1.size != params2.size) return false

            // 检查每个参数的类型
            for (i in params1.indices) {
                val type1 = params1[i].type
                val type2 = params2[i].type
                // 使用简单的类型构造器比较
                if (type1.constructor != type2.constructor) return false
            }
            return true
        } else if (member1 is PropertyDescriptor && member2 is PropertyDescriptor) {
            // 属性只需要名称相同就冲突
            return true
        }

        return false
    }

    /**
     * 检查扩展的继承签名
     *
     * 遍历扩展成员作用域中的所有成员，检查是否有抽象成员未被实现。
     *
     * @param extendDescriptor 扩展描述符
     * @param reportStrategy 错误报告策略
     * @param cangjieTypeRefiner 类型精化器
     */
    private fun checkInheritedSignaturesForExtend(
        extendDescriptor: ExtendDescriptor,
        reportStrategy: CheckInheritedSignaturesReportStrategy,
        cangjieTypeRefiner: CangJieTypeRefiner
    ) {
        for (member in DescriptorUtils.getAllDescriptors(extendDescriptor.memberScope)) {
            if (member is CallableMemberDescriptor) {
                checkInheritedAndDelegatedSignatures(
                    member, reportStrategy, null, cangjieTypeRefiner
                )
            }
        }
    }


    private interface CheckInheritedSignaturesReportStrategy {
        fun abstractMemberNotImplemented(descriptor: CallableMemberDescriptor)
        fun abstractBaseClassMemberNotImplemented(descriptor: CallableMemberDescriptor)
        fun abstractMemberWithMoreSpecificType(
            abstractMember: CallableMemberDescriptor, concreteMember: CallableMemberDescriptor
        )

        fun multipleImplementationsMemberNotImplemented(descriptor: CallableMemberDescriptor)
        fun conflictingInterfaceMemberNotImplemented(descriptor: CallableMemberDescriptor)
        fun typeMismatchOnInheritance(descriptor1: CallableMemberDescriptor, descriptor2: CallableMemberDescriptor)
        fun abstractInvisibleMember(descriptor: CallableMemberDescriptor)
    }

    private class CollectMissingImplementationsStrategy : CheckInheritedSignaturesReportStrategy {
        val shouldImplement = LinkedHashSet<CallableMemberDescriptor>()

        override fun abstractMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            shouldImplement.add(descriptor)
        }

        override fun abstractBaseClassMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            // don't care
        }

        override fun multipleImplementationsMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            shouldImplement.add(descriptor)
        }

        override fun conflictingInterfaceMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            if (descriptor.modality === Modality.ABSTRACT) {
                shouldImplement.add(descriptor)
            }
        }

        override fun typeMismatchOnInheritance(
            descriptor1: CallableMemberDescriptor, descriptor2: CallableMemberDescriptor
        ) {
            // don't care
        }

        override fun abstractInvisibleMember(descriptor: CallableMemberDescriptor) {
            // don't care
        }

        override fun abstractMemberWithMoreSpecificType(
            abstractMember: CallableMemberDescriptor, concreteMember: CallableMemberDescriptor
        ) {
            shouldImplement.add(abstractMember)
        }
    }

    private inner class CollectWarningInformationForInheritedMembersStrategy(
        cclass: CjTypeStatement, classDescriptor: ClassAndEnumDescriptor
    ) : CollectErrorInformationForInheritedMembersStrategy(cclass, classDescriptor) {
        constructor(delegateStrategy: CollectErrorInformationForInheritedMembersStrategy) : this(
            delegateStrategy.cclass,
            delegateStrategy.classDescriptor
        )

        override fun doReportErrors() {
            val canHaveAbstractMembers = classCanHaveAbstractFakeOverride(classDescriptor)
            if (abstractInBaseClassNoImpl.isNotEmpty() && !canHaveAbstractMembers) {
                if (/*languageVersionSettings.supportsFeature(
                        LanguageFeature.AbstractClassMemberNotImplementedWithIntermediateAbstractClass
                    )*/true
                ) {
                    trace.report(
                        ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED.on(
                            cclass, cclass, abstractInBaseClassNoImpl.first()
                        )
                    )
                } else {
                    trace.report(
                        ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED_WARNING.on(
                            cclass, cclass, abstractInBaseClassNoImpl.first()
                        )
                    )
                }
            }
            if (conflictingInterfaceMembers.isNotEmpty()) {
                val interfaceMember = conflictingInterfaceMembers.first()
                if (/*languageVersionSettings.supportsFeature(
                        LanguageFeature.AbstractClassMemberNotImplementedWithIntermediateAbstractClass
                    )*/ true
                ) {
                    trace.report(MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED.on(cclass, cclass, interfaceMember))
                } else {
                    trace.report(MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED_WARNING.on(cclass, cclass, interfaceMember))
                }
            }
        }
    }

    private open inner class CollectErrorInformationForInheritedMembersStrategy(
        val cclass: CjTypeStatement, val classDescriptor: ClassAndEnumDescriptor
    ) : CheckInheritedSignaturesReportStrategy, CheckOverrideReportStrategy {

        private val abstractNoImpl = linkedSetOf<CallableMemberDescriptor>()
        protected val abstractInBaseClassNoImpl = linkedSetOf<CallableMemberDescriptor>()
        private val abstractInvisibleSuper = linkedSetOf<CallableMemberDescriptor>()
        private val multipleImplementations = linkedSetOf<CallableMemberDescriptor>()
        protected val conflictingInterfaceMembers = linkedSetOf<CallableMemberDescriptor>()
        private val conflictingReturnTypes = linkedSetOf<CallableMemberDescriptor>()

        private val onceErrorsReported = SmartHashSet<DiagnosticFactoryWithPsiElement<*, *>>()

        fun toDeprecationStrategy() = CollectWarningInformationForInheritedMembersStrategy(this)

        override fun abstractMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            abstractNoImpl.add(descriptor)
        }

        override fun abstractBaseClassMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            abstractInBaseClassNoImpl.add(descriptor)
        }

        override fun multipleImplementationsMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            multipleImplementations.add(descriptor)
        }

        override fun conflictingInterfaceMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            conflictingInterfaceMembers.add(descriptor)
        }

        override fun typeMismatchOnInheritance(
            descriptor1: CallableMemberDescriptor, descriptor2: CallableMemberDescriptor
        ) {
            conflictingReturnTypes.add(descriptor1)
            conflictingReturnTypes.add(descriptor2)

            if (descriptor1 is PropertyDescriptor && descriptor2 is PropertyDescriptor) {
                if (descriptor1.isVar || descriptor2.isVar) {
                    reportInheritanceConflictIfRequired(VAR_TYPE_MISMATCH_ON_INHERITANCE, descriptor1, descriptor2)
                } else {
                    reportInheritanceConflictIfRequired(PROPERTY_TYPE_MISMATCH_ON_INHERITANCE, descriptor1, descriptor2)
                }
            } else {
                reportInheritanceConflictIfRequired(RETURN_TYPE_MISMATCH_ON_INHERITANCE, descriptor1, descriptor2)
            }
        }

        override fun abstractInvisibleMember(descriptor: CallableMemberDescriptor) {
            abstractInvisibleSuper.add(descriptor)
        }

        override fun abstractMemberWithMoreSpecificType(
            abstractMember: CallableMemberDescriptor, concreteMember: CallableMemberDescriptor
        ) {
            typeMismatchOnInheritance(abstractMember, concreteMember)
        }

        private fun reportInheritanceConflictIfRequired(
            diagnosticFactory: DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor>,
            descriptor1: CallableMemberDescriptor,
            descriptor2: CallableMemberDescriptor
        ) {
            if (!onceErrorsReported.contains(diagnosticFactory)) {
                onceErrorsReported.add(diagnosticFactory)
                trace.report(diagnosticFactory.on(cclass, descriptor1, descriptor2))
            }
        }

        override fun overridingFinalMember(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor) {
            reportDelegationProblemIfRequired(OVERRIDING_FINAL_MEMBER_BY_DELEGATION, null, overriding, overridden)
        }

        override fun returnTypeMismatchOnOverride(
            overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor
        ) {
            val (diagnosticFactory, relevantDiagnosticFromInheritance) = if (overridden is PropertyDescriptor) PROPERTY_TYPE_MISMATCH_BY_DELEGATION to PROPERTY_TYPE_MISMATCH_ON_INHERITANCE
            else RETURN_TYPE_MISMATCH_BY_DELEGATION to RETURN_TYPE_MISMATCH_ON_INHERITANCE

            reportDelegationProblemIfRequired(
                diagnosticFactory, relevantDiagnosticFromInheritance, overriding, overridden
            )
        }

        override fun letOverriddenByVar(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor) {
            reportDelegationProblemIfRequired(VAR_OVERRIDDEN_BY_LET_BY_DELEGATION, null, overriding, overridden)
        }


        override fun varOverriddenByLet(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor) {
            reportDelegationProblemIfRequired(VAR_OVERRIDDEN_BY_LET_BY_DELEGATION, null, overriding, overridden)
        }

        private fun reportDelegationProblemIfRequired(
            diagnosticFactory: DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor>,
            relevantDiagnosticFromInheritance: DiagnosticFactoryWithPsiElement<*, *>?,
            delegate: CallableMemberDescriptor,
            overridden: CallableMemberDescriptor
        ) {
//            assert(delegate.kind == DELEGATION) { "Delegate expected, got " + delegate + " of kind " + delegate.kind }

            if (!onceErrorsReported.contains(diagnosticFactory) && (relevantDiagnosticFromInheritance == null || !onceErrorsReported.contains(
                    relevantDiagnosticFromInheritance
                ))
            ) {
                onceErrorsReported.add(diagnosticFactory)
                trace.report(diagnosticFactory.on(cclass, delegate, overridden))
            }
        }

        open fun doReportErrors() {
            val canHaveAbstractMembers = classCanHaveAbstractFakeOverride(classDescriptor)
            if (abstractInBaseClassNoImpl.isNotEmpty() && !canHaveAbstractMembers) {
                trace.report(
                    ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED.on(
                        cclass, cclass, abstractInBaseClassNoImpl.first()
                    )
                )
            } else if (abstractNoImpl.isNotEmpty() && !canHaveAbstractMembers) {
                trace.report(ABSTRACT_MEMBER_NOT_IMPLEMENTED.on(cclass, cclass, abstractNoImpl))
            }

            if (abstractInvisibleSuper.isNotEmpty() && !canHaveAbstractMembers) {
//                trace.report(
//                    INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER.on(
//                        languageVersionSettings, cclass, classDescriptor, abstractInvisibleSuper
//                    )
//                )
            }

            conflictingInterfaceMembers.removeAll(conflictingReturnTypes)
            multipleImplementations.removeAll(conflictingReturnTypes)
            if (conflictingInterfaceMembers.isNotEmpty()) {
                trace.report(
                    MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED.on(
                        cclass, cclass, conflictingInterfaceMembers.first()
                    )
                )
            } else if (multipleImplementations.isNotEmpty()) {
                trace.report(MANY_IMPL_MEMBER_NOT_IMPLEMENTED.on(cclass, cclass, multipleImplementations.first()))
            }
        }
    }

    /**
     * 扩展成员错误信息收集策略
     *
     * 与 CollectErrorInformationForInheritedMembersStrategy 类似，但专门用于扩展。
     * 扩展不能是抽象的，所以必须实现所有接口成员。
     */
    private inner class CollectErrorInformationForExtendMembersStrategy(
        val extend: CjExtend,
        val extendDescriptor: ExtendDescriptor
    ) : CheckInheritedSignaturesReportStrategy {

        private val abstractNoImpl = linkedSetOf<CallableMemberDescriptor>()
        private val multipleImplementations = linkedSetOf<CallableMemberDescriptor>()
        private val conflictingInterfaceMembers = linkedSetOf<CallableMemberDescriptor>()
        private val conflictingReturnTypes = linkedSetOf<CallableMemberDescriptor>()

        private val onceErrorsReported = SmartHashSet<DiagnosticFactoryWithPsiElement<*, *>>()

        override fun abstractMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            abstractNoImpl.add(descriptor)
        }

        override fun abstractBaseClassMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            // 扩展不继承自抽象类，所以此情况不适用
        }

        override fun multipleImplementationsMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            multipleImplementations.add(descriptor)
        }

        override fun conflictingInterfaceMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            conflictingInterfaceMembers.add(descriptor)
        }

        override fun typeMismatchOnInheritance(
            descriptor1: CallableMemberDescriptor, descriptor2: CallableMemberDescriptor
        ) {
            conflictingReturnTypes.add(descriptor1)
            conflictingReturnTypes.add(descriptor2)

            if (descriptor1 is PropertyDescriptor && descriptor2 is PropertyDescriptor) {
                if (descriptor1.isVar || descriptor2.isVar) {
                    reportInheritanceConflictIfRequired(VAR_TYPE_MISMATCH_ON_INHERITANCE, descriptor1, descriptor2)
                } else {
                    reportInheritanceConflictIfRequired(PROPERTY_TYPE_MISMATCH_ON_INHERITANCE, descriptor1, descriptor2)
                }
            } else {
                reportInheritanceConflictIfRequired(RETURN_TYPE_MISMATCH_ON_INHERITANCE, descriptor1, descriptor2)
            }
        }

        override fun abstractInvisibleMember(descriptor: CallableMemberDescriptor) {
            // 扩展实现的接口成员通常都是公开的，此检查可跳过
        }

        override fun abstractMemberWithMoreSpecificType(
            abstractMember: CallableMemberDescriptor, concreteMember: CallableMemberDescriptor
        ) {
            typeMismatchOnInheritance(abstractMember, concreteMember)
        }

        private fun reportInheritanceConflictIfRequired(
            diagnosticFactory: DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor>,
            descriptor1: CallableMemberDescriptor,
            descriptor2: CallableMemberDescriptor
        ) {
            if (!onceErrorsReported.contains(diagnosticFactory)) {
                onceErrorsReported.add(diagnosticFactory)
                trace.report(diagnosticFactory.on(extend, descriptor1, descriptor2))
            }
        }

        fun doReportErrors() {
            // 扩展不能是抽象的，必须实现所有抽象成员
            if (abstractNoImpl.isNotEmpty()) {
                trace.report(ABSTRACT_MEMBER_NOT_IMPLEMENTED.on(extend, extend, abstractNoImpl))
            }

            conflictingInterfaceMembers.removeAll(conflictingReturnTypes)
            multipleImplementations.removeAll(conflictingReturnTypes)
            if (conflictingInterfaceMembers.isNotEmpty()) {
                trace.report(
                    MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED.on(
                        extend, extend, conflictingInterfaceMembers.first()
                    )
                )
            } else if (multipleImplementations.isNotEmpty()) {
                trace.report(MANY_IMPL_MEMBER_NOT_IMPLEMENTED.on(extend, extend, multipleImplementations.first()))
            }
        }
    }

    private interface CheckOverrideReportStrategy {
        fun overridingFinalMember(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor)
        fun returnTypeMismatchOnOverride(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor)
        fun varOverriddenByLet(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor)


        fun letOverriddenByVar(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor)


//        fun redefByNotStatic(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor)
//        fun overriddenByStatic(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor)

    }

    private interface CheckOverrideReportForDeclaredMemberStrategy : CheckOverrideReportStrategy {
        fun nothingToOverride(overriding: CallableMemberDescriptor, overrideToken: CjToken = CjTokens.OVERRIDE_KEYWORD)
        fun cannotOverrideInvisibleMember(
            overriding: CallableMemberDescriptor, invisibleOverridden: CallableMemberDescriptor
        )
    }

    private fun CjModifierList?.hasOverride(isStatic: Boolean = false): Pair<CjToken, Boolean> {
        this ?: return Pair(CjTokens.OPEN_KEYWORD, false)
        return if (hasModifier(CjTokens.OVERRIDE_KEYWORD)) {
            Pair(CjTokens.OVERRIDE_KEYWORD, true)
        } else if (hasModifier(CjTokens.REDEF_KEYWORD)) {
            Pair(CjTokens.REDEF_KEYWORD, true)
        } else {
            //     随便返回一个
            Pair(CjTokens.OPEN_KEYWORD, false)
        }
//        return if (isStatic) {
//            hasModifier(CjTokens.REDEF_KEYWORD)
//        } else {
//            hasModifier(CjTokens.OVERRIDE_KEYWORD)
//        }
    }

    /**
     * 检查成员是否正确地重写了基类的成员
     *
     * 此函数旨在确保当前声明的成员如果重写了基类的成员，则必须正确地使用`override`关键字
     * 它还会检查重写成员的类型是否匹配，并处理其他重写相关的错误情况
     *
     * @param declared 当前声明的成员描述符
     */
    private fun checkOverrideForMember(declared: CallableMemberDescriptor) {
        // 忽略合成的成员，因为它们不由用户直接编写，不适用重写检查
//    if (declared.kind == CallableMemberDescriptor.Kind.SYNTHESIZED) {
//        if (DataClassDescriptorResolver.isComponentLike(declared.name)) {
//            checkOverrideForComponentFunction(declared)
//        } else if (declared.name == DataClassDescriptorResolver.COPY_METHOD_NAME) {
//            checkOverrideForCopyFunction(declared)
//        }
//        return
//    }

        // 只处理声明类型的成员，其他类型不进行重写检查
        if (declared.kind != CallableMemberDescriptor.Kind.DECLARATION) {
            return
        }

        // 将描述符转换为具体的声明节点，以便后续检查
        val member = DescriptorToSourceUtils.descriptorToDeclaration(declared) as CjNamedDeclaration?
            ?: throw IllegalStateException("declared descriptor is not resolved to declaration: $declared")

        // 获取成员的修饰符列表，以检查是否包含`override`关键字
        val modifierList = member.modifierList
        val (overrideToken, hasOverride) = modifierList.hasOverride(declared.isStatic)
        val hasOverrideNode = modifierList != null && hasOverride
        val overriddenDescriptors = declared.overriddenDescriptors


        // 定义一个策略对象，用于报告各种重写错误
        val reportError = object : CheckOverrideReportForDeclaredMemberStrategy {
            private var finalOverriddenError = false
            private var typeMismatchError = false
            private var kindMismatchError = false

            // 报告重写final成员的错误
            override fun overridingFinalMember(
                overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor
            ) {
                if (!finalOverriddenError) {
                    finalOverriddenError = true
                    trace.report(
                        OVERRIDING_FINAL_MEMBER.on(
                            member, overridden, overridden.containingDeclaration
                        )
                    )
                }
            }

            // 报告重写成员返回类型不匹配的错误
            override fun returnTypeMismatchOnOverride(
                overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor
            ) {
                if (!typeMismatchError) {
                    typeMismatchError = true

                    when {
                        overridden is PropertyDescriptor && overridden.isVar -> trace.report(
                            VAR_TYPE_MISMATCH_ON_OVERRIDE.on(member, declared, overridden)
                        )

                        overridden is PropertyDescriptor && !overridden.isVar -> trace.report(
                            PROPERTY_TYPE_MISMATCH_ON_OVERRIDE.on(member, declared, overridden)
                        )

                        else -> trace.report(
                            RETURN_TYPE_MISMATCH_ON_OVERRIDE.on(
                                member, declared, DeclarationWithDiagnosticComponents(
                                    overridden, platformSpecificDiagnosticComponents
                                )
                            )
                        )
                    }
                }
            }

            // 报告用let重写var成员的错误
            override fun letOverriddenByVar(
                overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor
            ) {
                if (!kindMismatchError) {
                    kindMismatchError = true
                    trace.report(
                        LET_OVERRIDDEN_BY_VAR.on(
                            member, declared as PropertyDescriptor, overridden as PropertyDescriptor
                        )
                    )
                }
            }

            // 报告用var重写let成员的错误
            override fun varOverriddenByLet(
                overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor
            ) {
                if (!kindMismatchError) {
                    kindMismatchError = true
                    trace.report(
                        VAR_OVERRIDDEN_BY_LET.on(
                            member, declared as PropertyDescriptor, overridden as PropertyDescriptor
                        )
                    )
                }
            }

            // 报告尝试重写不可见成员的错误
            override fun cannotOverrideInvisibleMember(
                overriding: CallableMemberDescriptor, invisibleOverridden: CallableMemberDescriptor
            ) {
                trace.report(CANNOT_OVERRIDE_INVISIBLE_MEMBER.on(member, declared, invisibleOverridden))
            }

            // 报告无成员可重写的情况
            override fun nothingToOverride(overriding: CallableMemberDescriptor, overrideToken: CjToken) {
                when (overrideToken) {
                    CjTokens.OVERRIDE_KEYWORD -> trace.report(NOTHING_TO_OVERRIDE.on(member, declared))

                    CjTokens.REDEF_KEYWORD -> trace.report(REDEF_NOTHING_TO_OVERRIDE.on(member, declared))
                }
            }
        }

        // 检查重写成员的正确性
        if (!overriddenDescriptors.isEmpty() && !overridesBackwardCompatibilityHelper.overrideCanBeOmitted(declared)) {
            // 如果成员没有使用`override`关键字，则报告错误
            if (!hasOverrideNode) {
                // 成员隐藏了超类型的成员但没有使用 override 关键字
                // 此警告已被移除，因为编译器不会强制要求 override 关键字
            } else {
                //                declared.modality = Modality.OPEN
            }

            // 进一步检查重写成员的其他问题
            checkOverridesForMemberMarkedOverride(
                declared, cangjieTypeRefiner, reportError, languageVersionSettings, overrideToken
            )
        } else if (hasOverrideNode) {
            // 如果成员错误地使用了`override`关键字，则进行检查
            checkOverridesForMemberMarkedOverride(
                declared, cangjieTypeRefiner, reportError, languageVersionSettings, overrideToken
            )
        }
    }


    private fun checkParameterOverridesForAllClasses(c: TopDownAnalysisContext) {
        for (classDescriptor in c.declaredClasses.values) {
            for (member in DescriptorUtils.getAllDescriptors(classDescriptor.defaultType.memberScope)) {
                if (member is CallableMemberDescriptor) {
                    checkOverridesForParameters(member)
                }
            }
        }
    }

    private fun checkOverridesForParameters(declared: CallableMemberDescriptor) {
        val isDeclaration = declared.kind == CallableMemberDescriptor.Kind.DECLARATION
        if (isDeclaration) {
            // No check if the function is not marked as 'override'
            val declaration = DescriptorToSourceUtils.descriptorToDeclaration(declared) as CjModifierListOwner?
            if (declaration != null && !declaration.hasModifier(CjTokens.OVERRIDE_KEYWORD)) {
                return
            }
        }

        // Let p1 be a parameter of the overriding function
        // Let p2 be a parameter of the function being overridden
        // Then
        //  a) p1 is not allowed to have a default value declared
        //  b) p1 must have the same name as p2
        for (parameterFromSubclass in declared.valueParameters) {
            var defaultsInSuper = 0
            for (parameterFromSuperclass in parameterFromSubclass.overriddenDescriptors) {
                if (parameterFromSuperclass.declaresDefaultValue) {
                    defaultsInSuper++
                }
            }
            val multipleDefaultsInSuper = defaultsInSuper > 1

            if (isDeclaration) {
                checkNameAndDefaultForDeclaredParameter(parameterFromSubclass, multipleDefaultsInSuper)
            } else {
                checkNameAndDefaultForFakeOverrideParameter(declared, parameterFromSubclass, multipleDefaultsInSuper)
            }
        }
    }

    private fun checkNameAndDefaultForDeclaredParameter(
        descriptor: ValueParameterDescriptor, multipleDefaultsInSuper: Boolean
    ) {
        val parameter = DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? CjParameter
            ?: error("Declaration not found for parameter: $descriptor")

        if (descriptor.declaresDefaultValue) {
            trace.report(DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE.on(parameter))
        }

        if (multipleDefaultsInSuper) {
            trace.report(MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES.on(parameter, descriptor))
        }

        for (parameterFromSuperclass in descriptor.overriddenDescriptors) {
            if (shouldReportParameterNameOverrideWarning(descriptor, parameterFromSuperclass)) {

                trace.report(
                    PARAMETER_NAME_CHANGED_ON_OVERRIDE.on(
                        parameter,
                        parameterFromSuperclass.containingDeclaration.containingDeclaration as ClassDescriptor,
                        parameterFromSuperclass
                    )
                )
            }
        }
    }

    private fun checkNameAndDefaultForFakeOverrideParameter(
        containingFunction: CallableMemberDescriptor,
        descriptor: ValueParameterDescriptor,
        multipleDefaultsInSuper: Boolean
    ) {
        val containingClass = containingFunction.containingDeclaration
        val classElement = DescriptorToSourceUtils.descriptorToDeclaration(containingClass) as CjTypeStatement?
            ?: error("Declaration not found for class: $containingClass")

        if (multipleDefaultsInSuper) {
            trace.report(
                MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_MATCH_NO_EXPLICIT_OVERRIDE.on(
                    classElement, descriptor
                )
            )
        }

        for (parameterFromSuperclass in descriptor.overriddenDescriptors) {
            if (shouldReportParameterNameOverrideWarning(descriptor, parameterFromSuperclass)) {
                trace.report(
                    DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES.on(
                        classElement, containingFunction.overriddenDescriptors, parameterFromSuperclass.index + 1
                    )
                )
            }
        }
    }

    private fun checkVisibility(c: TopDownAnalysisContext) {
        for ((key, value) in c.members) {
            if (value is CallableMemberDescriptor)
                checkVisibilityForMember(key, value)
//       TODO     属性检查
//            if (key is CjProperty && value is PropertyDescriptor) {
//                val setter = key.setter
//                val setterDescriptor = value.setter
//                if (setter != null && setterDescriptor != null) {
//                    checkVisibilityForMember(setter, setterDescriptor)
//                }
//            }
        }
    }

    private fun checkVisibilityForMember(declaration: CjDeclaration, memberDescriptor: CallableMemberDescriptor) {
        val visibility = memberDescriptor.visibility
        for (descriptor in memberDescriptor.overriddenDescriptors) {
            val compare = DescriptorVisibilities.compare(visibility, descriptor.visibility)
            if (compare == null) {
                trace.report(
                    CANNOT_CHANGE_ACCESS_PRIVILEGE.on(
                        declaration, descriptor.visibility, descriptor, descriptor.containingDeclaration
                    )
                )
                return
            } else if (compare < 0) {
                trace.report(
                    CANNOT_WEAKEN_ACCESS_PRIVILEGE.on(
                        declaration, descriptor.visibility, descriptor, descriptor.containingDeclaration
                    )
                )
                return
            }
        }
    }

    companion object {

        fun resolveUnknownVisibilities(
            descriptors: Collection<CallableMemberDescriptor>, trace: BindingTrace
        ) {
            for (descriptor in descriptors) {
                OverridingUtil.resolveUnknownVisibilityForMember(descriptor, createCannotInferVisibilityReporter(trace))
            }
        }

        private fun createCannotInferVisibilityReporter(trace: BindingTrace): Function1<CallableMemberDescriptor, Unit> {
            return fun(descriptor: CallableMemberDescriptor) {
                val reportOn: DeclarationDescriptor = when {
                    descriptor.kind == FAKE_OVERRIDE || descriptor.kind == DELEGATION -> DescriptorUtils.getContainingClass(
                        descriptor
                    ) ?: throw AssertionError("Class member expected: $descriptor")

//                    descriptor is PropertyAccessorDescriptor && descriptor.isDefault ->
//                        descriptor.correspondingProperty

                    else -> descriptor
                }

                val element = DescriptorToSourceUtils.descriptorToDeclaration(reportOn)
                if (element is CjDeclaration) {
                    trace.report(CANNOT_INFER_VISIBILITY.on(element, descriptor))
                }
                return
            }
        }

        /**
         * 获取类描述符中缺失的实现方法或属性
         *
         * 此函数的目的是检查给定类描述符中哪些成员方法或属性需要实现但尚未实现
         * 它通过分析类的继承结构和成员描述符来确定哪些成员在当前类中缺少实现
         *
         * @param classDescriptor 类描述符，表示需要检查的类的信息
         * @return 返回一个包含缺失实现的成员描述符的集合
         */
        fun getMissingImplementations(classDescriptor: ClassDescriptor): Set<CallableMemberDescriptor> {
            // 创建一个收集缺失实现策略的实例
            val collector = CollectMissingImplementationsStrategy()

            // 检查继承和委托的签名，使用默认的类型细化器
            // 注意：这里使用默认的类型细化器是可行的，原因如下：
            // 1. 我们将重写方法与适当的细化器绑定，[checkInheritedAndDelegatedSignatures] 会跳过所有适当绑定的重写方法，
            //    因此我们只会考虑未绑定的重写方法
            // 2. 使用默认的细化器代替适当的细化器只能增加类型不匹配的数量，而不会减少它
            // 将1和2结合起来意味着，在[getMissingImplementations]的情况下，使用默认的细化器可能会使已经未绑定的重写方法更加“未绑定”，但这并不是问题
            checkInheritedAndDelegatedSignatures(classDescriptor, collector, null, CangJieTypeRefiner.Default)

            // 返回收集到的应当实现的成员描述符集合
            return collector.shouldImplement
        }

        private fun checkInheritedAndDelegatedSignatures(
            classDescriptor: ClassAndEnumDescriptor,
            inheritedReportStrategy: CheckInheritedSignaturesReportStrategy,
            overrideReportStrategyForDelegates: CheckOverrideReportStrategy?,
            cangjieTypeRefiner: CangJieTypeRefiner
        ) {
            for (member in DescriptorUtils.getAllDescriptors(classDescriptor.defaultType.memberScope)) {
                if (member is CallableMemberDescriptor) {
                    checkInheritedAndDelegatedSignatures(
                        member, inheritedReportStrategy, overrideReportStrategyForDelegates, cangjieTypeRefiner
                    )
                }
            }
        }

        private fun CallableMemberDescriptor.computeRelevantDirectlyOverridden(): Set<CallableMemberDescriptor> {
            val directOverridden = overriddenDescriptors

            // directOverridden may be empty if user tries to delegate implementation of abstract class instead of interface
            if (directOverridden.isEmpty()) return emptySet()

            // collects map from the directly overridden descriptor to the set of declarations:
            // -- if directly overridden is not fake, the set consists of one element: this directly overridden
            // -- if it's fake, overridden declarations (non-fake) of this descriptor are collected
            val overriddenDeclarationsByDirectParent = collectOverriddenDeclarations(directOverridden)

            // 明确使用 Iterable 重载以避免歧义
            val allOverriddenDeclarations = ContainerUtil.flatten(
                overriddenDeclarationsByDirectParent.values as Iterable<Collection<CallableMemberDescriptor>>
            )
//            val allFilteredOverriddenDeclarations = OverridingUtil.filterOutOverridden(
//                Sets.newLinkedHashSet(allOverriddenDeclarations)
//            )
            val allFilteredOverriddenDeclarations = emptySet<CallableMemberDescriptor>()

            return getRelevantDirectlyOverridden(
                overriddenDeclarationsByDirectParent, allFilteredOverriddenDeclarations
            )
        }

        private fun checkInheritedAndDelegatedSignatures(
            descriptor: CallableMemberDescriptor,
            reportingStrategy: CheckInheritedSignaturesReportStrategy,
            overrideReportStrategyForDelegates: CheckOverrideReportStrategy?,
            cangjieTypeRefiner: CangJieTypeRefiner
        ) {
            val kind = descriptor.kind
            if (kind != FAKE_OVERRIDE && kind != DELEGATION) return

            val relevantDirectlyOverridden = descriptor.computeRelevantDirectlyOverridden()
            if (relevantDirectlyOverridden.isEmpty()) return

            if (descriptor.visibility === DescriptorVisibilities.INVISIBLE_FAKE) {
                checkInvisibleFakeOverride(descriptor, relevantDirectlyOverridden, reportingStrategy)
                return
            }

            checkInheritedDescriptorsGroup(
                descriptor, relevantDirectlyOverridden, reportingStrategy, cangjieTypeRefiner
            )

            if (kind == DELEGATION && overrideReportStrategyForDelegates != null) {
                checkOverridesForMember(
                    descriptor, relevantDirectlyOverridden, overrideReportStrategyForDelegates, cangjieTypeRefiner
                )
            }

            if (kind != DELEGATION) {
                checkMissingOverridesByJava8Restrictions(relevantDirectlyOverridden, reportingStrategy)
            }

            val (concreteOverridden, abstractOverridden) = relevantDirectlyOverridden.filter {
                !isOrOverridesSynthesized(
                    it
                )
            }.partition { it.modality != Modality.ABSTRACT }

            when (concreteOverridden.size) {
                0 -> if (kind != DELEGATION) {
                    abstractOverridden.forEach {
                        reportingStrategy.abstractMemberNotImplemented(it)
                    }
                }

                1 -> if (kind != DELEGATION) {
                    val implementation = concreteOverridden.first()
                    collectAbstractMethodsWithMoreSpecificReturnType(
                        abstractOverridden, implementation, cangjieTypeRefiner
                    ).forEach {
                        reportingStrategy.abstractMemberWithMoreSpecificType(it, implementation)
                    }
                }

                else -> concreteOverridden.forEach {
                    reportingStrategy.multipleImplementationsMemberNotImplemented(it)
                }
            }
        }

        private fun checkInvisibleFakeOverride(
            descriptor: CallableMemberDescriptor,
            overriddenDescriptors: Collection<CallableMemberDescriptor>,
            reportingStrategy: CheckInheritedSignaturesReportStrategy
        ) {
            // the checks below are only relevant for non-abstract classes or objects
            if ((descriptor.containingDeclaration as? ClassDescriptor)?.modality === Modality.ABSTRACT) return

            val abstractOverrides = overriddenDescriptors.filter { it.modality === Modality.ABSTRACT }

            if (abstractOverrides.size != overriddenDescriptors.size) return // has non-abstract override

            for (override in abstractOverrides) {
                reportingStrategy.abstractInvisibleMember(override)
            }
        }

        private fun checkMissingOverridesByJava8Restrictions(
            relevantDirectlyOverridden: Set<CallableMemberDescriptor>,
            reportingStrategy: CheckInheritedSignaturesReportStrategy,
            onlyBaseClassMembers: Boolean = false,
            overriddenInterfaceMembers: MutableList<CallableMemberDescriptor> = SmartList()
        ) {
            // Java 8:
            // -- class should implement an abstract member of a super-class,
            //    even if relevant default implementation is provided in one of the super-interfaces;
            // -- inheriting multiple override equivalent methods from an interface is a conflict
            //    regardless of 'default' vs 'abstract'.

            var overridesClassMember = false
            var overridesNonAbstractInterfaceMember = false
            var overridesAbstractInBaseClass: CallableMemberDescriptor? = null
            var overridesNonAbstractInBaseClass: CallableMemberDescriptor? = null
            var fakeOverrideInBaseClass: CallableMemberDescriptor? = null
            for (overridden in relevantDirectlyOverridden) {
                val containingDeclaration = overridden.containingDeclaration as? ClassDescriptor ?: continue
                if (containingDeclaration.kind == ClassKind.CLASS) {
                    if (overridden.kind == FAKE_OVERRIDE /*&& !containingDeclaration.isExpect*/) {
                        // Fake override in a class in fact can mean an interface member
                        // We will process it at the end
                        // Note: with expect containing class, the situation is unclear, so we miss this case
                        // See extendExpectedClassWithAbstractMember.cj (BaseA, BaseAImpl, DerivedA1)
                        fakeOverrideInBaseClass = overridden
                    }
                    overridesClassMember = true
                    if (overridden.modality === Modality.ABSTRACT) {
                        overridesAbstractInBaseClass = overridden
                    } else {
                        overridesNonAbstractInBaseClass = overridden
                    }
                } else if (containingDeclaration.kind == ClassKind.INTERFACE) {
                    overriddenInterfaceMembers.add(overridden)
                    if (overridden.modality !== Modality.ABSTRACT) {
                        overridesNonAbstractInterfaceMember = true
                    }
                }
            }

            if (overridesAbstractInBaseClass != null && overridesNonAbstractInBaseClass == null) {
                reportingStrategy.abstractBaseClassMemberNotImplemented(overridesAbstractInBaseClass)
            } else if (!onlyBaseClassMembers && !overridesClassMember && overridesNonAbstractInterfaceMember && overriddenInterfaceMembers.size > 1) {
                for (member in overriddenInterfaceMembers) {
                    reportingStrategy.conflictingInterfaceMemberNotImplemented(member)
                }
            } else if (fakeOverrideInBaseClass != null) {
                val newReportingStrategy =
                    if (reportingStrategy is CollectErrorInformationForInheritedMembersStrategy) {
                        reportingStrategy.toDeprecationStrategy()
                    } else reportingStrategy
                checkMissingOverridesByJava8Restrictions(
                    fakeOverrideInBaseClass.computeRelevantDirectlyOverridden(),
                    reportingStrategy = newReportingStrategy,
                    // Note: we don't report MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED_WARNING
                    // in case all interface members are derived from fakeOverrideInClass
                    // (in this case we already have warning or error on its container)
                    onlyBaseClassMembers = overriddenInterfaceMembers.isEmpty(),
                    overriddenInterfaceMembers
                )
                if (newReportingStrategy is CollectWarningInformationForInheritedMembersStrategy) {
                    newReportingStrategy.doReportErrors()
                }
            }
        }

        private fun collectAbstractMethodsWithMoreSpecificReturnType(
            abstractOverridden: List<CallableMemberDescriptor>,
            implementation: CallableMemberDescriptor,
            cangjieTypeRefiner: CangJieTypeRefiner
        ): List<CallableMemberDescriptor> = abstractOverridden.filter { abstractMember ->
            !isReturnTypeOkForOverride(
                abstractMember, implementation, cangjieTypeRefiner
            )
        }

        private fun getRelevantDirectlyOverridden(
            overriddenByParent: MutableMap<CallableMemberDescriptor, Set<CallableMemberDescriptor>>,
            allFilteredOverriddenDeclarations: Set<CallableMemberDescriptor>
        ): Set<CallableMemberDescriptor> {/* Let the following class hierarchy is declared:

        trait A { fun foo() = 1 }
        trait B : A
        trait C : A
        trait D : A { override fun foo() = 2 }
        trait E : B, C, D {}

        Traits B and C have fake descriptors for function foo.
        The map 'overriddenByParent' is:
        { 'foo in B' (fake) -> { 'foo in A' }, 'foo in C' (fake) -> { 'foo in A' }, 'foo in D' -> { 'foo in D'} }
        This is a map from directly overridden descriptors (functions 'foo' in B, C, D in this example) to the set of declarations (non-fake),
        that are overridden by this descriptor.

        The goal is to leave only relevant directly overridden descriptors to count implementations of our fake function on them.
        In the example above there is no error (trait E inherits only one implementation of 'foo' (from D), because this implementation is more precise).
        So only 'foo in D' is relevant.

        Directly overridden descriptor is not relevant if it doesn't add any more appropriate non-fake declarations of the concerned function.
        More precisely directly overridden descriptor is not relevant if:
        - it's declaration set is a subset of declaration set for other directly overridden descriptor
        ('foo in B' is not relevant because it's declaration set is a subset of 'foo in C' function's declaration set)
        - each member of it's declaration set is overridden by a member of other declaration set
        ('foo in C' is not relevant, because 'foo in A' is overridden by 'foo in D', so 'foo in A' is not appropriate non-fake declaration for 'foo')

        For the last condition allFilteredOverriddenDeclarations helps (for the example above it's { 'foo in A' } only): each declaration set
        is compared with allFilteredOverriddenDeclarations, if they have no intersection, this means declaration set has only functions that
        are overridden by some other function and corresponding directly overridden descriptor is not relevant.
        */

            val iterator = overriddenByParent.entries.iterator()
            while (iterator.hasNext()) {
                if (!isRelevant(iterator.next().value, overriddenByParent.values, allFilteredOverriddenDeclarations)) {
                    iterator.remove()
                }
            }
            return overriddenByParent.keys
        }

        private fun isRelevant(
            declarationSet: Set<CallableMemberDescriptor>,
            allDeclarationSets: Collection<Set<CallableMemberDescriptor>>,
            allFilteredOverriddenDeclarations: Set<CallableMemberDescriptor>
        ): Boolean {
            for (otherSet in allDeclarationSets) {
                if (otherSet === declarationSet) continue
                if (otherSet.containsAll(declarationSet)) return false
                if (Collections.disjoint(allFilteredOverriddenDeclarations, declarationSet)) return false
            }
            return true
        }

        private fun collectOverriddenDeclarations(
            directOverriddenDescriptors: Collection<CallableMemberDescriptor>
        ): MutableMap<CallableMemberDescriptor, Set<CallableMemberDescriptor>> {
            val overriddenDeclarationsByDirectParent =
                Maps.newLinkedHashMap<CallableMemberDescriptor, Set<CallableMemberDescriptor>>()
            for (descriptor in directOverriddenDescriptors) {
                val overriddenDeclarations = OverridingUtil.getOverriddenDeclarations(descriptor)
                val filteredOverrides = OverridingUtil.filterOutOverridden(overriddenDeclarations)
                overriddenDeclarationsByDirectParent[descriptor] = LinkedHashSet(filteredOverrides)
            }
            return overriddenDeclarationsByDirectParent
        }

        private fun checkInheritedDescriptorsGroup(
            descriptor: CallableMemberDescriptor,
            overriddenDescriptors: Collection<CallableMemberDescriptor>,
            reportingStrategy: CheckInheritedSignaturesReportStrategy,
            cangjieTypeRefiner: CangJieTypeRefiner
        ) {
            if (overriddenDescriptors.size <= 1) return

            for (overriddenDescriptor in overriddenDescriptors) {
                require(descriptor !is PropertyDescriptor || overriddenDescriptor is PropertyDescriptor) {
                    "$overriddenDescriptor is not a property"
                }

                if (!isReturnTypeOkForOverride(overriddenDescriptor, descriptor, cangjieTypeRefiner)) {
                    reportingStrategy.typeMismatchOnInheritance(descriptor, overriddenDescriptor)
                }
            }
        }

        private fun checkOverridesForMemberMarkedOverride(
            declared: CallableMemberDescriptor,
            cangjieTypeRefiner: CangJieTypeRefiner,
            reportError: CheckOverrideReportForDeclaredMemberStrategy,
            languageVersionSettings: LanguageVersionSettings,
            overrideToken: CjToken = CjTokens.OVERRIDE_KEYWORD
        ) {
            val overriddenDescriptors = declared.overriddenDescriptors

            checkOverridesForMember(declared, overriddenDescriptors, reportError, cangjieTypeRefiner, overrideToken)



            if (overriddenDescriptors.isEmpty()) {
                val containingDeclaration = declared.containingDeclaration

                when (containingDeclaration) {
                    is ClassAndEnumDescriptor -> {
                        // 类成员：检查是否有不可见的被覆盖成员
                        val invisibleOverriddenDescriptor = findInvisibleOverriddenDescriptor(
                            declared, containingDeclaration, cangjieTypeRefiner, languageVersionSettings
                        )
                        if (invisibleOverriddenDescriptor != null) {
                            reportError.cannotOverrideInvisibleMember(declared, invisibleOverriddenDescriptor)
                        } else {
                            reportError.nothingToOverride(declared, overrideToken)
                        }
                    }
                    is ExtendDescriptor -> {
                        // 扩展成员：接口成员通常是公开的，直接报告没有可覆盖的成员
                        reportError.nothingToOverride(declared, overrideToken)
                    }
                    else -> {
                        error("Overrides may only be resolved in a class or extend, but $declared comes from $containingDeclaration")
                    }
                }
            }
        }

        /**
         * 检查成员的覆盖是否合法
         * 此函数用于检查一个成员是否正确地覆盖了其超类中的成员，主要关注于覆盖成员的模态性、返回类型以及属性类型是否匹配
         *
         * @param memberDescriptor 被检查的成员描述符
         * @param overriddenDescriptors 被覆盖的成员描述符集合
         * @param reportError 报告覆盖错误的策略
         * @param cangjieTypeRefiner 用于精炼类型的工具
         * @param overrideToken 覆盖的类型令牌，默认为CjTokens.OVERRIDE_KEYWORD
         */
        private fun checkOverridesForMember(
            memberDescriptor: CallableMemberDescriptor,
            overriddenDescriptors: Collection<CallableMemberDescriptor>,
            reportError: CheckOverrideReportStrategy,
            cangjieTypeRefine: CangJieTypeRefiner,
            overrideToken: CjToken = CjTokens.OVERRIDE_KEYWORD

        ) {
            for (overridden in overriddenDescriptors) {

                // 如果被覆盖的成员是final的，则报告错误
                if (overridden.modality == Modality.FINAL) {
                    reportError.overridingFinalMember(memberDescriptor, overridden)
                }

                // 如果覆盖成员的返回类型不正确，则报告错误
                if (!isReturnTypeOkForOverride(overridden, memberDescriptor, cangjieTypeRefine)) {
                    require(memberDescriptor !is PropertyDescriptor || overridden is PropertyDescriptor) {
                        "$overridden is overridden by property $memberDescriptor"
                    }
                    reportError.returnTypeMismatchOnOverride(memberDescriptor, overridden)
                }
                // 如果被覆盖成员是let，而覆盖成员是var，则报告错误
                if (checkPropertyKind(overridden, false) && checkPropertyKind(memberDescriptor, true)) {
                    reportError.letOverriddenByVar(memberDescriptor, overridden)
                }
                // 如果被覆盖成员是var，而覆盖成员是let，则报告错误
                if (checkPropertyKind(overridden, true) && checkPropertyKind(memberDescriptor, false)) {
                    reportError.varOverriddenByLet(memberDescriptor, overridden)
                }
            }
        }

        private fun isReturnTypeOkForOverride(
            superDescriptor: CallableDescriptor,
            subDescriptor: CallableDescriptor,
            cangjieTypeRefiner: CangJieTypeRefiner,
        ): Boolean {
            val typeSubstitutor = prepareTypeSubstitutor(superDescriptor, subDescriptor) ?: return false

            val superReturnType = superDescriptor.returnType!!

            val subReturnType = subDescriptor.returnType!!

            val substitutedSuperReturnType = typeSubstitutor.substitute(superReturnType )!!

            val typeChecker = DefaultCangJieTypeChecker(cangjieTypeRefiner)
            return if (superDescriptor is PropertyDescriptor && superDescriptor.isVar) typeChecker.equalTypes(
                subReturnType,
                substitutedSuperReturnType
            )
            else typeChecker.isSubtypeOf(subReturnType, substitutedSuperReturnType)
        }

        private fun prepareTypeSubstitutor(
            superDescriptor: CallableDescriptor, subDescriptor: CallableDescriptor
        ): ComposableTypeSubstitutor? {
            val superTypeParameters = superDescriptor.typeParameters
            val subTypeParameters = subDescriptor.typeParameters
            if (subTypeParameters.size != superTypeParameters.size) return null

            val substitutorMap = superTypeParameters.zip(subTypeParameters).associate { (superParam, subParam) ->
                superParam.typeConstructor to subParam.defaultType.unwrap()
            }

            return ComposableTypeSubstitutor.create(substitutorMap)

        }

//        private fun findDataModifierForDataClass(dataClass: DeclarationDescriptor): PsiElement {
//            val classDeclaration = DescriptorToSourceUtils.getSourceFromDescriptor(dataClass) as CjTypeStatement?
//            if (classDeclaration?.modifierList != null) {
//                val modifier = classDeclaration.modifierList!!.getModifier(CjTokens.DATA_KEYWORD)
//                if (modifier != null) {
//                    return modifier
//                }
//            }
//
//            throw IllegalStateException("No data modifier is found for data class $dataClass")
//        }

        private fun findInvisibleOverriddenDescriptor(
            declared: CallableMemberDescriptor,
            declaringClass: ClassAndEnumDescriptor,
            cangjieTypeRefiner: CangJieTypeRefiner,
            languageVersionSettings: LanguageVersionSettings
        ): CallableMemberDescriptor? {
            for (supertype in cangjieTypeRefiner.refineSupertypes(declaringClass)) {
                val all = linkedSetOf<CallableMemberDescriptor>()
                all.addAll(
                    supertype.memberScope.getContributedFunctions(
                        declared.name, NoLookupLocation.MATCH_CHECK_OVERRIDES
                    )
                )
//                不可以重写变量
//                all.addAll(
//                    supertype.memberScope.getContributedVariables(
//                        declared.name,
//                        NoLookupLocation.MATCH_CHECK_OVERRIDES
//                    )
//                )
                for (fromSuper in all) {
                    if (OverridingUtil.DEFAULT.isOverridableBy(
                            fromSuper, declared, null
                        ).result == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE
                    ) {
                        if (OverridingUtil.isVisibleForOverride(
                                declared, fromSuper, languageVersionSettings.useSpecialRulesForPrivateSealedConstructors
                            )
                        ) {
                            throw IllegalStateException(
                                "Descriptor " + fromSuper + " is overridable by " + declared + " and visible but does not appear in its getOverriddenDescriptors()"
                            )
                        }
                        return fromSuper
                    }
                }
            }
            return null
        }

        fun shouldReportParameterNameOverrideWarning(
            parameterFromSubclass: ValueParameterDescriptor, parameterFromSuperclass: ValueParameterDescriptor
        ): Boolean {
            return parameterFromSubclass.containingDeclaration.hasStableParameterNames() && parameterFromSuperclass.containingDeclaration.hasStableParameterNames() && parameterFromSuperclass.name != parameterFromSubclass.name
        }

        private fun checkPropertyKind(descriptor: CallableMemberDescriptor, isVar: Boolean): Boolean {
            return descriptor is PropertyDescriptor && descriptor.isVar == isVar
        }
    }
}


