/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi

/**
 * 仓颉语言内置注解枚举
 *
 * 该枚举定义了所有仓颉语言的内置注解类型,对应C++编译器中的AnnotationKind枚举。
 * 内置注解用于提供FFI互操作、编译器指令、特性标记等功能。
 *
 * 注意:
 * - OVERFLOW 在早期版本的 glibc (VERSION < 2.27) 中是一个宏定义
 * - 为了与旧版本 glibc 兼容,使用 [NUMERIC_OVERFLOW] 代替 OVERFLOW
 * - 详见: https://man7.org/linux/man-pages/man3/matherr.3.html
 *
 * @property annotationName 注解在源代码中使用的名称(不含@符号)
 * @property description 注解的功能描述
 * @property category 注解所属的分类
 */
enum class CjBuiltInAnnotation(
    val annotationName: String,
    val description: String,
    val category: AnnotationCategory
) {
    /**
     * Java 互操作注解
     *
     * 用于标记与Java代码进行互操作的声明。
     */
    JAVA(
        annotationName = "Java",
        description = "Java 互操作标记",
        category = AnnotationCategory.FFI
    ),

    /**
     * 调用约定注解
     *
     * 用于指定函数的调用约定(如cdecl、stdcall等)。
     */
    CALLING_CONV(
        annotationName = "CallingConv",
        description = "调用约定指定",
        category = AnnotationCategory.FFI
    ),

    /**
     * C 互操作注解
     *
     * 用于标记与C代码进行互操作的声明。
     */
    C(
        annotationName = "C",
        description = "C 互操作标记",
        category = AnnotationCategory.FFI
    ),

    /**
     * Java 镜像类型注解
     *
     * 用于标记仓颉类型对应的Java镜像类型。
     */
    JAVA_MIRROR(
        annotationName = "JavaMirror",
        description = "Java 镜像类型",
        category = AnnotationCategory.FFI
    ),

    /**
     * Java 实现注解
     *
     * 用于标记通过Java实现的仓颉声明。
     */
    JAVA_IMPL(
        annotationName = "JavaImpl",
        description = "Java 实现标记",
        category = AnnotationCategory.FFI
    ),

    /**
     * Objective-C 镜像类型注解
     *
     * 用于标记仓颉类型对应的Objective-C镜像类型。
     */
    OBJ_C_MIRROR(
        annotationName = "ObjCMirror",
        description = "Objective-C 镜像类型",
        category = AnnotationCategory.FFI
    ),

    /**
     * Objective-C 实现注解
     *
     * 用于标记通过Objective-C实现的仓颉声明。
     */
    OBJ_C_IMPL(
        annotationName = "ObjCImpl",
        description = "Objective-C 实现标记",
        category = AnnotationCategory.FFI
    ),

    /**
     * 外部名称映射注解
     *
     * 用于指定仓颉声明对应的外部(C/Java等)名称。
     */
    FOREIGN_NAME(
        annotationName = "ForeignName",
        description = "外部名称映射",
        category = AnnotationCategory.FFI
    ),

    /**
     * 特性标记注解
     *
     * 用于标记特定的语言特性或编译器行为。
     */
    ATTRIBUTE(
        annotationName = "Attribute",
        description = "特性标记",
        category = AnnotationCategory.COMPILER_DIRECTIVE
    ),

    /**
     * 数值溢出行为控制注解 - 抛出异常模式
     *
     * 用于控制数值运算溢出时抛出异常。
     *
     * 注意: 避免使用 OVERFLOW 作为标识符以兼容旧版本 glibc。
     */
    OVERFLOW_THROWING(
        annotationName = "OverflowThrowing",
        description = "数值溢出抛出异常",
        category = AnnotationCategory.COMPILER_DIRECTIVE
    ),

    /**
     * 数值溢出行为控制注解 - 环绕模式
     *
     * 用于控制数值运算溢出时进行环绕处理。
     */
    OVERFLOW_WRAPPING(
        annotationName = "OverflowWrapping",
        description = "数值溢出环绕",
        category = AnnotationCategory.COMPILER_DIRECTIVE
    ),

    /**
     * 数值溢出行为控制注解 - 饱和模式
     *
     * 用于控制数值运算溢出时进行饱和处理(限制在最大/最小值)。
     */
    OVERFLOW_SATURATING(
        annotationName = "OverflowSaturating",
        description = "数值溢出饱和",
        category = AnnotationCategory.COMPILER_DIRECTIVE
    ),

    /**
     * 内部函数标记注解
     *
     * 用于标记编译器内建函数(intrinsic function)。
     */
    INTRINSIC(
        annotationName = "Intrinsic",
        description = "内部函数标记",
        category = AnnotationCategory.COMPILER_DIRECTIVE
    ),

    /**
     * 条件编译注解
     *
     * 用于条件编译,根据编译时条件决定是否包含某段代码。
     */
    WHEN(
        annotationName = "When",
        description = "条件编译",
        category = AnnotationCategory.COMPILER_DIRECTIVE
    ),

    /**
     * 快速本地方法注解
     *
     * 用于标记快速本地方法,提供性能优化提示。
     */
    FAST_NATIVE(
        annotationName = "FastNative",
        description = "快速本地方法",
        category = AnnotationCategory.COMPILER_DIRECTIVE
    ),

    /**
     * 注解类型标记
     *
     * 用于标记一个类是注解类型定义。
     */
    ANNOTATION(
        annotationName = "Annotation",
        description = "注解类型标记",
        category = AnnotationCategory.META
    ),

    /**
     * 常量安全标记注解
     *
     * 用于标记常量表达式的安全性。
     */
    CONST_SAFE(
        annotationName = "ConstSafe",
        description = "常量安全标记",
        category = AnnotationCategory.COMPILER_DIRECTIVE
    ),

    /**
     * 废弃标记注解
     *
     * 用于标记已废弃的声明,提示用户不应再使用。
     */
    DEPRECATED(
        annotationName = "Deprecated",
        description = "废弃标记",
        category = AnnotationCategory.SEMANTIC
    ),

    /**
     * 冻结标记注解
     *
     * 用于标记不可修改的声明或值。
     */
    FROZEN(
        annotationName = "Frozen",
        description = "冻结标记(不可修改)",
        category = AnnotationCategory.SEMANTIC
    ),

    /**
     * Mock 准备标记注解
     *
     * 用于确保类型准备好被模拟(用于测试)。
     */
    ENSURE_PREPARED_TO_MOCK(
        annotationName = "EnsurePreparedToMock",
        description = "确保准备好被模拟",
        category = AnnotationCategory.TESTING
    );

    companion object {
        /**
         * 所有内置注解名称的集合
         *
         * 用于快速检查某个注解名称是否为内置注解。
         */
        val ALL_NAMES: Set<String> = entries.map { it.annotationName }.toSet()

        /**
         * 根据注解名称查找对应的内置注解枚举值
         *
         * @param name 注解名称(不含@符号)
         * @return 对应的枚举值,如果不是内置注解则返回null
         */
        fun fromName(name: String): CjBuiltInAnnotation? {
            return entries.find { it.annotationName == name }
        }

        /**
         * 检查给定的注解名称是否为内置注解
         *
         * @param name 注解名称(不含@符号)
         * @return 如果是内置注解返回true,否则返回false
         */
        fun isBuiltIn(name: String): Boolean {
            return name in ALL_NAMES
        }

        /**
         * 根据分类获取内置注解列表
         *
         * @param category 注解分类
         * @return 该分类下的所有内置注解列表
         */
        fun byCategory(category: AnnotationCategory): List<CjBuiltInAnnotation> {
            return entries.filter { it.category == category }
        }
    }
}

