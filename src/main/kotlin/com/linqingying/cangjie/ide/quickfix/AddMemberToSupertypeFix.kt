/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.ide.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.ui.IconManager
import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.ClassKind
import com.linqingying.cangjie.descriptors.Modality
import com.linqingying.cangjie.diagnostics.Diagnostic
import com.linqingying.cangjie.ide.IdeDescriptorRenderers
import com.linqingying.cangjie.ide.ShortenReferences
import com.linqingying.cangjie.ide.TemplateKind
import com.linqingying.cangjie.ide.getFunctionBodyTextFromTemplate
import com.linqingying.cangjie.ide.imports.importableFqName
import com.linqingying.cangjie.ide.quickfix.overrideImplement.getOrCreateBody
import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.implicitModality
import com.linqingying.cangjie.psi.psiUtil.modalityModifier
import com.linqingying.cangjie.references.util.DescriptorToSourceUtilsIde
import com.linqingying.cangjie.renderer.DescriptorRenderer
import com.linqingying.cangjie.renderer.DescriptorRendererModifier
import com.linqingying.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.linqingying.cangjie.resolve.isSameModule
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.checker.CangJieTypeChecker
import com.linqingying.cangjie.types.util.supertypes
import com.linqingying.cangjie.utils.executeWriteCommand
import com.linqingying.cangjie.utils.safeAs
import org.jetbrains.annotations.Nls
import javax.swing.Icon

abstract class AddMemberToSupertypeFix(element: CjCallableDeclaration, private val candidateMembers: List<MemberData>) :
    CangJieQuickFixAction<CjCallableDeclaration>(element), LowPriorityAction {

    class MemberData(val signaturePreview: String, val sourceCode: String, val targetClass: CjTypeStatement)

    init {
        assert(candidateMembers.isNotEmpty())
    }

    abstract val kind: String
    abstract val icon: Icon

    override fun getText(): String =
        candidateMembers.singleOrNull()?.let { actionName(it) }
            ?: CangJieBundle.message("fix.add.member.supertype.text", kind)

    override fun getFamilyName() = CangJieBundle.message("fix.add.member.supertype.family", kind)

    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, editor: Editor?, file: CjFile) {
        CommandProcessor.getInstance().runUndoTransparentAction {
            if (candidateMembers.size == 1 || editor == null || !editor.component.isShowing) {
                addMember(candidateMembers.first(), project)
            } else {
                JBPopupFactory.getInstance().createListPopup(createMemberPopup(project)).showInBestPositionFor(editor)
            }
        }
    }

    private fun addMember(memberData: MemberData, project: Project) {
        project.executeWriteCommand(CangJieBundle.message("fix.add.member.supertype.progress", kind)) {
            element?.removeDefaultParameterValues()
            val classBody = memberData.targetClass.getOrCreateBody()
            val memberElement: CjCallableDeclaration = CjPsiFactory(project).createDeclaration(memberData.sourceCode)
//            memberElement.copyAnnotationEntriesFrom(element)
            val insertedMemberElement = classBody.addBefore(memberElement, classBody.rBrace) as CjCallableDeclaration
            ShortenReferences.DEFAULT.process(insertedMemberElement)
            val modifierToken = insertedMemberElement.modalityModifier()?.node?.elementType as? CjModifierKeywordToken
                ?: return@executeWriteCommand
            if (insertedMemberElement.implicitModality() == modifierToken) {
                RemoveModifierFixBase(insertedMemberElement, modifierToken, true).invoke()
            }
        }
    }

    private fun CjCallableDeclaration.removeDefaultParameterValues() {
        valueParameters.forEach {
            it.defaultValue?.delete()
            it.equalsToken?.delete()
        }
    }

