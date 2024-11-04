package com.linqingying.cangjie.ide.structureView

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptorWithVisibility
import com.linqingying.cangjie.descriptors.DescriptorVisibilities
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.ui.Queryable
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.TestOnly
import javax.swing.Icon
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Required until 2 implementations would be merged
 */
interface AbstractCangJieStructureViewElement {
    val accessLevel: Int?
    val isPublic: Boolean
    val element: NavigatablePsiElement
}

class CangJieStructureViewElement(
    override val element: NavigatablePsiElement,
    private val isInherited: Boolean = false
) : PsiTreeElementBase<NavigatablePsiElement>(element), Queryable, AbstractCangJieStructureViewElement {

    private var cangjiePresentation
            by AssignableLazyProperty {
                CangJieStructureElementPresentation(isInherited, element, countDescriptor())
            }

    var visibility
            by AssignableLazyProperty {
                Visibility(countDescriptor())
            }
        private set

    constructor(element: NavigatablePsiElement, descriptor: DeclarationDescriptor, isInherited: Boolean) : this(
        element,
        isInherited
    ) {
        if (element !is CjElement) {
            // Avoid storing descriptor in fields
            cangjiePresentation = CangJieStructureElementPresentation(isInherited, element, descriptor)
            visibility = Visibility(descriptor)
        }
    }

    override val accessLevel: Int?
        get() = visibility.accessLevel
    override val isPublic: Boolean
        get() = visibility.isPublic

    override fun getPresentation(): ItemPresentation = cangjiePresentation
    override fun getLocationString(): String? = cangjiePresentation.locationString
    override fun getIcon(open: Boolean): Icon? = cangjiePresentation.getIcon(open)
    override fun getPresentableText(): String? = cangjiePresentation.presentableText

    @TestOnly
    override fun putInfo(info: MutableMap<in String, in String?>) {
        // Sanity check for API consistency
        assert(presentation.presentableText == presentableText) { "Two different ways of getting presentableText" }
        assert(presentation.locationString == locationString) { "Two different ways of getting locationString" }

        info["text"] = presentableText
        info["location"] = locationString
    }

    override fun getChildrenBase(): Collection<StructureViewTreeElement> {
        val children = when (val element = element) {
            is CjFile -> {
                val declarations = element.declarations

                element
                    .declarations
            }

            is CjClass -> element.getStructureDeclarations()
            is CjTypeStatement -> element.declarations
            is CjFunction, is CjClassInitializer, is CjProperty -> element.collectLocalDeclarations()
            else -> emptyList()
        }

        return children.map { CangJieStructureViewElement(it, false) }
    }

    private fun PsiElement.collectLocalDeclarations(): List<CjDeclaration> {
        val result = mutableListOf<CjDeclaration>()

        acceptChildren(object : CjTreeVisitorVoid() {

            override fun visitTypeStatement(typeStatement: CjTypeStatement) {
                result.add(typeStatement)

            }

            override fun visitNamedFunction(function: CjNamedFunction) {
                result.add(function)
            }
        })

        return result
    }

    private fun countDescriptor(): DeclarationDescriptor? {
        val element = element
        return when {
            !element.isValid -> null
            element !is CjDeclaration -> null
            element is CjAnonymousInitializer -> null
            else -> runReadAction {
                if (!DumbService.isDumb(element.getProject())) {
                    element.resolveToDescriptorIfAny()
                } else null
            }
        }
    }

    class Visibility(descriptor: DeclarationDescriptor?) {
        private val visibility = (descriptor as? DeclarationDescriptorWithVisibility)?.visibility

        val isPublic: Boolean
            get() = visibility == DescriptorVisibilities.PUBLIC

        val accessLevel: Int?
            get() = when {
                visibility == DescriptorVisibilities.PUBLIC -> 1
                visibility == DescriptorVisibilities.INTERNAL -> 2
                visibility == DescriptorVisibilities.PROTECTED -> 3
                visibility?.let { DescriptorVisibilities.isPrivate(it) } == true -> 4
                else -> null
            }
    }
}

private class AssignableLazyProperty<in R, T : Any>(val init: () -> T) : ReadWriteProperty<R, T> {
    private var _value: T? = null

    override fun getValue(thisRef: R, property: KProperty<*>): T {
        return _value ?: init().also { _value = it }
    }

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        _value = value
    }
}

fun CjTypeStatement.getStructureDeclarations() =
    buildList {
        primaryConstructor?.let { add(it) }
        primaryConstructorParameters.filterTo(this) { it.hasLetOrVar() }
        addAll(declarations)
    }

