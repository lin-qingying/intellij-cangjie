package com.huawei.cangjie.resolve.scopes.receivers

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjSimpleNameExpression
import com.huawei.cangjie.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.descriptorUtil.classValueType
import com.huawei.cangjie.resolve.scopes.ChainedMemberScope
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.resolve.source.MemberScopeImpl
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.utils.Printer

interface Qualifier : QualifierReceiver {
    val referenceExpression: CjSimpleNameExpression
}
interface ClassifierQualifier : Qualifier {
    override val descriptor: ClassifierDescriptorWithTypeParameters
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
class ClassQualifier(
    override val referenceExpression: CjSimpleNameExpression,
    override val descriptor: ClassDescriptor
) : ClassifierQualifier {
    override val classValueReceiver: ClassValueReceiver? = descriptor.classValueType?.let {
        ClassValueReceiver(this, it)
    }

    override val staticScope: MemberScope
        get() =
            if (descriptor.kind == ClassKind.ENUM_ENTRY) descriptor.staticScope
            else ChainedMemberScope.create(
                "Static scope for ${descriptor.name} as class or object",
                descriptor.staticScope,
                descriptor.unsubstitutedInnerClassesScope
            )


    override fun toString() = "Class{$descriptor}"
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
            DescriptorUtils.isEnum (classDescriptor) ->
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
