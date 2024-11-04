package com.linqingying.cangjie.ide.quickfix

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.ide.quickfix.RemoveOptionalFix.NullableKind.*
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls


class RemoveOptionalFix(element: CjOptionType, private val typeOfError: NullableKind) :
    CangJiePsiOnlyQuickFixAction<CjOptionType>(element) {
    enum class NullableKind(@Nls val message: String) {
        REDUNDANT(CangJieBundle.message("remove.redundant")),
        SUPERTYPE(CangJieBundle.message("text.remove.question")),
        USELESS(CangJieBundle.message("remove.useless")),
        PROPERTY(CangJieBundle.message("make.not.nullable")),
        VARIABLE(CangJieBundle.message("make.not.nullable")),


        //        TODO 恶搞警告
        REDUNDANT_DOLL("结束套娃的一生"),

    }

    override fun getFamilyName() = CangJieBundle.message("text.remove.question")

    override fun getText() = typeOfError.message

    override fun invoke(project: Project, editor: Editor?, file: CjFile) {
        val element = element ?: return
        val type = element.getInnerType()
            ?: error("No inner type ${element.text}, should have been rejected in createFactory()")
        element.replace(type)
    }

    companion object {

        //        TODO 恶搞警告
        val removeForRedundantDoll = createFactory(REDUNDANT_DOLL)

        val removeForRedundant = createFactory(REDUNDANT)


        val removeForSuperType = createFactory(SUPERTYPE)
        val removeForUseless = createFactory(USELESS)
        val removeForLateInitProperty = createFactory(PROPERTY)
        val removeForLateInitVariable = createFactory(VARIABLE)

        private fun createFactory(typeOfError: NullableKind): QuickFixesPsiBasedFactory<CjElement> {
            return quickFixesPsiBasedFactory { e ->
                when (typeOfError) {
                    REDUNDANT, SUPERTYPE, USELESS, REDUNDANT_DOLL -> {
                        val nullType: CjOptionType? = when (e) {
                            is CjTypeReference -> e.typeElement as? CjOptionType
                            else -> e.getNonStrictParentOfType()
                        }
                        if (nullType?.getInnerType() == null) return@quickFixesPsiBasedFactory emptyList()
                        listOf(RemoveOptionalFix(nullType, typeOfError))
                    }

                    PROPERTY -> {
                        val property = e as? CjProperty ?: return@quickFixesPsiBasedFactory emptyList()
                        val typeReference = property.typeReference ?: return@quickFixesPsiBasedFactory emptyList()
                        val typeElement =
                            typeReference.typeElement as? CjOptionType ?: return@quickFixesPsiBasedFactory emptyList()
                        if (typeElement.getInnerType() == null) return@quickFixesPsiBasedFactory emptyList()
                        listOf(RemoveOptionalFix(typeElement, PROPERTY))
                    }

                    VARIABLE -> {
                        val property = e as? CjVariable ?: return@quickFixesPsiBasedFactory emptyList()
                        val typeReference = property.typeReference ?: return@quickFixesPsiBasedFactory emptyList()
                        val typeElement =
                            typeReference.typeElement as? CjOptionType ?: return@quickFixesPsiBasedFactory emptyList()
                        if (typeElement.getInnerType() == null) return@quickFixesPsiBasedFactory emptyList()
                        listOf(RemoveOptionalFix(typeElement, PROPERTY))
                    }
                }
            }
        }
    }
}