/**
 * 注解分类枚举
 *
 * 将内置注解按功能分组。
 */
enum class AnnotationCategory(val description: String) {
    /**
     * FFI(外部函数接口)相关注解
     *
     * 用于与其他语言(C、Java、Objective-C等)进行互操作。
     */
    FFI("外部函数接口"),

    /**
     * 编译器指令注解
     *
     * 用于控制编译器行为或提供编译器优化提示。
     */
    COMPILER_DIRECTIVE("编译器指令"),

    /**
     * 语义标记注解
     *
     * 用于标记代码的语义特性(如废弃、冻结等)。
     */
    SEMANTIC("语义标记"),

    /**
     * 元注解
     *
     * 用于定义其他注解的注解。
     */
    META("元注解"),

    /**
     * 测试相关注解
     *
     * 用于测试框架和模拟功能。
     */
    TESTING("测试")
}

/**
 * 调用约定枚举
 *
 * 定义了仓颉语言支持的C函数调用约定类型。
 * 调用约定决定了函数参数如何传递、堆栈如何清理等底层细节。
 *
 * @property conventionName 调用约定在注解中使用的名称
 * @property description 调用约定的描述
 */
enum class CallingConvention(
    val conventionName: String,
    val description: String
) {
    /**
     * CDECL 调用约定
     *
     * clang C编译器在不同平台上默认使用的调用约定。
     * 调用方负责清理堆栈,支持可变参数函数。
     */
    CDECL(
        conventionName = "CDECL",
        description = "C语言默认调用约定"
    ),

    /**
     * STDCALL 调用约定
     *
     * Win32 API使用的调用约定。
     * 被调用方负责清理堆栈,不支持可变参数。
     */
    STDCALL(
        conventionName = "STDCALL",
        description = "Win32 API调用约定"
    );

    companion object {
        /**
         * 所有调用约定名称的集合
         */
        val ALL_CONVENTION_NAMES: Set<String> = entries.map { it.conventionName }.toSet()

        /**
         * 根据名称查找对应的调用约定
         *
         * @param name 调用约定名称
         * @return 对应的枚举值,如果不存在则返回null
         */
        fun fromName(name: String): CallingConvention? {
            return entries.find { it.conventionName == name }
        }

        /**
         * 检查给定的名称是否为有效的调用约定
         *
         * @param name 调用约定名称
         * @return 如果是有效的调用约定返回true,否则返回false
         */
        fun isValid(name: String): Boolean {
            return name in ALL_CONVENTION_NAMES
        }
    }
}

