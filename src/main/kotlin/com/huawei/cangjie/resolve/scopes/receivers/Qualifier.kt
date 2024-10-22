package com.huawei.cangjie.resolve.scopes.receivers

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.lang.CangJieLanguage
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.calls.model.CangJieCall
import com.huawei.cangjie.resolve.descriptorUtil.classValueType
import com.huawei.cangjie.resolve.scopes.ChainedMemberScope
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.resolve.scopes.StaticMemberScope
import com.huawei.cangjie.resolve.source.MemberScopeImpl
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.utils.Printer
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

        override fun <D> acceptChildren(visitor: CjVisitor<Void, D>, data: D) {

        }

        override fun acceptChildren(visitor: PsiElementVisitor) {

        }

        override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
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
                    descriptor.unsubstitutedInnerClassesScope
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
                    descriptor.unsubstitutedInnerClassesScope
                )
            )


    override fun toString() = "Class{$descriptor}"
}

class EnumClassValueReceiver @JvmOverloads constructor(
    val classQualifier: EnumClassQualifierByCall,
    private val type: CangJieType,
    original: EnumClassValueReceiver? = null
) : ExpressionReceiver {
    private val original = original ?: this

    override fun getType() = type

    override val expression: CjExpression
        get() = classQualifier.referenceExpression.calleeExpression!!

    override fun replaceType(newType: CangJieType) = EnumClassValueReceiver(classQualifier, newType, original)

    override fun getOriginal() = original
}

class ClassValueReceiver @JvmOverloads constructor(
    val classQualifier: ClassifierQualifier,
    private val type: CangJieType,
    original: ClassValueReceiver? = null
) : ExpressionReceiver {
    private val original = original ?: this

    override fun getType() = type

    override val expression: CjExpression
        get() = classQualifier.expression

    override fun replaceType(newType: CangJieType) = ClassValueReceiver(classQualifier, newType, original)

    override fun getOriginal() = original
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
     * We cannot use [com.huawei.cangjie.descriptors.ClassDescriptor.getUnsubstitutedMemberScope] directly,
     * because we do not allow complete resolve through type aliases yet .
     *
     * However, we want to allow to resolve and autocomplete enum constants even through type aliases;
     * that's why we use [com.huawei.cangjie.descriptors.ClassDescriptor.getUnsubstitutedMemberScope],
     * but filter only enum entries.
     */
    private inner class EnumEntriesScope : MemberScopeImpl() {
        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> =
            classDescriptor.unsubstitutedInnerClassesScope
                .getContributedDescriptors(kindFilter, nameFilter)
                .filter { DescriptorUtils.isEnumEntry(it) }

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
            classDescriptor.unsubstitutedInnerClassesScope
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
