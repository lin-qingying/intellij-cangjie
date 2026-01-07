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
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.StaticMemberScope
import org.cangnova.cangjie.resolve.source.MemberScopeImpl
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.utils.Printer
import org.cangnova.cangjie.resolve.scopes.ChainedMemberScope

/**
 * 限定符（Qualifier）
 *
 * Qualifier 表示由简单名称引用组成的限定符，是 [QualifierReceiver] 的一种实现。
 * 典型的使用场景是包名、类名等静态限定符。
 *
 * @property referenceExpression 引用表达式
 *
 * @see QualifierReceiver
 * @see ClassifierQualifier
 */
interface Qualifier : QualifierReceiver {
    /** 简单名称引用表达式 */
    val referenceExpression: CjSimpleNameExpression
}

/**
 * 类分类符限定符
 *
 * ClassifierQualifier 表示一个类分类符（类、接口、枚举等）的限定符。
 * 所有类类型（包括普通类和枚举类）都使用此接口统一处理。
 *
 * @property descriptor 带类型参数的类分类符描述符
 *
 * @see Qualifier
 */
interface ClassifierQualifier : Qualifier {
    /** 带类型参数的类分类符描述符 */
    override val descriptor: ClassifierDescriptorWithTypeParameters
}

/**
 * 获取限定符接收器对应的表达式
 *
 * 返回最顶层的限定表达式或引用表达式本身。
 *
 * @throws IllegalStateException 如果限定符接收器不是 Qualifier
 */
val QualifierReceiver.expression: CjExpression
    get() {
        return when (this) {
            is Qualifier -> referenceExpression.getTopmostParentQualifiedExpressionForSelector() ?: referenceExpression
            else -> throw IllegalStateException("QualifierReceiver is not a Qualifier")
        }
    }


/**
 * 获取限定符对应的表达式
 *
 * 返回最顶层的限定表达式或引用表达式本身。
 */
val Qualifier.expression: CjExpression
    get() = referenceExpression.getTopmostParentQualifiedExpressionForSelector() ?: referenceExpression


/**
 * 包限定符
 *
 * PackageQualifier 表示一个包名限定符。
 *
 * @param referenceExpression 简单名称引用表达式
 * @param descriptor 包视图描述符
 *
 * @see Qualifier
 */
class PackageQualifier(
    override val referenceExpression: CjSimpleNameExpression,
    override val descriptor: PackageViewDescriptor
) : Qualifier {
    /** 包没有类值接收器，总是返回 null */
    override val classValueReceiver: ReceiverValue? get() = null

    /** 包的静态作用域，即包的成员作用域 */
    override val staticScope: MemberScope get() = descriptor.memberScope

    override fun toString() = "Package{$descriptor}"
}


/**
 * 类限定符
 *
 * ClassQualifier 表示一个类（包括普通类和枚举类）的限定符。
 * 对于所有类类型，都通过 staticScope 提供静态成员访问。
 * 枚举类的构造器通过静态作用域暴露。
 *
 * ## 仓颉语言语义
 *
 * 在仓颉语言中，通过类型名可以访问：
 * - 静态方法/属性
 * - 构造器
 * - 枚举构造器（对于枚举类型）
 *
 * 枚举类不提供 classValueReceiver，因为枚举不能作为值使用，
 * 只能通过其静态作用域访问枚举构造器（如 `Color.Red`）。
 *
 * @param referenceExpression 简单名称引用表达式
 * @param descriptor 类描述符
 * @param _cangjieType 类的类型（可选）
 *
 * @see ClassifierQualifier
 */
class ClassQualifier(
    override val referenceExpression: CjSimpleNameExpression,
    override val descriptor: ClassAndEnumDescriptor,
    _cangjieType: CangJieType? = null
) : ClassifierQualifier {

    /**
     * 类值接收器
     *
     * - 对于枚举类：返回 null（枚举不能作为值使用，只能访问其构造器）
     * - 对于普通类：如果有伴生对象或 class 类型值，返回对应接收器
     */
    override val classValueReceiver: ClassValueReceiver? =
        if (descriptor.kind == ClassKind.ENUM) {
            // 枚举类不提供 classValueReceiver
            // 枚举构造器通过 staticScope 访问
            null
        } else {
            _cangjieType?.let { ClassValueReceiver(this, it) }
        }

    /**
     * 静态作用域
     *
     * 包含所有可通过类型名访问的成员：
     * - 静态方法/属性
     * - 构造器
     * - 枚举构造器（对于枚举类型）
     */
    override val staticScope: MemberScope
        get() = StaticMemberScope(
            ChainedMemberScope.create(
                "Static scope for ${descriptor.name}",
                descriptor.staticScope,
                descriptor.unsubstitutedMemberScope
            )
        )

    override fun toString() = "Class{$descriptor}"
}

/**
 * 类值接收器
 *
 * ClassValueReceiver 表示类作为值使用时的接收器。
 * 这允许将类作为一个值传递或使用，例如访问类的伴生对象成员。
 *
 * @param classQualifier 类限定符
 * @param type 接收器的类型
 * @param original 原始的接收器值
 *
 * @see ExpressionReceiver
 * @see ClassifierQualifier
 */
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

/**
 * 类型参数限定符
 *
 * TypeParameterQualifier 表示一个类型参数的限定符。
 * 类型参数本身不能作为值使用，因此没有类值接收器和静态作用域。
 *
 * @param referenceExpression 简单名称引用表达式
 * @param descriptor 类型参数描述符
 *
 * @see Qualifier
 */
class TypeParameterQualifier(
    override val referenceExpression: CjSimpleNameExpression,
    override val descriptor: TypeParameterDescriptor
) : Qualifier {
    /** 类型参数没有类值接收器 */
    override val classValueReceiver: ReceiverValue? get() = null

    /** 类型参数没有静态作用域 */
    override val staticScope: MemberScope get() = MemberScope.Empty

    override fun toString() = "TypeParameter{$descriptor}"
}

/**
 * 类型别名限定符
 *
 * TypeAliasQualifier 表示一个类型别名的限定符。
 * 它代理到实际的类描述符来提供类值接收器和静态作用域。
 *
 * @param referenceExpression 简单名称引用表达式
 * @param descriptor 类型别名描述符
 * @param classDescriptor 实际的类描述符
 *
 * @see ClassifierQualifier
 */
class TypeAliasQualifier(
    override val referenceExpression: CjSimpleNameExpression,
    override val descriptor: TypeAliasDescriptor,
    val classDescriptor: ClassDescriptor
) : ClassifierQualifier {
    /**
     * 类值接收器
     *
     * 类型别名不提供 classValueReceiver，与 ClassQualifier 保持一致。
     * 枚举构造器通过 staticScope 访问。
     */
    override val classValueReceiver: ClassValueReceiver?
        get() = null

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
                .filter { DescriptorUtils.isEnumConstructor(it) }

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
            classDescriptor.unsubstitutedMemberScope
                .getContributedClassifier(name, location)
                ?.takeIf { DescriptorUtils.isEnumConstructor(it) }

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, " {")
            p.pushIndent()
            p.println("descriptor = ", descriptor)
            p.popIndent()
            p.println("}")
        }
    }
}
