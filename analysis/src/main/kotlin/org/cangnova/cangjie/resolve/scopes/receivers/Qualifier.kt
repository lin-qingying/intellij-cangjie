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

package org.cangnova.cangjie.resolve.scopes.receivers

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.calls.model.CangJieCall
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.StaticMemberScope
import org.cangnova.cangjie.resolve.source.MemberScopeImpl
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.utils.Printer
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import org.cangnova.cangjie.resolve.scopes.ChainedMemberScope
import org.cangnova.cangjie.utils.classValueType
import javax.swing.Icon

interface Qualifier : QualifierReceiver {
    val referenceExpression: CjSimpleNameExpression
}

/**
 * enum<T,T>
 */
interface EnumClassQualifierByCall : QualifierReceiver {
    val referenceExpression: CjCallElement
    override val descriptor: ClassifierDescriptorWithTypeParameters

}

interface ClassifierQualifier : Qualifier {
    override val descriptor: ClassifierDescriptorWithTypeParameters
}

val QualifierReceiver.expression: CjExpression
    get() {
        return when (this) {
            is Qualifier -> referenceExpression.getTopmostParentQualifiedExpressionForSelector() ?: referenceExpression
            is EnumClassQualifierByCall -> referenceExpression.calleeExpression!!
            else -> throw IllegalStateException("QualifierReceiver is not a Qualifier")
        }
    }


val Qualifier.expression: CjExpression
    get() = referenceExpression.getTopmostParentQualifiedExpressionForSelector() ?: referenceExpression


class PackageQualifier(
    override val referenceExpression: CjSimpleNameExpression,
    override val descriptor: PackageViewDescriptor
) : Qualifier {
    override val classValueReceiver: ReceiverValue? get() = null
    override val staticScope: MemberScope get() = descriptor.memberScope

    override fun toString() = "Package{$descriptor}"
}


