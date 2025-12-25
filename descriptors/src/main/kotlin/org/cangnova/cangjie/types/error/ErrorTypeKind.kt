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

package org.cangnova.cangjie.types.error

/**
 * 错误类型种类枚举
 *
 * 定义了仓颉类型系统中所有可能的错误类型种类。
 * 每种错误类型代表编译器在类型解析、类型推断或类型检查过程中遇到的特定问题。
 *
 * @param debugMessage 调试消息模板，用于生成错误提示信息（可包含格式化参数）
 * @param isUnresolved 是否为未解析类型（true 表示这是一个解析失败的类型）
 */
enum class ErrorTypeKind(val debugMessage: String, val isUnresolved: Boolean = false) {

    // ==================== 未解析类型 ====================

    /** 未解析的类型：通用的未解析类型错误 */
    UNRESOLVED_TYPE("Unresolved type for %s", true),

    /** 未解析的类型参数类型：泛型类型参数无法解析 */
    UNRESOLVED_TYPE_PARAMETER_TYPE("Unresolved type parameter type", true),

    /** 未解析的类类型：类名无法解析到具体的类 */
    UNRESOLVED_CLASS_TYPE("Unresolved class %s", true),

    /** 未解析的声明：声明引用无法解析 */
    UNRESOLVED_DECLARATION("Unresolved declaration %s", true),


    /** 未解析的类型别名：类型别名无法解析到实际类型 */
    UNRESOLVED_TYPE_ALIAS("Unresolved type alias %s"),

    /** 函数描述符未找到：无法为函数找到对应的描述符 */
    NOT_FOUND_DESCRIPTOR_FOR_FUNCTION("Descriptor not found for function %s"),

    // ==================== 返回类型错误 ====================

    /** 返回类型错误：通用的返回类型解析失败 */
    RETURN_TYPE("Return type for %s cannot be resolved"),

    /** 函数返回类型错误：函数的返回类型无法解析 */
    RETURN_TYPE_FOR_FUNCTION("Return type for function cannot be resolved"),

    /** 变量返回类型错误：变量的类型无法解析 */
    RETURN_TYPE_FOR_VARIABLE("Return type for variable %s cannot be resolved"),

    /** 属性返回类型错误：属性的类型无法解析 */
    RETURN_TYPE_FOR_PROPERTY("Return type for property %s cannot be resolved"),

    /** 构造函数返回类型错误：构造函数的返回类型无法解析 */
    RETURN_TYPE_FOR_CONSTRUCTOR("Return type for constructor %s cannot be resolved"),

    /** 函数隐式返回类型错误：无法推断函数的隐式返回类型 */
    IMPLICIT_RETURN_TYPE_FOR_FUNCTION("Implicit return type for function %s cannot be resolved"),

    /** 属性隐式返回类型错误：无法推断属性的隐式返回类型 */
    IMPLICIT_RETURN_TYPE_FOR_PROPERTY("Implicit return type for property %s cannot be resolved"),

    /** 属性访问器隐式返回类型错误：无法推断 getter/setter 的隐式返回类型 */
    IMPLICIT_RETURN_TYPE_FOR_PROPERTY_ACCESSOR("Implicit return type for property accessor %s cannot be resolved"),

    /** 解构组件返回类型错误：解构声明中的 componentN() 函数返回类型错误 */
    ERROR_TYPE_FOR_DESTRUCTURING_COMPONENT("%s() return type"),

    // ==================== 递归或循环类型 ====================

    /** 递归类型：类型定义中存在递归引用 */
    RECURSIVE_TYPE("Recursive type"),

    /** 递归类型别名：类型别名的定义中存在递归引用 */
    RECURSIVE_TYPE_ALIAS("Recursive type alias %s"),

    /** 递归注解类型：注解的类型定义中存在递归引用 */
    RECURSIVE_ANNOTATION_TYPE("Recursive annotation's type"),

    /** 循环上界：类型参数的上界约束中存在循环依赖 */
    CYCLIC_UPPER_BOUNDS("Cyclic upper bounds"),

    /** 循环父类型：类的继承层次中存在循环依赖 */
    CYCLIC_SUPERTYPES("Cyclic supertypes"),

    // ==================== 类型解析和类型推断 ====================

    /** Lambda 上下文接收者类型推断失败：无法推断 lambda 的上下文接收者类型 */
    UNINFERRED_LAMBDA_CONTEXT_RECEIVER_TYPE("Cannot infer a lambda context receiver type"),

    /** Lambda 参数类型推断失败：无法推断 lambda 参数的类型 */
    UNINFERRED_LAMBDA_PARAMETER_TYPE("Cannot infer a lambda parameter type"),

    /** 类型变量推断失败：无法推断泛型类型变量的具体类型 */
    UNINFERRED_TYPE_VARIABLE("Cannot infer a type variable %s"),