//    private fun CjCallableDeclaration.copyAnnotationEntriesFrom(member: CjCallableDeclaration?) {
//        member?.annotationEntries?.reversed()?.forEach { addAnnotationEntry(it) }
//    }

    private fun createMemberPopup(project: Project): ListPopupStep<*> {
        return object : BaseListPopupStep<MemberData>(
            CangJieBundle.message("fix.add.member.supertype.choose.type"),
            candidateMembers
        ) {
            override fun isAutoSelectionEnabled() = false

            override fun onChosen(selectedValue: MemberData, finalChoice: Boolean): PopupStep<*>? {
                if (finalChoice) {
                    addMember(selectedValue, project)
                }
                return PopupStep.FINAL_CHOICE
            }

            override fun getIconFor(value: MemberData) = icon
            override fun getTextFor(value: MemberData) = actionName(value)
        }
    }

    @Nls
    private fun actionName(memberData: MemberData): String =
        CangJieBundle.message(
            "fix.add.member.supertype.add.to",
            memberData.signaturePreview, memberData.targetClass.name.toString()
        )
}

abstract class AddMemberToSupertypeFactory : CangJieSingleIntentionActionFactory() {
    protected fun getCandidateMembers(memberElement: CjCallableDeclaration): List<AddMemberToSupertypeFix.MemberData> {
        val descriptors = generateCandidateMembers(memberElement)
        return descriptors.mapNotNull { createMemberData(it, memberElement) }
    }

    abstract fun createMemberData(
        memberDescriptor: CallableMemberDescriptor,
        memberElement: CjCallableDeclaration
    ): AddMemberToSupertypeFix.MemberData?

    /**
     * 根据给定的成员元素生成候选成员描述符列表。
     *
     * @param memberElement 成员元素，类型为 [CjCallableDeclaration]
     * @return 返回一个 [CallableMemberDescriptor] 列表
     */
    private fun generateCandidateMembers(memberElement: CjCallableDeclaration): List<CallableMemberDescriptor> {
        val memberDescriptor =
            memberElement.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? CallableMemberDescriptor
                ?: return emptyList()
        val containingClass = memberDescriptor.containingDeclaration as? ClassDescriptor ?: return emptyList()
        // TODO: 过滤掉不可能的超类型（例如当参数的类型在超类中不可见时）。
        return getCangJieSourceSuperClasses(containingClass).map {
            generateMemberSignatureForType(
                memberDescriptor,
                it
            )
        }
    }


    private fun getCangJieSourceSuperClasses(classDescriptor: ClassDescriptor): List<ClassDescriptor> {
        val supertypes = classDescriptor.defaultType.supertypes().toMutableList().sortSubtypesFirst()
        return supertypes.mapNotNull { type ->
            type.constructor.declarationDescriptor.safeAs<ClassDescriptor>().takeIf {
//                排除掉非本模块的声明

                it?.let { it1 -> classDescriptor.isSameModule(it1) } == true
            }
        }
    }

    /**
     * 对互见词进行子类型优先排序的方法
     * 此方法旨在对一个互见词列表进行排序，使得每个互见词的子类型优先于其本身出现
     * 互见词的子类型是指在结构或意义上属于该互见词下属的类型
     *
     * @return 返回排序后的互见词列表，子类型优先于其本身出现
     */
    private fun MutableList<CangJieType>.sortSubtypesFirst(): List<CangJieType> {
        // 使用默认的互见词类型检查器来判断子类型关系
        val typeChecker = CangJieTypeChecker.DEFAULT

        // 遍历列表中的每个互见词，除了第一个
        for (i in 1 until size) {
            val currentType = this[i]

            // 检查当前互见词与之前遍历过的互见词的子类型关系
            for (j in 0 until i) {
                // 如果当前互见词是之前某个互见词的子类型
                if (typeChecker.isSubtypeOf(currentType, this[j])) {
                    // 将当前互见词从列表中移除并插入到其子类型之前
                    this.removeAt(i)
                    this.add(j, currentType)
                    // 继续下一次外层循环，避免在当前循环中索引越界
                    break
                }
            }
        }

        // 返回经过子类型优先排序的互见词列表
        return this
    }