class EnumClassQualifier(
    override val referenceExpression: CjCallElement,
    override val descriptor: ClassDescriptor,
    val call: CangJieCall?
) : EnumClassQualifierByCall {
    class EnumCallElement(val referenceExpression: CjSimpleNameExpression) : CjCallElement {
        override val calleeExpression: CjExpression
            get() = referenceExpression
        override val valueArgumentList: CjValueArgumentList?
            get() = null
        override val valueArguments: List<ValueArgument>
            get() = emptyList()
        override val lambdaArguments: List<CjLambdaArgument>
            get() = emptyList()
        override val typeArguments: List<CjTypeProjection>
            get() = emptyList()
        override val typeArgumentList: CjTypeArgumentList?
            get() = null

        override fun <D> acceptChildren(visitor: CjVisitor<Unit, D>, data: D) {

        }

        override fun acceptChildren(visitor: PsiElementVisitor) {

        }

        override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
            return referenceExpression.accept(visitor, data)
        }

        override fun accept(visitor: PsiElementVisitor) {

        }

        override fun getReference(): PsiReference? {
            return null
        }

        override fun <T : Any?> getUserData(key: Key<T>): T? {
            return null

        }

        override fun <T : Any?> putUserData(key: Key<T>, value: T?) {

        }

        override fun getIcon(flags: Int): Icon {
            TODO("Not yet implemented")
        }

        override fun getProject(): Project {
            return referenceExpression.project
        }

        override fun getLanguage(): Language {
            return CangJieLanguage
        }

        override fun getManager(): PsiManager {
            return referenceExpression.manager
        }

        override fun getChildren(): Array<PsiElement> {
            return emptyArray()
        }

        override fun getParent(): PsiElement {
            return referenceExpression.parent
        }

        override fun getFirstChild(): PsiElement {
            return referenceExpression.firstChild
        }

        override fun getLastChild(): PsiElement {
            return referenceExpression.lastChild
        }

        override fun getNextSibling(): PsiElement {
            return referenceExpression.nextSibling
        }

        override fun getPrevSibling(): PsiElement {
            return referenceExpression.prevSibling
        }

        override fun getContainingFile(): PsiFile {
            return referenceExpression.containingFile
        }

        override fun getTextRange(): TextRange {
            return referenceExpression.textRange
        }

        override fun getStartOffsetInParent(): Int {
            return referenceExpression.startOffsetInParent
        }

        override fun getTextLength(): Int {
            return referenceExpression.textLength
        }

        override fun findElementAt(offset: Int): PsiElement? {
            return referenceExpression.findElementAt(offset)
        }

        override fun findReferenceAt(offset: Int): PsiReference? {
            return referenceExpression.findReferenceAt(offset)
        }

        override fun getTextOffset(): Int {
            return referenceExpression.textOffset
        }

        override fun getText(): String {
            return referenceExpression.text
        }

        override fun textToCharArray(): CharArray {
            return referenceExpression.textToCharArray()
        }

        override fun getNavigationElement(): PsiElement {
            return referenceExpression.navigationElement
        }

        override fun getOriginalElement(): PsiElement {
            return referenceExpression.originalElement
        }

        override fun textMatches(text: CharSequence): Boolean {
            return referenceExpression.textMatches(text)
        }

        override fun textMatches(element: PsiElement): Boolean {
            return referenceExpression.textMatches(element)
        }

        override fun textContains(c: Char): Boolean {
            return referenceExpression.textContains(c)
        }

        override fun copy(): PsiElement {
            return referenceExpression.copy()
        }

        override fun add(element: PsiElement): PsiElement {
            return referenceExpression.add(element)
        }

        override fun addBefore(element: PsiElement, anchor: PsiElement?): PsiElement {
            return referenceExpression.addBefore(element, anchor)
        }

        override fun addAfter(element: PsiElement, anchor: PsiElement?): PsiElement {
            return referenceExpression.addAfter(element, anchor)
        }

        override fun checkAdd(element: PsiElement) {
            referenceExpression.checkAdd(element)
        }

        override fun addRange(first: PsiElement?, last: PsiElement?): PsiElement {
            return referenceExpression.addRange(first, last)
        }

        override fun addRangeBefore(first: PsiElement, last: PsiElement, anchor: PsiElement?): PsiElement {
            return referenceExpression.addRangeBefore(first, last, anchor)
        }

        override fun addRangeAfter(first: PsiElement?, last: PsiElement?, anchor: PsiElement?): PsiElement {
            return referenceExpression.addRangeAfter(first, last, anchor)
        }

        override fun delete() {

        }

        override fun checkDelete() {

        }

        override fun deleteChildRange(first: PsiElement?, last: PsiElement?) {

        }

        override fun replace(newElement: PsiElement): PsiElement {
            return referenceExpression.replace(newElement)
        }

        override fun isValid(): Boolean {
            return referenceExpression.isValid
        }

        override fun isWritable(): Boolean {
            return referenceExpression.isWritable
        }

        override fun getReferences(): Array<PsiReference> {
            return referenceExpression.references
        }

        override fun <T : Any?> getCopyableUserData(key: Key<T>): T? {
            return referenceExpression.getCopyableUserData(key)
        }

        override fun <T : Any?> putCopyableUserData(key: Key<T>, value: T?) {
            referenceExpression.putCopyableUserData(key, value)
        }

        override fun processDeclarations(
            processor: PsiScopeProcessor,
            state: ResolveState,
            lastParent: PsiElement?,
            place: PsiElement
        ): Boolean {
            return referenceExpression.processDeclarations(processor, state, lastParent, place)
        }

        override fun getContext(): PsiElement? {
            return referenceExpression.context
        }

        override fun isPhysical(): Boolean {
            return referenceExpression.isPhysical
        }

        override fun getResolveScope(): GlobalSearchScope {
            return referenceExpression.resolveScope
        }

        override fun getUseScope(): SearchScope {
            return referenceExpression.useScope
        }

        override fun getNode(): ASTNode {
            return referenceExpression.node
        }

        override fun isEquivalentTo(another: PsiElement?): Boolean {
            return referenceExpression.isEquivalentTo(another)
        }

        override fun getName(): String? {
            return referenceExpression.name
        }

        override fun getPresentation(): ItemPresentation? {
            return referenceExpression.presentation
        }

        override fun getPsiOrParent(): CjElement {
            return referenceExpression.psiOrParent
        }

        override fun getContainingCjFile(): CjFile {
            return referenceExpression.containingCjFile
        }

    }

    constructor(
        referenceExpression: CjSimpleNameExpression,
        descriptor: ClassDescriptor
    ) : this(EnumCallElement(referenceExpression), descriptor, null)


    override val classValueReceiver: EnumClassValueReceiver? = descriptor.classValueType?.let {
        EnumClassValueReceiver(this, it)
    }

    override val staticScope: MemberScope
        get() =
            if (descriptor.kind == ClassKind.ENUM_ENTRY) descriptor.staticScope
            else StaticMemberScope(
                ChainedMemberScope.create(
                    "Static scope for ${descriptor.name} as class or object",
                    descriptor.staticScope,
                    descriptor.unsubstitutedMemberScope
                )
            )


    override fun toString() = "Class{$descriptor}"
}

