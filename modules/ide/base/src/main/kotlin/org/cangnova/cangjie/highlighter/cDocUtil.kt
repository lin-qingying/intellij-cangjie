package org.cangnova.cangjie.highlighter

import com.intellij.psi.impl.source.tree.LightTreeUtil.getChildrenOfType
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lexer.cdoc.CDocContent
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag
import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocTag
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.psiUtil.findDescendantOfType
import org.cangnova.cangjie.psi.psiUtil.getChildOfType
import org.cangnova.cangjie.psi.psiUtil.getChildrenOfType
import org.cangnova.cangjie.psi.psiUtil.isPropertyParameter
import org.cangnova.cangjie.utils.toLowerCaseAsciiOnly


fun CjElement.findCDocByPsi(): CDocContent? {
    return this.lookupOwnedCDoc()
        ?: this.lookupCDocInContainer()
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

private fun CjElement.lookupOwnedCDoc(): CDocContent? {
    // CDoc for primary constructor is located inside of its class CDoc
    val psiDeclaration = when (this) {
        is CjPrimaryConstructor -> getContainingTypeStatement()
        else -> this
    }

    if (psiDeclaration is CjDeclaration) {
        val cdoc = psiDeclaration.docComment
        if (cdoc != null) {
            if (this is CjConstructor<*>) {
                // ConstructorDescriptor resolves to the same JetDeclaration
                val constructorSection = cdoc.findSectionByTag(CDocKnownTag.CONSTRUCTOR)
                if (constructorSection != null) {
                    // if annotated with @constructor tag and the caret is on constructor definition,
                    // then show @constructor description as the main content, and additional sections
                    // that contain @param tags (if any), as the most relatable ones
                    // practical example: val foo = Fo<caret>o("argument") -- show @constructor and @param content
                    val paramSections = cdoc.findSectionsContainingTag(CDocKnownTag.PARAM)
                    return CDocContent(constructorSection, paramSections)
                }
            }
            return CDocContent(cdoc.getDefaultSection(), cdoc.getAllSections())
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