/**
 * 注解目标枚举
 *
 * 定义了自定义注解可以应用的目标位置,对应C++编译器中的AnnotationTargetT枚举。
 * 用于限制自定义注解的使用范围,确保注解只能应用于合法的语言元素上。
 *
 * 该枚举主要用于 @Annotation 元注解,以声明自定义注解的有效应用位置。
 *
 * @property targetName 目标类型的名称
 * @property description 目标类型的中文描述
 */
enum class CjAnnotationTarget(
    val targetName: String,
    val description: String
) {
    /**
     * 类型声明
     *
     * 可应用于类(class)、结构体(struct)、接口(interface)、枚举(enum)等类型定义。
     */
    TYPE("TYPE", "类型声明"),

    /**
     * 参数声明
     *
     * 可应用于函数参数或构造函数参数。
     */
    PARAMETER("PARAMETER", "参数声明"),

    /**
     * 初始化块
     *
     * 可应用于类或结构体的初始化块(init)。
     */
    INIT("INIT", "初始化块"),

    /**
     * 成员属性
     *
     * 可应用于类或结构体的成员属性(property)。
     */
    MEMBER_PROPERTY("MEMBER_PROPERTY", "成员属性"),

    /**
     * 成员函数
     *
     * 可应用于类或结构体的成员函数。
     */
    MEMBER_FUNCTION("MEMBER_FUNCTION", "成员函数"),

    /**
     * 成员变量
     *
     * 可应用于类或结构体的成员变量。
     */
    MEMBER_VARIABLE("MEMBER_VARIABLE", "成员变量"),

    /**
     * 枚举构造函数
     *
     * 可应用于枚举类型的构造函数。
     */
    ENUM_CONSTRUCTOR("ENUM_CONSTRUCTOR", "枚举构造函数"),

    /**
     * 全局函数
     *
     * 可应用于全局作用域的函数声明。
     */
    GLOBAL_FUNCTION("GLOBAL_FUNCTION", "全局函数"),

    /**
     * 全局变量
     *
     * 可应用于全局作用域的变量声明。
     */
    GLOBAL_VARIABLE("GLOBAL_VARIABLE", "全局变量"),

    /**
     * 扩展声明
     *
     * 可应用于类型扩展(extend)声明。
     */
    EXTEND("EXTEND", "扩展声明");

    companion object {
        /**
         * 所有目标类型名称的集合
         *
         * 用于快速检查某个目标名称是否有效。
         */
        val ALL_TARGET_NAMES: Set<String> = entries.map { it.targetName }.toSet()

        /**
         * 根据目标名称查找对应的枚举值
         *
         * @param name 目标类型名称
         * @return 对应的枚举值,如果不存在则返回null
         */
        fun fromName(name: String): CjAnnotationTarget? {
            return entries.find { it.targetName == name }
        }

        /**
         * 检查给定的目标名称是否有效
         *
         * @param name 目标类型名称
         * @return 如果是有效的目标类型返回true,否则返回false
         */
        fun isValidTarget(name: String): Boolean {
            return name in ALL_TARGET_NAMES
        }
    }
}
/**
 * 溢出策略枚举
 *
 * 定义了数值运算溢出时的处理策略,对应C++编译器中的OverflowStrategy枚举。
 *
 * @property strategyName 策略在注解中使用的名称
 * @property description 策略的描述
 */
enum class OverflowStrategy(
    val strategyName: String,
    val description: String
) {
    /**
     * 无策略
     */
    NA(
        strategyName = "no",
        description = "无溢出策略"
    ),

    /**
     * 检查策略
     *
     * 在运行时检查溢出。
     */
    CHECKED(
        strategyName = "checked",
        description = "检查溢出"
    ),

    /**
     * 环绕策略
     *
     * 溢出时进行环绕处理(wrap around)。
     */
    WRAPPING(
        strategyName = "wrapping",
        description = "溢出环绕"
    ),

    /**
     * 抛出异常策略
     *
     * 溢出时抛出异常。
     */
    THROWING(
        strategyName = "throwing",
        description = "溢出抛出异常"
    ),

    /**
     * 饱和策略
     *
     * 溢出时饱和处理,限制在最大/最小值。
     */
    SATURATING(
        strategyName = "saturating",
        description = "溢出饱和"
    );

    companion object {
        /**
         * 所有策略名称的集合
         */
        val ALL_STRATEGY_NAMES: Set<String> = entries.map { it.strategyName }.toSet()

        /**
         * 根据策略名称查找对应的枚举值
         *
         * @param name 策略名称(不区分大小写)
         * @return 对应的枚举值,如果不存在则返回null
         */
        fun fromName(name: String): OverflowStrategy? {
            val lowerName = name.lowercase()
            return entries.find { it.strategyName == lowerName }
        }

        /**
         * 检查给定的策略名称是否有效
         *
         * @param name 策略名称
         * @return 如果是有效的策略名称返回true,否则返回false
         */
        fun isValid(name: String): Boolean {
            return name.lowercase() in ALL_STRATEGY_NAMES
        }
    }
}
