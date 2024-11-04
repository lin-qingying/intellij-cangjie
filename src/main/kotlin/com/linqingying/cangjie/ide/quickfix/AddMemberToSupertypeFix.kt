package com.linqingying.cangjie.ide.quickfix

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.*
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
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.ui.IconManager
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

    private fun generateCandidateMembers(memberElement: CjCallableDeclaration): List<CallableMemberDescriptor> {
        val memberDescriptor =
            memberElement.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? CallableMemberDescriptor
                ?: return emptyList()
        val containingClass = memberDescriptor.containingDeclaration as? ClassDescriptor ?: return emptyList()
        // TODO: filter out impossible supertypes (for example when argument's type isn't visible in a superclass).
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

    private fun MutableList<CangJieType>.sortSubtypesFirst(): List<CangJieType> {
        val typeChecker = CangJieTypeChecker.DEFAULT
        for (i in 1 until size) {
            val currentType = this[i]
            for (j in 0 until i) {
                if (typeChecker.isSubtypeOf(currentType, this[j])) {
                    this.removeAt(i)
                    this.add(j, currentType)
                    break
                }
            }
        }
        return this
    }

    private fun generateMemberSignatureForType(
        memberDescriptor: CallableMemberDescriptor,
        typeDescriptor: ClassDescriptor
    ): CallableMemberDescriptor {
        // TODO: support for generics.
        val modality = if (typeDescriptor.kind == ClassKind.INTERFACE || typeDescriptor.modality == Modality.SEALED) {
            Modality.ABSTRACT
        } else {
            typeDescriptor.modality
        }

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
    override val icon: Icon = IconManager.getInstance().getPlatformIcon(com.intellij.ui.PlatformIcons.Function)

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
            val targetClass =targetElement as? CjClass
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
