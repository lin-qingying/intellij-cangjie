package com.linqingying.cangjie.ide.codeinsight.quickDoc.cdoc

import com.linqingying.cangjie.doc.parser.CDocKnownTag
import com.linqingying.cangjie.doc.psi.CDoc
import com.linqingying.cangjie.doc.psi.impl.CDocSection
import com.linqingying.cangjie.doc.psi.impl.CDocTag
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.*
import com.linqingying.cangjie.utils.toLowerCaseAsciiOnly
import com.intellij.psi.util.PsiTreeUtil

fun CjElement.findCDocByPsi(): CDocContent? {
    return this.lookupOwnedCDoc()
        ?: this.lookupCDocInContainer()
}
private fun CjElement.lookupOwnedCDoc(): CDocContent? {
    // CDoc for primary constructor is located inside of its class CDoc
    val psiDeclaration = when (this) {
        is CjPrimaryConstructor -> containingTypeStatement
        else -> this
    }

    if (psiDeclaration is CjDeclaration) {
        val kdoc = psiDeclaration.docComment
        if (kdoc != null) {
            if (this is CjConstructor<*>) {
                // ConstructorDescriptor resolves to the same JetDeclaration
                val constructorSection = kdoc.findSectionByTag(CDocKnownTag.CONSTRUCTOR)
                if (constructorSection != null) {
                    // if annotated with @constructor tag and the caret is on constructor definition,
                    // then show @constructor description as the main content, and additional sections
                    // that contain @param tags (if any), as the most relatable ones
                    // practical example: val foo = Fo<caret>o("argument") -- show @constructor and @param content
                    val paramSections = kdoc.findSectionsContainingTag(CDocKnownTag.PARAM)
                    return CDocContent(constructorSection, paramSections)
                }
            }
            return CDocContent(kdoc.getDefaultSection(), kdoc.getAllSections())
        }
    }
    return null
}
/**
 * Looks for sections that have a deeply nested [tag],
 * as opposed to [CDoc.findSectionByTag], which only looks among the top level
 */
private fun CDoc.findSectionsContainingTag(tag: CDocKnownTag): List<CDocSection> {
    return getChildrenOfType<CDocSection>()
        .filter { it.findTagByName(tag.name.toLowerCaseAsciiOnly()) != null }
}



private fun CjElement.lookupCDocInContainer(): CDocContent? {
    val subjectName = name
    val containingDeclaration =
        PsiTreeUtil.findFirstParent(this, true) {
            it is CjDeclarationWithBody && it !is CjPrimaryConstructor
                    || it is CjTypeStatement
        }

    val containerCDoc = containingDeclaration?.getChildOfType<CDoc>()
    if (containerCDoc == null || subjectName == null) return null
    val propertySection = containerCDoc.findSectionByTag(CDocKnownTag.PROPERTY, subjectName)
    val paramTag = containerCDoc.findDescendantOfType<CDocTag> { it.knownTag == CDocKnownTag.PARAM && it.getSubjectName() == subjectName }

    val primaryContent = when {
        // class Foo(val <caret>s: String)
        this is CjParameter && this.isPropertyParameter() -> propertySection ?: paramTag
        // fun some(<caret>f: String) || class Some<<caret>T: Base> || Foo(<caret>s = "argument")
        this is CjParameter || this is CjTypeParameter -> paramTag
        // if this property is declared separately (outside primary constructor), but it's for some reason
        // annotated as @property in class's description, instead of having its own CDoc
        this is CjProperty && containingDeclaration is CjTypeStatement -> propertySection
        else -> null
    }
    return primaryContent?.let {
        // makes little sense to include any other sections, since we found
        // documentation for a very specific element, like a property/param
        CDocContent(it, sections = emptyList())
    }
}