class ClassQualifier(
    override val referenceExpression: CjSimpleNameExpression,
    override val descriptor: ClassDescriptor,
    _cangjieType: CangJieType? = null
) : ClassifierQualifier {
    override val classValueReceiver: ClassValueReceiver? = _cangjieType?.let {
        ClassValueReceiver(this, it)

    } ?: descriptor.classValueType?.let {
        ClassValueReceiver(this, it)
    }

    override val staticScope: MemberScope
        get() =
            if (descriptor.kind == ClassKind.ENUM_ENTRY) descriptor.staticScope
            else StaticMemberScope(
                ChainedMemberScope.create(
                    "Static scope for ${descriptor.name} as class or object",
                    descriptor.staticScope,
                    descriptor.unsubstitutedMemberScope
                )
            )


    override fun toString() = "Class{$descriptor}"
}

class EnumClassValueReceiver @JvmOverloads constructor(
    val classQualifier: EnumClassQualifierByCall,
    override val type: CangJieType,
    original: EnumClassValueReceiver? = null
) : ExpressionReceiver {
    override val original = original ?: this


    override val expression: CjExpression
        get() = classQualifier.referenceExpression.calleeExpression!!

    override fun replaceType(newType: CangJieType) = EnumClassValueReceiver(classQualifier, newType, original)

}

class ClassValueReceiver @JvmOverloads constructor(
    val classQualifier: ClassifierQualifier,
    override val type: CangJieType,
    original: ClassValueReceiver? = null
) : ExpressionReceiver {
    override val original = original ?: this


    override val expression: CjExpression
        get() = classQualifier.expression

    override fun replaceType(newType: CangJieType) = ClassValueReceiver(classQualifier, newType, original)

}

class TypeParameterQualifier(
    override val referenceExpression: CjSimpleNameExpression,
    override val descriptor: TypeParameterDescriptor
) : Qualifier {
    override val classValueReceiver: ReceiverValue? get() = null
    override val staticScope: MemberScope get() = MemberScope.Empty

    override fun toString() = "TypeParameter{$descriptor}"
}

class TypeAliasQualifier(
    override val referenceExpression: CjSimpleNameExpression,
    override val descriptor: TypeAliasDescriptor,
    val classDescriptor: ClassDescriptor
) : ClassifierQualifier {
    override val classValueReceiver: ClassValueReceiver?
        get() = classDescriptor.classValueType?.let {
            ClassValueReceiver(this, it)
        }

    override val staticScope: MemberScope
        get() = when {
            DescriptorUtils.isEnum(classDescriptor) ->
                ChainedMemberScope.create(
                    "Static scope for typealias ${descriptor.name}",
                    classDescriptor.staticScope,
                    EnumEntriesScope()
                )

            else ->
                classDescriptor.staticScope
        }

    /**
     * We cannot use [org.cangnova.cangjie.descriptors.ClassDescriptor.getUnsubstitutedMemberScope] directly,
     * because we do not allow complete resolveName through type aliases yet .
     *
     * However, we want to allow to resolveName and autocomplete enum constants even through type aliases;
     * that's why we use [org.cangnova.cangjie.descriptors.ClassDescriptor.getUnsubstitutedMemberScope],
     * but filter only enum entries.
     */
    private inner class EnumEntriesScope : MemberScopeImpl() {
        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> =
            classDescriptor.unsubstitutedMemberScope
                .getContributedDescriptors(kindFilter, nameFilter)
                .filter { DescriptorUtils.isEnumEntry(it) }

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
            classDescriptor.unsubstitutedMemberScope
                .getContributedClassifier(name, location)
                ?.takeIf { DescriptorUtils.isEnumEntry(it) }

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, " {")
            p.pushIndent()
            p.println("descriptor = ", descriptor)
            p.popIndent()
            p.println("}")
        }
    }
}
