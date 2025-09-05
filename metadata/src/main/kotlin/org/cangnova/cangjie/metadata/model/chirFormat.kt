package org.cangnova.cangjie.metadata.model//package org.cangnova.cjodome.model
//
///**
// * Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// * This source file is part of the Cangjie project, licensed under Apache-2.0
// * with Runtime Library Exception.
// *
// * See https://cangjie-lang.cn/pages/LICENSE for license information.
// *
// * 仓颉语言包格式定义文件
// * 定义了包中类型、表达式、声明等核心数据结构
// *
// * 本文件为Kotlin数据结构定义，映射CHIR包格式。
// */
//
////
//// 类型相关定义
////
//
///** 类型种类 */
//enum class CHIRTypeKind {
//    INVALID, INT8, INT16, INT32, INT64, INT_NATIVE, UINT8, UINT16, UINT32, UINT64, UINT_NATIVE, FLOAT16, FLOAT32, FLOAT64, RUNE, BOOLEAN, UNIT, NOTHING, VOID, TUPLE, CLOSURE, STRUCT, ENUM, FUNC, CLASS, RAWARRAY, VARRAY, C_POINTER, C_STRING, GENERIC, REFTYPE
//}
//
///** 源表达式类型 */
//enum class SourceExpr {
//    IF_EXPR, WHILE_EXPR, DO_WHILE_EXPR, MATCH_EXPR, IF_LET_OR_WHILE_LET, QUEST, BINARY, FOR_IN_EXPR, OTHER
//}
//
///** 类型表 */
//data class Type(
//    val kind: CHIRTypeKind, val typeID: UInt, val argTys: List<UInt> = emptyList(), val refDims: ULong = 0u
//)
//
////
//// 其他类型结构体可按需补充
////
//
///** 链接类型 */
//enum class Linkage {
//    WEAK_ODR, EXTERNAL, INTERNAL, LINKONCE_ODR, EXTERNAL_WEAK
//}
//
///** 位置信息 */
//data class Pos(
//    val line: ULong, val column: ULong
//)
//
///** 调试位置信息 */
//data class DebugLocation(
//    val filePath: String, val fileId: UInt, val beginPos: Pos, val endPos: Pos, val scope: List<Int>
//)
//
///** 跳过检查类型 */
//enum class SkipKind {
//    NO_SKIP, SKIP_DCE_WARNING, SKIP_CODEGEN
//}
//
///** 溢出策略 */
//enum class OverflowStrategy {
//    NA, CHECKED, WRAPPING, THROWING, SATURATING
//}
//
///** 内联类型 */
//enum class Inline {
//    ALWAYS, NEVER, DEFAULT
//}
//
///** 注解联合体（Kotlin用sealed class模拟） */
//sealed class Annotation {
//    data class NeedCheckArrayBound(val need: Boolean) : Annotation()
//    data class NeedCheckCast(val need: Boolean) : Annotation()
//    data class DebugLocationInfo(val info: DebugLocation) : Annotation()
//    data class DebugLocationInfoForWarning(val info: DebugLocation) : Annotation()
//    data class LinkTypeInfo(val linkage: Linkage) : Annotation()
//    data class SkipCheck(val skipKind: SkipKind) : Annotation()
//    data class InlineInfo(val kind: Inline) : Annotation()
//    data class NeverOverflowInfo(val neverOverflow: Boolean) : Annotation()
//}
//
///** 基础表 */
//data class Base(
//    val annos: List<Annotation> = emptyList()
//)
//
///** 值种类 */
//enum class ValueKind {
//    LITERAL, GLOBALVAR, PARAMETER, IMPORTED_FUNC, IMPORTED_VAR, LOCALVAR, FUNC, BLOCK, BLOCK_GROUP
//}
//
///** 值表 */
//data class Value(
//    val base: Base,
//    val type: UInt,
//    val identifier: String,
//    val kind: ValueKind,
//    val valueID: UInt,
//    val users: List<UInt> = emptyList(),
//    val attributes: ULong = 0u
//)
//
///** 常量值种类 */
//enum class ConstantValueKind {
//    BOOL, RUNE, INT, FLOAT, STRING, UNIT, NULL, FUNC
//}
//
///** 字面量值表 */
//data class LiteralValue(
//    val base: Value, val literalKind: ConstantValueKind
//)
//
///** 表达式种类 */
//enum class CHIRExprKind {
//    INVALID, GOTO, BRANCH, MULTIBRANCH, EXIT, APPLY_WITH_EXCEPTION, INVOKE_WITH_EXCEPTION, RAISE_EXCEPTION, INT_OP_WITH_EXCEPTION, SPAWN_WITH_EXCEPTION, TYPECAST_WITH_EXCEPTION, INTRINSIC_WITH_EXCEPTION, ALLOCATE_WITH_EXCEPTION, RAW_ARRAY_ALLOCATE_WITH_EXCEPTION, RAW_ARRAY_LITERAL_ALLOCATE_WITH_EXCEPTION, NEG, NOT, BITNOT, ADD, SUB, MUL, DIV, MOD, EXP, LSHIFT, RSHIFT, BITAND, BITOR, BITXOR, LT, GT, LE, GE, EQUAL, NOTEQUAL, AND, OR, ALLOCATE, LOAD, STORE, GET_ELEMENT_REF, IF, LOOP, FORIN_RANGE, FORIN_ITER, FORIN_CLOSED_RANGE, LAMBDA, CONSTANT, DEBUG, TUPLE, FIELD, APPLY, INVOKE, INVOKE_STATIC, INSTANCEOF, BOX, UNBOX, TYPECAST, GET_EXCEPTION, RAW_ARRAY_ALLOCATE, RAW_ARRAY_LITERAL_ALLOCATE, RAW_ARRAY_INIT_BY_VALUE, VARRAY, VARRAY_BUILDER, INTRINSIC, SPAWN
//}
//
///** 内置表达式种类（部分） */
//enum class IntrinsicKind {
//    NOT_INTRINSIC, NOT_IMPLEMENTED, ARRAY_INIT, SIZE_OF, ALIGN_OF
//    // ... 省略其余成员，按需补充
//}
//
///** 表达式表 */
//data class Expression(
//    val base: Base,
//    val kind: CHIRExprKind,
//    val expressionID: UInt,
//    val operands: List<UInt> = emptyList(),
//    val blockGroups: List<UInt> = emptyList(),
//    val parentBlock: UInt = 0u,
//    val resultLocalVar: UInt = 0u,
//    val resultTy: UInt = 0u
//)
//
///** 仓颉包表 */
//data class CHIRPackage(
//    val name: String,
//    val path: String,
//    val pkgAccessLevel: String,
//    val types: List<Type>,
//    val values: List<Value>,
//    val exprs: List<Expression>,
//    // defs, globalInitFunc 等可按需补充
//)