    /** 解析错误类型：类型解析过程中出现错误 */
    RESOLUTION_ERROR_TYPE("Resolution error type (%s)"),

    /** 期望类型错误：期望的类型出现错误 */
    ERROR_EXPECTED_TYPE("Error expected type"),

    /** 数据流类型错误：数据流分析中的类型错误 */
    ERROR_DATA_FLOW_TYPE("Error type for data flow"),

    /** 裸类型重建失败：无法重建裸类型（bare type） */
    ERROR_WHILE_RECONSTRUCTING_BARE_TYPE("Failed to reconstruct type %s"),

    /** 类型替换失败：无法执行类型参数替换 */
    UNABLE_TO_SUBSTITUTE_TYPE("Unable to substitute type (%s)"),

    // ==================== 特殊内部错误类型 ====================

    /** DONT_CARE 类型：特殊的占位符类型，表示"不关心"的类型 */
    DONT_CARE("Special DONT_CARE type"),

    /** 桩类型：用于临时占位的桩类型 */
    STUB_TYPE("Stub type %s"),

    /** 函数占位符类型：函数类型的占位符 */
    FUNCTION_PLACEHOLDER_TYPE("Function placeholder type (arguments: %s)"),

    /** Result 类型桩：Result 类型的占位符 */
    TYPE_FOR_RESULT("Stubbed 'Result' type"),

    /** 编译器异常类型：编译器内部异常导致的错误类型 */
    TYPE_FOR_COMPILER_EXCEPTION("Error type for a compiler exception while analyzing %s"),

    // ==================== 不一致类型 ====================

    /** 错误的灵活类型：仓颉灵活类型出现错误 */
    ERROR_FLEXIBLE_TYPE("Error Cangjie flexible type with id %s. (%s..%s)"),

    /** 错误的原始类型：原始类型（raw type）出现错误 */
    ERROR_RAW_TYPE("Error raw type %s"),

    /** 类型参数不匹配：类型参数数量与类型实参数量不匹配 */
    TYPE_WITH_MISMATCHED_TYPE_ARGUMENTS_AND_PARAMETERS("Inconsistent type %s (parameters.size = %s, arguments.size = %s)"),

    /** 动态类型范围非法：动态类型的类型范围不合法 */
    ILLEGAL_TYPE_RANGE_FOR_DYNAMIC("Illegal type range for dynamic type %s..%s"),

    // ==================== 反序列化错误 ====================

    /** 无法加载反序列化的类型参数：从元数据中加载类型参数失败 */
    CANNOT_LOAD_DESERIALIZE_TYPE_PARAMETER("Unknown type parameter %s. Please try recompiling module containing \"%s\""),

    /** 按名称反序列化类型参数失败：通过名称查找类型参数失败 */
    CANNOT_LOAD_DESERIALIZE_TYPE_PARAMETER_BY_NAME("Couldn't deserialize type parameter %s in %s"),

    /** 不一致的挂起函数：元数据中的挂起函数类型不一致 */
    INCONSISTENT_SUSPEND_FUNCTION("Inconsistent suspend function type in metadata with constructor %s"),

    /** 意外的灵活类型 ID：灵活类型的 ID 不符合预期 */
    UNEXPECTED_FLEXIBLE_TYPE_ID("Unexpected id of a flexible type %s. (%s..%s)"),

    /** 未知类型：完全未知的类型 */
    UNKNOWN_TYPE("Unknown type"),

    // ==================== 未指定类型的桩 ====================

    /** 未指定类型：声明没有指定类型 */
    NO_TYPE_SPECIFIED("No type specified for %s"),

    /** 循环范围无类型：for 循环的范围表达式没有类型 */
    NO_TYPE_FOR_LOOP_RANGE("Loop range has no type"),

    /** 循环参数无类型：for 循环的迭代变量没有类型 */
    NO_TYPE_FOR_LOOP_PARAMETER("Loop parameter has no type"),

    /** 参数缺少类型：函数参数缺少类型声明 */
    MISSED_TYPE_FOR_PARAMETER("Missed a type for a value parameter %s"),

    /** 类型参数缺少类型实参：泛型类型缺少对应的类型实参 */
    MISSED_TYPE_ARGUMENT_FOR_TYPE_PARAMETER("Missed a type argument for a type parameter %s"),

    // ==================== 非法类型使用 ====================

    /** 解析错误参数：解析错误导致的参数类型错误 */
    PARSE_ERROR_ARGUMENT("Error type for parse error argument %s"),

    /** 调用中的星投影：星投影（*）直接作为调用的类型实参传递 */
    STAR_PROJECTION_IN_CALL("Error type for star projection directly passing as a call type argument"),

    /** 禁止的动态类型：在不允许的上下文中使用动态类型 */
    PROHIBITED_DYNAMIC_TYPE("Dynamic type in a not allowed context"),

