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

package org.cangnova.cangjie.resolve.calls.model

import org.cangnova.cangjie.descriptors.ClassAndEnumDescriptor
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.TypeAliasDescriptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.scopes.receivers.DetailedReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.QualifierReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.cangnova.cangjie.resolve.scopes.receivers.TransientReceiver
import org.cangnova.cangjie.types.UnwrappedType

/**
 * 接收者仓颉调用参数接口
 * 表示一个带有接收者的调用参数,例如对象方法调用时的对象本身
 */
interface ReceiverCangJieCallArgument : CangJieCallArgument {
    /** 接收者对象 */
    val receiver: DetailedReceiver
    /** 是否为安全调用(?.运算符) */
    val isSafeCall: Boolean
}

/**
 * 集合字面量仓颉调用参数接口
 * 表示可以延迟解析的集合字面量参数,如 [1, 2, 3]
 */
interface CollectionLiteralCangJieCallArgument : PostponableCangJieCallArgument

/**
 * 仓颉调用参数基础接口
 * 所有调用参数的根接口
 */
interface CangJieCallArgument {
    /** 是否为展开参数(如 *args) */
    val isSpread: Boolean
    /** 参数名称(用于具名参数,如 func(name = "value")) */
    val argumentName: Name?
}

/**
 * 可延迟解析的仓颉调用参数接口
 * 表示需要在类型推断过程中延迟解析的参数,如 lambda 表达式
 */
interface PostponableCangJieCallArgument : CangJieCallArgument, ResolutionAtom

/**
 * 简单仓颉调用参数接口
 * 表示既是普通参数又是接收者参数的简单参数类型
 */
interface SimpleCangJieCallArgument : CangJieCallArgument, ReceiverCangJieCallArgument {
    /** 带有智能类型转换信息的接收者值 */
    override val receiver: ReceiverValueWithSmartCastInfo
}

/**
 * 可调用引用仓颉调用参数接口
 * 表示函数引用或属性引用作为参数,如 ::functionName
 */
interface CallableReferenceCangJieCallArgument : PostponableCangJieCallArgument, CallableReferenceResolutionAtom {
    /** 引用类型不能使用展开运算符 */
    override val isSpread: Boolean
        get() = false

    /** 关联的调用对象 */
    override val call: CangJieCall
}

/**
 * 表达式仓颉调用参数接口
 * 表示作为参数的普通表达式
 */
interface ExpressionCangJieCallArgument : SimpleCangJieCallArgument, ResolutionAtom

/**
 * 左手边(LHS)解析结果的密封类
 * 用于表示表达式左侧的解析结果,如成员访问表达式 a.b 中的 a
 */
sealed class LHSResult {

    /**
     * 类型结果
     * 表示左侧是一个类型(如静态成员访问)
     * @param qualifier 限定符接收者,表示类型的限定符
     * @param resolvedType 解析后的类型
     */
    class Type(val qualifier: QualifierReceiver?, resolvedType: UnwrappedType) : LHSResult() {
        /** 未绑定的详细接收者,用于后续的成员解析 */
        val unboundDetailedReceiver: ReceiverValueWithSmartCastInfo

        init {
            // 如果存在限定符,验证其必须是类或枚举描述符,或类型别名描述符
            if (qualifier != null) {
                assert(qualifier.descriptor is ClassAndEnumDescriptor || qualifier.descriptor is TypeAliasDescriptor) {
                    "Should be ClassDescriptor: ${qualifier.descriptor}"
                }
            }

            // 创建一个临时接收者来包装解析后的类型
            val unboundReceiver = TransientReceiver(resolvedType)
            // 创建带有智能类型转换信息的接收者值(空的类型集合,稳定状态)
            unboundDetailedReceiver = ReceiverValueWithSmartCastInfo(unboundReceiver, emptySet(), isStable = true)
        }
    }

    /**
     * 表达式结果
     * 表示左侧是一个表达式(如实例成员访问)
     * @param lshCallArgument 左侧表达式的调用参数
     */
    class Expression(val lshCallArgument: SimpleCangJieCallArgument) : LHSResult()

    /**
     * 空结果
     * 表示没有左侧表达式(目前此情况被禁止)
     */
    object Empty : LHSResult()

    /**
     * 错误结果
     * 表示左侧解析出错
     */
    object Error : LHSResult()
}

/**
 * 简单类型参数接口
 * 表示显式指定的类型参数,如 List<String> 中的 String
 */
interface SimpleTypeArgument : TypeArgument {
    /** 具体的类型 */
    val type: UnwrappedType
}

/**
 * 函数表达式接口
 * 表示作为参数的函数字面量(fun 表达式)
 */
interface FunctionExpression : LambdaCangJieCallArgument {
    /** 参数类型数组,null 表示该参数类型未声明 */
    override val parametersTypes: Array<UnwrappedType?>

    /**
     * 接收者类型
     * null 表示该函数不能有接收者(即不是扩展函数)
     */
    val receiverType: UnwrappedType?

    /**
     * 返回类型
     * null 表示返回类型未声明
     * 对于 fun(){ ... } 形式,returnType == Unit
     */
    val returnType: UnwrappedType?
}

/**
 * 简单仓颉参数接口
 * 表示普通的调用参数,既可以作为参数也可以作为接收者
 */
interface SimpleCangJieArgument : CangJieCallArgument, ReceiverCangJieCallArgument {
    /** 带有智能类型转换信息的接收者值 */
    override val receiver: ReceiverValueWithSmartCastInfo
}

/**
 * 类型参数接口
 * 泛型类型参数的基础接口
 */
interface TypeArgument

/**
 * 类型参数占位符
 * 用作类型参数的桩或下划线类型参数(如 List<_>)
 */
object TypeArgumentPlaceholder : TypeArgument

/**
 * 子调用仓颉调用参数接口
 * 表示嵌套调用作为参数,需要先解析内部调用
 */
interface SubCangJieCallArgument : SimpleCangJieCallArgument, ResolutionAtom {
    /** 内部调用的部分解析结果 */
    val callResult: PartialCallResolutionResult
}

/**
 * 限定符接收者仓颉调用参数类
 * 表示使用限定符的接收者,如类名或包名
 * @param receiver 限定符接收者对象
 */
class QualifierReceiverCangJieCallArgument(override val receiver: QualifierReceiver) : ReceiverCangJieCallArgument {
    /** 限定符接收者不支持安全调用 */
    override val isSafeCall: Boolean
        get() = false // TODO: 添加警告

    override fun toString() = "$receiver"

    /** 限定符不能使用展开运算符 */
    override val isSpread get() = false

    /** 限定符没有参数名 */
    override val argumentName: Name? get() = null
}

/**
 * Lambda 仓颉调用参数接口
 * 表示 lambda 表达式作为参数,如 { x -> x + 1 }
 */
interface LambdaCangJieCallArgument : PostponableCangJieCallArgument {
    /** Lambda 不能使用展开运算符 */
    override val isSpread: Boolean
        get() = false

    /**
     * 是否有构建器推断注解
     * 构建器推断仅支持 lambda(在 `LambdaCangJieArgumentImpl` 中实现)
     * 不支持匿名函数
     */
    var hasBuilderInferenceAnnotation: Boolean
        get() = false
        set(_) {}

    /**
     * 构建器推断会话
     * 用于处理使用构建器模式时的类型推断
     */
    var builderInferenceSession: InferenceSession?
        get() = null
        set(_) {}

    /**
     * 参数类型数组
     * parametersTypes == null 表示没有声明参数
     * 数组中的 null 表示该类型未显式声明
     */
    val parametersTypes: Array<UnwrappedType?>?
}