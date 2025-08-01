/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.diagnostics

import cn.cangnova.cangjie.name.ClassId
import cn.cangnova.cangjie.name.IClassId

/**
 * 匹配缺失情况类
 * 
 * 表示在模式匹配中缺失的分支情况
 */
sealed class MatchMissingCase {
    /**
     * 分支条件文本
     * 
     * 用于显示缺失分支的条件文本
     */
    abstract val branchConditionText: String

    /**
     * 未知缺失情况
     * 
     * 表示无法确定具体缺失的情况
     */
    object Unknown : MatchMissingCase() {
        override fun toString(): String = "unknown"

        override val branchConditionText: String = "else"
    }

    /**
     * 条件类型是期望类型的缺失情况
     * 
     * 表示缺失的分支涉及期望类型的条件
     *
     * @param typeOfDeclaration 声明类型的描述
     */
    sealed class ConditionTypeIsExpect(val typeOfDeclaration: String) : MatchMissingCase() {
        /**
         * 密封类缺失情况
         */
        object SealedClass : ConditionTypeIsExpect("sealed class")
        
        /**
         * 密封接口缺失情况
         */
        object SealedInterface : ConditionTypeIsExpect("sealed interface")
        
        /**
         * 枚举缺失情况
         */
        object Enum : ConditionTypeIsExpect("enum")

        override val branchConditionText: String = "else"

        override fun toString(): String = "unknown"
    }

    /**
     * 空值缺失情况
     * 
     * 表示缺失对null值的处理分支
     */
    object NullIsMissing : MatchMissingCase() {
        override val branchConditionText: String = "null"
    }

    /**
     * 布尔值缺失情况
     * 
     * 表示缺失对布尔值的处理分支
     *
     * @param value 缺失的布尔值
     */
    sealed class BooleanIsMissing(val value: Boolean) : MatchMissingCase() {
        /**
         * true值缺失情况
         */
        object TrueIsMissing : BooleanIsMissing(true)
        
        /**
         * false值缺失情况
         */
        object FalseIsMissing : BooleanIsMissing(false)

        override val branchConditionText: String = value.toString()
    }

    /**
     * 类型检查缺失情况
     * 
     * 表示缺失对特定类型的检查分支
     *
     * @param classId 类标识符
     * @param isSingleton 是否为单例
     */
    class IsTypeCheckIsMissing(val classId: IClassId, val isSingleton: Boolean) : MatchMissingCase() {
        override val branchConditionText: String = run {
            val fqName = classId.asSingleFqName().toString()
            if (isSingleton) fqName else "is $fqName"
        }

        override fun toString(): String {
            val className = classId.shortClassName
            val name = if (className.isSpecial) className.asString() else className.identifier
            return if (isSingleton) name else "is $name"
        }
    }
    
    /**
     * 其他检查缺失情况
     * 
     * 表示缺失其他类型的检查分支
     */
    class OtherCheckIsMissing : MatchMissingCase() {
        override val branchConditionText: String
            get() = "Other"
    }

    /**
     * 枚举检查缺失情况
     * 
     * 表示缺失对枚举值的检查分支
     *
     * @param callableId 可调用标识符
     */
    class EnumCheckIsMissing(val callableId: CallableId) : MatchMissingCase() {
        override val branchConditionText: String = callableId.asSingleFqName().toString()

        override fun toString(): String {
            return callableId.callableName.identifier
        }
    }
    
    /**
     * 元组检查缺失情况
     * 
     * 表示缺失对元组的检查分支
     *
     * @param callableId 可调用标识符
     */
    class TupleCheckIsMissing(val callableId: CallableId) : MatchMissingCase() {
        override val branchConditionText: String = callableId.asSingleFqName().toString()

        override fun toString(): String {
            return callableId.callableName.identifier
        }
    }
    
    /**
     * 获取缺失情况的字符串表示
     * 
     * @return 缺失情况的字符串表示
     */
    override fun toString(): String {
        return branchConditionText
    }
}
