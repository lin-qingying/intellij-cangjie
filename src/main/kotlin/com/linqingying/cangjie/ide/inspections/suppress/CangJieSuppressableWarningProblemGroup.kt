package com.linqingying.cangjie.ide.inspections.suppress

import com.linqingying.cangjie.diagnostics.Severity
import com.linqingying.cangjie.ide.codeinsight.CangJieCodeInsightBundle
import com.linqingying.cangjie.psi.*
import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.codeInspection.SuppressableProblemGroup
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace


class CangJieSuppressableWarningProblemGroup(private val factoryName: String) : SuppressableProblemGroup {
    override fun getProblemName(): String = factoryName

    override fun getSuppressActions(element: PsiElement?): Array<SuppressIntentionAction> {
        return if (element != null) {
            createSuppressWarningActions(element, Severity.WARNING, factoryName).toTypedArray()
        } else {
            SuppressIntentionAction.EMPTY_ARRAY
        }
    }
}

fun createSuppressWarningActions(
    element: PsiElement,
    severity: Severity,
    suppressionKey: String
): List<SuppressIntentionAction> {
    if (severity != Severity.WARNING) {
        return emptyList()
    }

    val actions = arrayListOf<SuppressIntentionAction>()
    var current: PsiElement? = element
    var suppressAtStatementAllowed = true
    while (current != null) {
        when {
            current is CjDeclaration && current !is CjDestructuringDeclaration -> {
                val declaration = current
                val kind = DeclarationKindDetector.detect(declaration)
                if (kind != null) {
                    actions.add(CangJieSuppressIntentionAction(declaration, suppressionKey, kind))
                }
                suppressAtStatementAllowed = false
            }

            current is CjExpression && suppressAtStatementAllowed -> {
                // Add suppress action at first statement
                if (current.parent is CjBlockExpression || current.parent is CjDestructuringDeclaration) {
                    val kind = if (current.parent is CjBlockExpression)
                        CangJieCodeInsightBundle.message("declaration.kind.statement")
                    else
                        CangJieCodeInsightBundle.message("declaration.kind.initializer")

                    val hostKind = AnnotationHostKind(kind, null, true)
                    actions.add(CangJieSuppressIntentionAction(current, suppressionKey, hostKind))
                    suppressAtStatementAllowed = false
                }
            }

            current is PsiWhiteSpace && current.prevSibling is CjClassLikeDeclaration -> {
                current = current.prevSibling
                continue
            }

            current is CjFile -> {
                val hostKind =
                    AnnotationHostKind(CangJieCodeInsightBundle.message("declaration.kind.file"), current.name, true)
                actions.add(CangJieSuppressIntentionAction(current, suppressionKey, hostKind))
                break
            }
        }

        current = current.parent
    }

    return actions
}

private object DeclarationKindDetector : CjVisitor<AnnotationHostKind?, Unit?>() {
    fun detect(declaration: CjDeclaration) = declaration.accept(this, null)

    override fun visitDeclaration(declaration: CjDeclaration, data: Unit?) = null

    private fun getDeclarationName(declaration: CjDeclaration): @NlsSafe String {
        return declaration.name ?: CangJieCodeInsightBundle.message("declaration.name.anonymous")
    }

    override fun visitClass(declaration: CjClass, data: Unit?): AnnotationHostKind {


        return visitTypeStatement(declaration, data)
    }

    override fun visitInterface(cinterface: CjInterface, data: Unit?): AnnotationHostKind {
        return visitTypeStatement(cinterface, data)

    }

    override fun visitEnum(cenum: CjEnum, data: Unit?): AnnotationHostKind {
        return visitTypeStatement(cenum, data)

    }

    override fun visitStruct(cstruct: CjStruct, data: Unit?): AnnotationHostKind {
        return visitTypeStatement(cstruct, data)

    }

    override fun visitTypeStatement(typeStatement: CjTypeStatement, data: Unit?): AnnotationHostKind {

        val kind = when (typeStatement) {
            is CjClass -> CangJieCodeInsightBundle.message("declaration.kind.class")
            is CjInterface -> CangJieCodeInsightBundle.message("declaration.kind.interface")
            is CjStruct -> CangJieCodeInsightBundle.message("declaration.kind.struct")
            is CjEnum -> CangJieCodeInsightBundle.message("declaration.kind.enum")
            else -> ""
        }
        return AnnotationHostKind(kind, getDeclarationName(typeStatement), newLineNeeded = true)
    }

    override fun visitNamedFunction(declaration: CjNamedFunction, data: Unit?): AnnotationHostKind {
        val kind = CangJieCodeInsightBundle.message("declaration.kind.fun")
        return AnnotationHostKind(kind, getDeclarationName(declaration), newLineNeeded = true)
    }

    override fun visitProperty(declaration: CjProperty, data: Unit?): AnnotationHostKind {
        val kind = when {
            declaration.isVar -> CangJieCodeInsightBundle.message("declaration.kind.var")
            else -> CangJieCodeInsightBundle.message("declaration.kind.val")
        }
        return AnnotationHostKind(kind, getDeclarationName(declaration), newLineNeeded = true)
    }

    override fun visitTypeParameter(declaration: CjTypeParameter, data: Unit?): AnnotationHostKind {
        val kind = CangJieCodeInsightBundle.message("declaration.kind.type.parameter")
        return AnnotationHostKind(kind, getDeclarationName(declaration), newLineNeeded = false)
    }

//    override fun visitEnumEntry(declaration: CjEnumEntry, data: Unit?): AnnotationHostKind {
//        val kind = CangJieCodeInsightBundle.message("declaration.kind.enum.entry")
//        return AnnotationHostKind(kind, getDeclarationName(declaration), newLineNeeded = true)
//    }

    override fun visitParameter(declaration: CjParameter, data: Unit?): AnnotationHostKind {
        val kind = CangJieCodeInsightBundle.message("declaration.kind.parameter")
        return AnnotationHostKind(kind, getDeclarationName(declaration), newLineNeeded = false)
    }

    override fun visitSecondaryConstructor(declaration: CjSecondaryConstructor, data: Unit?): AnnotationHostKind {
        val kind = CangJieCodeInsightBundle.message("declaration.kind.secondary.constructor.of")
        return AnnotationHostKind(kind, getDeclarationName(declaration), newLineNeeded = true)
    }


}
