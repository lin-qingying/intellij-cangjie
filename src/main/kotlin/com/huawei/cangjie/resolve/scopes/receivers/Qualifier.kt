package com.huawei.cangjie.resolve.scopes.receivers

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.ClassKind
import com.huawei.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjSimpleNameExpression
import com.huawei.cangjie.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import com.huawei.cangjie.resolve.descriptorUtil.classValueType
import com.huawei.cangjie.resolve.scopes.ChainedMemberScope
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.types.CangJieType

interface Qualifier : QualifierReceiver {
    val referenceExpression: CjSimpleNameExpression
}
interface ClassifierQualifier : Qualifier {
    override val descriptor: ClassifierDescriptorWithTypeParameters
}

val Qualifier.expression: CjExpression
    get() = referenceExpression.getTopmostParentQualifiedExpressionForSelector() ?: referenceExpression

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