    /**
     * 为给定的类型生成成员签名。
     *
     * @param memberDescriptor 成员描述符，表示要生成签名的成员。
     * @param typeDescriptor 类描述符，表示成员所属的类型。
     * @return 返回一个新的 CallableMemberDescriptor，其签名已根据给定的类型进行调整。
     */
    private fun generateMemberSignatureForType(
        memberDescriptor: CallableMemberDescriptor,
        typeDescriptor: ClassDescriptor
    ): CallableMemberDescriptor {
        // TODO: 支持泛型。
        // 根据类型描述符的种类和模态性确定新的模态性。
        val modality = if (typeDescriptor.kind == ClassKind.INTERFACE || typeDescriptor.modality == Modality.SEALED) {
            Modality.ABSTRACT
        } else {
            typeDescriptor.modality
        }

        // 复制成员描述符，并更新其类型描述符、模态性、可见性和种类。
        return memberDescriptor.copy(
            typeDescriptor,
            modality,
            memberDescriptor.visibility,
            CallableMemberDescriptor.Kind.DECLARATION,
            /* copyOverrides = */ false
        )
    }

}

class AddFunctionToSupertypeFix private constructor(element: CjNamedFunction, functions: List<MemberData>) :
    AddMemberToSupertypeFix(element, functions) {

    override val kind: String = "function"
    override val icon: Icon = AllIcons.Nodes.Function

    companion object : AddMemberToSupertypeFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val functionElement = diagnostic.psiElement as? CjNamedFunction ?: return null
            val candidateFunctions = getCandidateMembers(functionElement)
            return if (candidateFunctions.isNotEmpty()) AddFunctionToSupertypeFix(
                functionElement,
                candidateFunctions
            ) else null
        }

        override fun createMemberData(
            memberDescriptor: CallableMemberDescriptor,
            memberElement: CjCallableDeclaration
        ): MemberData? {
            val classDescriptor = memberDescriptor.containingDeclaration as ClassDescriptor
            val project = memberElement.project
            var sourceCode =
                IdeDescriptorRenderers.SOURCE_CODE.withNoAnnotations().withDefaultValueOption(project)
                    .render(memberDescriptor)
            if (classDescriptor.kind != ClassKind.INTERFACE && memberDescriptor.modality != Modality.ABSTRACT) {
                val returnType = memberDescriptor.returnType
                sourceCode += if (returnType == null || !CangJieBuiltIns.isUnit(returnType)) {
                    val bodyText = getFunctionBodyTextFromTemplate(
                        project,
                        TemplateKind.FUNCTION,
                        memberDescriptor.name.asString(),
                        memberDescriptor.returnType?.let { IdeDescriptorRenderers.SOURCE_CODE.renderType(it) }
                            ?: "Unit",
                        classDescriptor.importableFqName
                    )
                    "{\n$bodyText\n}"
                } else {
                    "{}"
                }
            }

            val targetElement = DescriptorToSourceUtilsIde.getAnyDeclaration(project, classDescriptor)
            val targetClass = targetElement as? CjClass
                ?: targetElement as? CjInterface ?: return null
            return MemberData(
                IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.withDefaultValueOption(project)
                    .render(memberDescriptor),
                sourceCode,
                targetClass
            )
        }
    }
}

private fun DescriptorRenderer.withNoAnnotations(): DescriptorRenderer {
    return withOptions {
        modifiers -= DescriptorRendererModifier.ANNOTATIONS
    }
}

private fun DescriptorRenderer.withDefaultValueOption(project: Project): DescriptorRenderer {
    return withOptions {
//        this.defaultParameterValueRenderer = {
//            OptionalParametersHelper.defaultParameterValueExpression(it, project)?.text
//                ?: error("value parameter renderer shouldn't be called when there is no value to render")
//        }
    }
}