    /** 注解上下文中的非注解类型：在注解位置使用了非注解类型 */
    NOT_ANNOTATION_TYPE_IN_ANNOTATION_CONTEXT("Not an annotation type %s in the annotation context"),

    /** inc/dec 返回 Unit：自增/自减操作符返回了 Unit 类型 */
    UNIT_RETURN_TYPE_FOR_INC_DEC("Unit type returned by inc or dec"),

    /** 不允许的返回：在不允许返回的位置使用了 return */
    RETURN_NOT_ALLOWED("Return not allowed"),

    // ==================== 插件相关 ====================

    /** 未解析的 Parcel 类型：Android Parcelable 相关的类型未解析 */
    UNRESOLVED_PARCEL_TYPE("Unresolved 'Parcel' type", true),

    /** Kapt 错误类型：Kotlin 注解处理工具（KAPT）相关的错误类型 */
    KAPT_ERROR_TYPE("Kapt error type"),

    /** 合成元素错误类型：编译器合成元素的错误类型 */
    SYNTHETIC_ELEMENT_ERROR_TYPE("Error type for synthetic element"),

    /** 轻量级类解析的临时错误类型：为轻量级类解析创建的临时错误类型 */
    AD_HOC_ERROR_TYPE_FOR_LIGHTER_CLASSES_RESOLVE("Error type in ad hoc resolve for lighter classes"),

    // ==================== 表达式相关类型 ====================

    /** 错误表达式类型：表达式的类型解析失败 */
    ERROR_EXPRESSION_TYPE("Error expression type"),

    /** 错误接收者类型：成员访问或调用的接收者类型错误 */
    ERROR_RECEIVER_TYPE("Error receiver type for %s"),

    /** 错误常量值：常量表达式的值错误 */
    ERROR_CONSTANT_VALUE("Error constant value %s"),

    /** 空可调用引用：可调用引用为空 */
    EMPTY_CALLABLE_REFERENCE("Empty callable reference"),

    /** 不支持的可调用引用类型：不支持的可调用引用类型 */
    UNSUPPORTED_CALLABLE_REFERENCE_TYPE("Unsupported callable reference type %s"),

    /** 委托类型错误：委托表达式的类型错误 */
    TYPE_FOR_DELEGATION("Error delegation type for %s"),

    // ==================== 声明相关类型 ====================

    /** 声明的类型不可用：声明的类型无法获取 */
    UNAVAILABLE_TYPE_FOR_DECLARATION("Type is unavailable for declaration %s"),

    /** 错误类型参数：类型参数本身是错误的 */
    ERROR_TYPE_PARAMETER("Error type parameter"),

    /** 错误类型投影：类型投影（如 out T、in T）是错误的 */
    ERROR_TYPE_PROJECTION("Error type projection"),

    /** 错误父类型：父类型（supertype）是错误的 */
    ERROR_SUPER_TYPE("Error super type"),

    /** 错误类型的父类型：错误类型的父类型 */
    SUPER_TYPE_FOR_ERROR_TYPE("Supertype of error type %s"),

    /** 错误属性类型：属性的类型是错误的 */
    ERROR_PROPERTY_TYPE("Error property type"),

    /** 错误变量类型：变量的类型是错误的 */
    ERROR_VARIABLE_TYPE("Error variable type"),

    /** 错误类：类本身是错误的 */
    ERROR_CLASS("Error class"),

    /** 错误类型构造器的类型：为错误的类型构造器创建的类型 */
    TYPE_FOR_ERROR_TYPE_CONSTRUCTOR("Type for error type constructor (%s)"),

    /** 错误类型的交集：多个错误类型的交集类型 */
    INTERSECTION_OF_ERROR_TYPES("Intersection of error types %s"),

    /** 无法计算擦除边界：无法计算类型参数的擦除上界 */
    CANNOT_COMPUTE_ERASED_BOUND("Cannot compute erased upper bound of a type parameter %s"),

    /** 无效类型：类型无效 */
    INVALID_TYPE("Invalid Type"),

    // ==================== 无法加载的类型 ====================

    /** 未找到无符号类型：无符号类型（UInt、ULong 等）未找到 */
    NOT_FOUND_UNSIGNED_TYPE("Unsigned type %s not found"),

    /** 错误枚举类型：找不到枚举条目对应的枚举类 */
    ERROR_ENUM_TYPE("Not found the corresponding enum class for given enum constructor %s.%s"),

    /** 未记录类型：没有为声明记录类型信息 */
    NO_RECORDED_TYPE("Not found recorded type for %s"),

    // ==================== 其他 ====================

    /** 多个较小公共父类型：存在多个较小的公共父类型（类型推断问题） */
    MULIT_SMALL_COMMON_SUPERTYPES("Error type for multiple small common supertype"),
    ;
}