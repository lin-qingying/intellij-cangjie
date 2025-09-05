package org.cangnova.cangjie.metadata.model

import org.cangnova.cangjie.metadata.builtins.BuiltInsBinaryVersion
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.util.toName
import org.cangnova.cangjie.name.Name


data class Package(
    val cjcVersion: String,
    val cjoVersion: BuiltInsBinaryVersion,
    val fullPkgName: String,//完整包名
    val pkgDepInfo: String,
    val imports: List<String>,
    val files: List<String>,
    val fileImports: List<Imports>,
    val types: List<SemaTy>,
    val decls: List<Decl>,
    val exprs: List<Expr>,
    val values: List<CompositeValue>,
    val moduleName: String,   // 模块名称
    val kind: PackageKind = PackageKind.Normal, // 包类型
    val access: AccessLevel = AccessLevel.Public, // 访问级别


)

data class Imports(
    val importSpecs: List<ImportSpec>
)

/**
 * // 导入规范表
 * // 定义导入规范
 * table ImportSpec {
 *   begin:Position;     // 开始位置
 *   end:Position;       // 结束位置
 *   prefixPaths:[string]; // 前缀路径列表
 *   identifier:string;  // 标识符
 *   asIdentifier:string; // 别名
 *   reExport:AccessModifier = Private; // 重新导出
 * }
 */
data class ImportSpec(
    val begin: Position?,
    val end: Position?,
    val prefixPaths: List<String>,
    val identifier: String?,
    val asIdentifier: String?,
    val reExport: AccessModifier?
)

/**
 * // 位置成员的最大长度与 'struct Position' 的定义相同
struct Position {
file:uint32;        // 文件ID
pkgId:uint32;       // 文件所属包的索引。0表示当前包
line:int32;         // 行号
column:int32;       // 列号
ignore:bool;        // 是否忽略
}
 */
data class Position(
    val file: Int,
    val pkgId: Int,
    val line: Int,
    val column: Int,
    val ignore: Boolean
)

// 访问修饰符枚举
// 定义访问修饰符
enum class AccessModifier(val value: UByte) {
    Private(0u),    // 私有
    Internal(1u),   // 内部
    Protected(2u),  // 受保护
    Public(3u);     // 公共

    companion object {
        fun fromUByte(value: UByte): AccessModifier =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown AccessModifier value: $value")
    }
}

/**
 * // 兼容性：任何新的类型种类必须只添加到最后
 * // 类型种类枚举
 * // 定义类型种类
 * enum TypeKind : uint16 {
 *   Invalid,             // 无效
 *
 *   // 原始类型
 *   Unit,                // 单元
 *   // 整数
 *   Int8,
 *   Int16,
 *   Int32,
 *   Int64,
 *   IntNative,
 *
 *   // 无符号整数
 *   UInt8,
 *   UInt16,
 *   UInt32,
 *   UInt64,
 *   UIntNative,
 *   // 浮点数
 *   Float16,
 *   Float32,
 *   Float64,
 *
 *   Rune,                // 字符
 *   Nothing,
 *   Bool,                // 布尔值
 *
 *   // 复合类型
 *   Tuple,               // 元组
 *   Enum,                // 枚举
 *   Func,                // 函数
 *   Struct,              // 结构体
 *
 *   // 引用类型
 *   Array,               // 数组
 *   VArray,              // 可变数组
 *   CPointer,
 *   CString,
 *   Class,
 *   Interface,
 *
 *   Type,                // 类型别名
 *   Generic,             // 泛型类型
 * }
 */

// 类型种类枚举
enum class TypeKind(val value: UShort) {
    Invalid(0u),

    // 原始类型
    Unit(1u),

    // 整数
    Int8(2u),
    Int16(3u),
    Int32(4u),
    Int64(5u),
    IntNative(6u),

    // 无符号整数
    UInt8(7u),
    UInt16(8u),
    UInt32(9u),
    UInt64(10u),
    UIntNative(11u),

    // 浮点数
    Float16(12u),
    Float32(13u),
    Float64(14u),

    Rune(15u),
    Nothing(16u),
    Bool(17u),

    // 复合类型
    Tuple(18u),
    Enum(19u),
    Func(20u),
    Struct(21u),

    // 引用类型
    Array(22u),
    VArray(23u),
    CPointer(24u),
    CString(25u),
    Class(26u),
    Interface(27u),

    Type(28u),
    Generic(29u);

    companion object {
        fun fromUShort(value: UShort): TypeKind =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown TypeKind value: $value")
    }
}

/**
 * // 完整ID表
 * // 用于跨包引用声明
 * table FullId {
 *   pkgId:int32;        // 声明所属的包
 *   decl:string;        // 其他包中被引用节点的导出ID
 *   index:uint32;       // 同一包中被引用声明的索引
 * }
 */
data class FullId(
    val pkgId: Int,
    val decl: String,
    val index: Int
)

/**
 * // 函数类型信息表
 * // 定义函数类型信息
 * table FuncTyInfo {
 *   retType:uint32;      // 函数返回类型
 *   isC:bool;           // 是否是CFunc或@C函数类型
 *   hasVariableLenArg:bool; // 是否具有可变长度参数
 * }
 */
data class FuncTyInfo(
    val retType: Int,
    val isC: Boolean,
    val hasVariableLenArg: Boolean
)

/**
 * // 复合类型信息表
 * // 定义复合类型信息
 * table CompositeTyInfo {
 *   declPtr:FullId;     // 声明ID
 *   isThisTy:bool=false; // 是否是This类型
 * }
 */
data class CompositeTyInfo(
    val declPtr: FullId?,
    val isThisTy: Boolean = false
)

/**
 * // 泛型类型信息表
 * // 定义泛型类型信息
 * table GenericTyInfo {
 *   declPtr:FullId;     // 声明ID
 *   upperBounds:[uint32]; // 上界列表
 * }
 */
data class GenericTyInfo(
    val declPtr: FullId?,
    val upperBounds: List<Int>
)

/**
 * // 数组类型信息表
 * // 定义数组类型信息
 * table ArrayTyInfo {
 *   dimsOrSize:int64;   // 维度或大小
 * }
 */
data class ArrayTyInfo(
    val dimsOrSize: Long
)

/**
 * // 语义类型联合体
 * // 定义语义类型信息
 * union SemaTyInfo {
 *   FuncTyInfo,          // 函数类型信息
 *   CompositeTyInfo,     // 复合类型信息
 *   GenericTyInfo,      // 泛型类型信息
 *   ArrayTyInfo,        // 数组类型信息
 * }
 */
sealed class SemaTyInfo {
    data class Func(val info: FuncTyInfo) : SemaTyInfo()
    data class Composite(val info: CompositeTyInfo) : SemaTyInfo()
    data class Generic(val info: GenericTyInfo) : SemaTyInfo()
    data class Array(val info: ArrayTyInfo) : SemaTyInfo()
    object None : SemaTyInfo()
}

/**
 * // 语义类型表
 * // 定义语义类型
 * table SemaTy {
 *   kind:TypeKind=Unit; // 类型种类
 *   // 类型参数，也用于funcTy的paramTypes
 *   typeArgs:[uint32];
 *   info:SemaTyInfo;    // 语义类型信息
 * }
 */
data class SemaTy(
    val kind: TypeKind = TypeKind.Unit,
    val typeArgs: List<Int> = emptyList(),
    val info: SemaTyInfo = SemaTyInfo.None
)

/**
 * // 自动微分信息表
 * // 定义自动微分信息
 * table AutoDiffInfo {
 *   isDiff:bool;        // 是否需要微分
 *   isAdj:bool;         // 是否需要伴随
 *   primal:string;      // 原始函数
 *   excepts:[string];   // 排除的变量
 *   includes:[string];  // 包含的变量
 *   stage:uint64;       // 阶段
 * }
 */
data class AutoDiffInfo(
    val isDiff: Boolean,
    val isAdj: Boolean,
    val primal: String?,
    val excepts: List<String>,
    val includes: List<String>,
    val stage: ULong
)

/**
 * // 类信息表
 * // 定义类信息
 * table ClassInfo {
 *   inheritedTypes:[uint32]; // 继承的类型列表
 *   body:[uint32];           // 主体表达式索引列表
 *   adInfo:AutoDiffInfo;     // 自动微分信息
 *   isAnno:bool = false;     // 是否当前类是自定义注解
 *   annoTargets:uint8;       // 当前类是自定义注解时可用
 *   runtimeVisible:bool;     // 运行时可见
 *   annoTargets2:uint8;
 * }
 */
data class ClassInfo(
    val inheritedTypes: List<Int>,
    val body: List<Int>,
    val adInfo: AutoDiffInfo?,
    val isAnno: Boolean = false,
    val annoTargets: UByte,
    val runtimeVisible: Boolean,
    val annoTargets2: UByte
)

/**
 * // 接口信息表
 * // 定义接口信息
 * table InterfaceInfo {
 *   inheritedTypes:[uint32]; // 继承的类型列表
 *   body:[uint32];           // 主体表达式索引列表
 * }
 */
data class InterfaceInfo(
    val inheritedTypes: List<Int>,
    val body: List<Int>
)

/**
 * 结构体信息表
 * 定义结构体信息
 * table StructInfo {
 *   inheritedTypes:[uint32]; // 继承的类型列表
 *   body:[uint32];          // 主体表达式索引列表
 *   adInfo:AutoDiffInfo;   // 自动微分信息
 * }
 */
data class StructInfo(
    val inheritedTypes: List<Int>,
    val body: List<Int>,
    val adInfo: AutoDiffInfo
)

/**
 * 枚举信息表
 * 定义枚举信息
 * table EnumInfo {
 *   inheritedTypes:[uint32]; // 继承的类型列表
 *   body:[uint32];          // 主体表达式索引列表
 *   adInfo:AutoDiffInfo;   // 自动微分信息
 *   hasArguments:bool;     // 是否有参数
 *   nonExhaustive:bool;   // 是否非穷尽
 *   ellipsisPos:Position; // 省略号位置
 * }
 */
data class EnumInfo(
    val inheritedTypes: List<Int>,
    val body: List<Int>,
    val adInfo: AutoDiffInfo,
    val hasArguments: Boolean,
    val nonExhaustive: Boolean,
    val ellipsisPos: Position
)

/**
 * 扩展信息表
 * 定义扩展信息
// * table ExtendInfo {
 *   inheritedTypes:[uint32]; // 继承的类型列表
 *   body:[uint32];          // 主体表达式索引列表
 * }
 */
data class ExtendInfo(
    val inheritedTypes: List<Int>,
    val body: List<Int>
)

/**
 * 变量信息表
 * 定义变量信息
 * table VarInfo {
 *   isVar:bool;           // 是否是变量
 *   isConst:bool;         // 是否是常量
 *   isMemberParam:bool;  // 是否是成员参数
 *   initializer:uint32;   // 表达式索引
 *   value:ConstValue;     // 如果有值则是常量
 * }
 */
data class VarInfo(
    val isVar: Boolean,
    val isConst: Boolean,
    val isMemberParam: Boolean,
    val initializer: Int,
    val value: ConstValue?
)

/**
 * 常量值联合体
 * 定义所有可能的常量值类型
 * union ConstValue {
 *   Int8Value,          // 8位有符号整数
 *   UInt8Value,         // 8位无符号整数
 *   Int16Value,         // 16位有符号整数
 *   UInt16Value,        // 16位无符号整数
 *   Int32Value,         // 32位有符号整数
 *   UInt32Value,        // 32位无符号整数
 *   Int64Value,         // 64位有符号整数
 *   UInt64Value,        // 64位无符号整数
 *   Float32Value,       // 32位浮点数
 *   Float64Value,       // 64位浮点数
 *   ArrayValue,         // 数组值
 *   StringValue:string, // 字符串值
 *   CompositeValue:CompositeValueIndex, // 复合值索引
 * }
 */
sealed class ConstValue {
    data object None : ConstValue() // 无效常量值
    data class Int8Value(val value: Byte) : ConstValue()
    data class UInt8Value(val value: UByte) : ConstValue()
    data class Int16Value(val value: Short) : ConstValue()
    data class UInt16Value(val value: UShort) : ConstValue()
    data class Int32Value(val value: Int) : ConstValue()
    data class UInt32Value(val value: UInt) : ConstValue()
    data class Int64Value(val value: Long) : ConstValue()
    data class UInt64Value(val value: ULong) : ConstValue()
    data class Float32Value(val value: Float) : ConstValue()
    data class Float64Value(val value: Double) : ConstValue()
    data class ArrayValue(val value: List<ConstValue>) : ConstValue()
    data class StringValue(val value: String) : ConstValue()
    data class CompositeValue(val value: CompositeValueIndex) : ConstValue() // CompositeValueIndex
}

/**
 * 8位有符号整数值结构体
 * struct Int8Value {
 *   val:int8;           // 值
 * }
 *
 * 8位无符号整数值结构体
 * struct UInt8Value {
 *   val:uint8;          // 值
 * }
 *
 * 16位有符号整数值结构体
 * struct Int16Value {
 *   val:int16;          // 值
 * }
 *
 * 16位无符号整数值结构体
 * struct UInt16Value {
 *   val:uint16;         // 值
 * }
 *
 * 32位有符号整数值结构体
 * struct Int32Value {
 *   val:int32;          // 值
 * }
 *
 * 32位无符号整数值结构体
 * struct UInt32Value {
 *   val:uint32;         // 值
 * }
 *
 * 64位有符号整数值结构体
 * struct Int64Value {
 *   val:int64;          // 值
 * }
 *
 * 64位无符号整数值结构体
 * struct UInt64Value {
 *   val:uint64;         // 值
 * }
 *
 * 32位浮点数值结构体
 * struct Float32Value {
 *   val:float32;        // 值
 * }
 *
 * 64位浮点数值结构体
 * struct Float64Value {
 *   val:float64;        // 值
 * }
 */

/**
 * 数组值表
 * 定义数组常量
 * table ArrayValue {
 *   val:[ConstValue];   // 数组元素列表
 * }
 */
/**
 * 复合值索引结构体
 * 定义复合值的索引
 * struct CompositeValueIndex {
 *   idx:uint32;         // 索引
 * }
 */
data class CompositeValueIndex(
    val idx: UInt
)

/**
 * 声明种类枚举
 * 兼容性：任何新的声明种类必须只添加到最后
 * enum DeclKind : uint16 {
 *   InvalidDecl,        // 无效声明
 *   ClassDecl,          // 类声明
 *   InterfaceDecl,      // 接口声明
 *   FuncDecl,           // 函数声明
 *   PropDecl,           // 属性声明
 *   VarDecl,            // 变量声明
 *   VarWithPatternDecl, // 带模式的变量声明
 *   FuncParam,          // 函数参数
 *   StructDecl,         // 结构体声明
 *   EnumDecl,           // 枚举声明
 *   ExtendDecl,         // 扩展声明
 *   TypeAliasDecl,      // 类型别名声明
 *   GenericParamDecl,   // 泛型参数声明
 *   BuiltInDecl,        // 内置声明
 * }
 */
enum class DeclKind(val value: UShort) {
    InvalidDecl(0u),        // 无效声明
    ClassDecl(1u),          // 类声明
    InterfaceDecl(2u),      // 接口声明
    FuncDecl(3u),           // 函数声明
    PropDecl(4u),           // 属性声明
    VarDecl(5u),            // 变量声明
    VarWithPatternDecl(6u), // 带模式的变量声明
    FuncParam(7u),          // 函数参数
    StructDecl(8u),         // 结构体声明
    EnumDecl(9u),           // 枚举声明
    ExtendDecl(10u),        // 扩展声明
    TypeAliasDecl(11u),     // 类型别名声明
    GenericParamDecl(12u),  // 泛型参数声明
    BuiltInDecl(13u);       // 内置声明

    companion object {
        fun fromUShort(value: UShort): DeclKind =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown DeclKind value: $value")
    }
}


/**
 * 溢出策略枚举
 * 定义溢出策略
 * enum OverflowPolicy : uint8 {
 *   NA,                  // 不适用
 *   Checked,             // 检查
 *   Wrapping,            // 环绕
 *   Throwing,            // 抛出
 *   Saturating,          // 饱和
 * }
 */
enum class OverflowPolicy(val value: UByte) {
    NA(0u),         // 不适用
    Checked(1u),    // 检查
    Wrapping(2u),   // 环绕
    Throwing(3u),   // 抛出
    Saturating(4u); // 饱和

    companion object {
        fun fromUByte(value: UByte): OverflowPolicy =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown OverflowPolicy value: $value")
    }
}

/**
 * 运算符类型枚举
 * 定义运算符类型
 */
enum class OperatorKind(val value: UByte) {
    NA(0u),                 // 不适用
    Index(1u),              // []
    Call(2u),               // ()
    Not(3u),                // !
    Power(4u),              // **
    Multiply(5u),           // *
    Divide(6u),             // /
    Remainder(7u),          // %
    Add(8u),                // +
    Subtract(9u),           // -
    BitLeftShift(10u),      // <<
    BitRightShift(11u),     // >>
    LT(12u),                // <
    LE(13u),                // <=
    GT(14u),                // >
    GE(15u),                // >=
    Equal(16u),             // ==
    NotEqual(17u),          // !=
    BitAnd(18u),            // &
    BitXor(19u),            // ^
    BitOr(20u),             // |
    PostInc(21u),           // ++
    PostDec(22u),           // --
    Is(23u),                // is
    As(24u),                // as
    LogicAnd(25u),          // &&
    LogicOr(26u),           // ||
    Coalescing(27u),        // ??
    Pipeline(28u),          // |>
    Composition(29u),       // ~>
    Assign(30u),            // =
    PowerAssign(31u),       // **=
    MultiplyAssign(32u),    // *=
    DivideAssign(33u),      // /=
    RemainderAssign(34u),   // %=
    AddAssign(35u),         // +=
    SubtractAssign(36u),    // -=
    LeftShiftAssign(37u),   // <<=
    RightShiftAssign(38u),  // >>=
    BitAndAssign(39u),      // &=
    BitXorAssign(40u),      // ^=
    BitOrAssign(41u),       // |=
    LogicAndAssign(42u),    // &&=
    LogicOrAssign(43u);     // ||=

    companion object {
        fun fromUByte(value: UByte): OperatorKind =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown OperatorKind value: $value")
    }
}

/**
 * // 函数参数列表表
 * // 定义函数参数列表
 * table FuncParamList {
 *   params:[uint32];    // 参数列表
 *   desugars:[uint32];  // 脱糖列表
 * }
 */
data class FuncParamList(
    val params: List<Int>,
    val desugars: List<Int>
)

/**
 * // 函数体表
 * // 定义函数体信息
 * table FuncBody {
 *   paramLists:[FuncParamList]; // 参数列表
 *   retType:uint32;     // 返回类型
 *   body:uint32;        // 主体表达式索引
 *   always:bool;        // 是否总是执行
 *   captureKind:uint8;  // 捕获类型
 * }
 */
data class FuncBody(
    val paramLists: List<FuncParamList>,
    val retType: Int,
    val body: Int,
    val always: Boolean,
    val captureKind: UByte
){
    val params: List<Int> = paramLists.flatMap { it.params }
    val desugars: List<Int> = paramLists.flatMap { it.desugars }
}

/**
 * // 函数信息表
 * // 定义函数信息
 * table FuncInfo {
 *   funcBody:FuncBody;   // 函数体
 *   overflowPolicy:OverflowPolicy; // 溢出策略
 *   op:OperatorKind = NA; // 运算符类型
 *   adInfo:AutoDiffInfo; // 自动微分信息
 *   isConst:bool;        // 是否是常量
 *   isInline:bool;       // 是否是内联函数
 *   isFastNative:bool;   // 是否是快速本地函数
 * }
 */
data class FuncInfo(
    val funcBody: FuncBody,
    val overflowPolicy: OverflowPolicy,
    val op: OperatorKind = OperatorKind.NA,
    val adInfo: AutoDiffInfo,
    val isConst: Boolean,
    val isInline: Boolean,
    val isFastNative: Boolean
)

/**
 * // 参数信息表
 * // 定义参数信息
 * table ParamInfo {
 *   isNamedParam:bool;   // 是否是命名参数
 *   isMemberParam:bool;  // 是否是成员参数
 *   defaultVal:uint32;   // 表达式索引
 * }
 */
data class ParamInfo(
    val isNamedParam: Boolean,
    val isMemberParam: Boolean,
    val defaultVal: Int
)

/**
 * // 别名信息表
 * // 定义别名信息
 * table AliasInfo {
 *   aliasedTy:uint32;    // 语义类型索引
 * }
 */
data class AliasInfo(
    val aliasedTy: Int
)

/**
 * // 属性信息表
 * // 定义属性信息
 * table PropInfo {
 *   isConst:bool;        // 是否是常量
 *   isMutable:bool;      // 是否可变
 *   setters:[uint32];    // 设置器索引列表
 *   getters:[uint32];    // 获取器索引列表
 * }
 */
data class PropInfo(
    val isConst: Boolean,
    val isMutable: Boolean,
    val setters: List<UInt>,
    val getters: List<UInt>
)

/**
 * // 内置类型枚举
 * // 定义内置类型
 * enum BuiltInType : uint8 {
 *   Array,               // 数组
 *   VArray,              // 可变数组
 *   CPointer,            // C指针
 *   CString,             // C字符串
 *   CFunc,               // C函数
 * }
 */
enum class BuiltInType(val value: UByte) {
    Array(0u),      // 数组
    VArray(1u),     // 可变数组
    CPointer(2u),   // C指针
    CString(3u),    // C字符串
    CFunc(4u);      // C函数

    companion object {
        fun fromUByte(value: UByte): BuiltInType =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown BuiltInType value: $value")
    }
}

/**
 * // 内置信息表
 * // 定义内置信息
 * table BuiltInInfo {
 *   builtInType:BuiltInType; // 内置类型
 * }
 */
data class BuiltInInfo(
    val builtInType: BuiltInType
)

/**
 * // 模式类型枚举
 * // 定义模式类型
 * enum PatternKind:int8 {
 *   InvalidPattern,      // 无效模式
 *   ConstPattern,        // 常量模式
 *   WildcardPattern,     // 通配符模式
 *   VarPattern,          // 变量模式
 *   TuplePattern,        // 元组模式
 *   TypePattern,         // 类型模式
 *   EnumPattern,         // 枚举模式
 *   ExceptTypePattern,   // 异常类型模式
 * }
 */
enum class PatternKind(val value: Byte) {
    InvalidPattern(0),      // 无效模式
    ConstPattern(1),        // 常量模式
    WildcardPattern(2),     // 通配符模式
    VarPattern(3),          // 变量模式
    TuplePattern(4),        // 元组模式
    TypePattern(5),         // 类型模式
    EnumPattern(6),         // 枚举模式
    ExceptTypePattern(7);   // 异常类型模式

    companion object {
        fun fromByte(value: Byte): PatternKind =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown PatternKind value: $value")
    }
}

/**
 * // 模式表
 * // 定义模式
 * table Pattern {
 *   kind:PatternKind = InvalidPattern; // 模式类型
 *   begin:Position;     // 开始位置
 *   end:Position;       // 结束位置
 *   patterns:[Pattern]; // 模式列表
 *   types:[uint32];     // SemaTy索引
 *   exprs:[uint32];     // 表达式索引
 *   values:[ConstValue]; // 常量值
 *   matchBeforeRuntime:bool; // 运行时类型检查前匹配
 *   needRuntimeTypeCheck:bool; // 是否需要运行时类型检查
 * }
 */
data class Pattern(
    val kind: PatternKind = PatternKind.InvalidPattern,
    val begin: Position,
    val end: Position,
    val patterns: List<Pattern> = emptyList(),
    val types: List<Int> = emptyList(),
    val exprs: List<Int> = emptyList(),
    val values: List<ConstValue> = emptyList(),
    val matchBeforeRuntime: Boolean = false,
    val needRuntimeTypeCheck: Boolean = false
)

/**
 * // 带模式的变量信息表
 * // 定义带模式的变量信息
 * table VarWithPatternInfo {
 *   isVar:bool;           // 是否是变量
 *   isConst:bool;         // 是否是常量
 *   irrefutablePattern:Pattern; // 仅存在于表达式子节点中
 *   initializer:uint32;   // 表达式索引
 * }
 */
data class VarWithPatternInfo(
    val isVar: Boolean,
    val isConst: Boolean,
    val irrefutablePattern: Pattern,
    val initializer: Int
)

/**
 * // 声明信息联合体
 * // 定义声明信息
 * union DeclInfo {
 *   ClassInfo,           // 类信息
 *   InterfaceInfo,       // 接口信息
 *   StructInfo,          // 结构体信息
 *   EnumInfo,            // 枚举信息
 *   ExtendInfo,          // 扩展信息
 *   PropInfo,            // 属性信息
 *   VarInfo,             // 变量信息
 *   VarWithPatternInfo, // 带模式的变量信息
 *   ParamInfo,           // 参数信息
 *   FuncInfo,            // 函数信息
 *   BuiltInInfo,         // 内置信息
 *   AliasInfo,           // 别名信息
 * }
 */

// 声明信息联合体的密封类定义
sealed class DeclInfo {
    data class Class(val info: ClassInfo) : DeclInfo()
    data class Interface(val info: InterfaceInfo) : DeclInfo()
    data class Struct(val info: StructInfo) : DeclInfo()
    data class Enum(val info: EnumInfo) : DeclInfo()
    data class Extend(val info: ExtendInfo) : DeclInfo()
    data class Prop(val info: PropInfo) : DeclInfo()
    data class Var(val info: VarInfo) : DeclInfo()
    data class VarWithPattern(val info: VarWithPatternInfo) : DeclInfo()
    data class Param(val info: ParamInfo) : DeclInfo()
    data class Func(val info: FuncInfo) : DeclInfo()
    data class BuiltIn(val info: BuiltInInfo) : DeclInfo()
    data class Alias(val info: AliasInfo) : DeclInfo()
    data object None : DeclInfo() // 无效声明信息
}

/**
 * // 泛型表
 * // 定义泛型信息
 * table Generic{
 *   typeParameters:[uint32]; // 泛型参数声明列表
 *   constraints:[Constraint]; // 约束列表
 * }
 */
data class Generic(
    val typeParameters: List<Int>, // 泛型参数声明列表
    val constraints: List<Constraint> // 约束列表
)

/**
 * // 约束表
 * // 定义泛型约束
 * table Constraint {
 *   begin:Position;     // 开始位置
 *   end:Position;       // 结束位置
 *   type:uint32;        // 对应类型参数的类型
 *   uppers:[uint32];    // 上界列表
 * }
 */
data class Constraint(
    val begin: Position?,      // 开始位置
    val end: Position?,        // 结束位置
    val type: UInt,            // 对应类型参数的类型
    val uppers: List<Int>     // 上界列表
)


// 声明哈希结构体
// 用于声明的一致性检查
data class DeclHash(
    val instVar: ULong,   // 实例变量哈希
    val virt: ULong,      // 虚拟方法哈希
    val sig: ULong,       // 签名哈希
    val srcUse: ULong,    // 源代码使用哈希
    val bodyHash: ULong   // 主体哈希
)

/**
 * 注解类型枚举
 * 定义注解类型
 * enum AnnoKind : uint16 {
 *   Deprecated,          // 已废弃
 *   TestRegistration,    // 测试注册
 *   Frozen,              // 冻结
 *   Custom               // 自定义
 * }
 */
enum class AnnoKind(val value: UShort) {
    Deprecated(0u),          // 已废弃
    TestRegistration(1u),    // 测试注册
    Frozen(2u),              // 冻结
    Custom(3u);              // 自定义

    companion object {
        fun fromUShort(value: UShort): AnnoKind =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown AnnoKind value: $value")
    }
}

/**
 * 注解参数表
 * 定义注解参数
 * table AnnoArg {
 *   name:string;        // 名称
 *   expr:uint32;        // 表达式索引 (LitConstExpr)
 * }
 */
data class AnnoArg(
    val name: String,    // 名称
    val expr: UInt        // 表达式索引 (LitConstExpr)
)

/**
 * 注解表
 * 定义注解
 * table Anno { // 短名称 "Anno" 因为与 "PackageFormat.fbs" 中的 "Annotation" 冲突
 *   kind:AnnoKind;      // 注解类型
 *   identifier:string;  // 标识符
 *   args:[AnnoArg];     // 参数
 * }
 */
data class Anno(
    val kind: AnnoKind,         // 注解类型
    val identifier: String?,     // 标识符
    val args: List<AnnoArg>     // 参数
)


/**
 * 声明表
 * 定义声明
 */
data class Decl(
    val kind: DeclKind = DeclKind.InvalidDecl, // 声明类型
    val isTopLevel: Boolean = false,           // 是否是顶层声明
    val fullPkgName: String,                   // 完整包名
    val genericDecl: FullId?,                   // 泛型声明ID
    val generic: Generic?,                      // 泛型信息
    val begin: Position?,                       // 开始位置
    val end: Position?,                         // 结束位置
    val identifier: String,                    // 标识符

    val identifierPos: Position?,               // 标识符位置
    val attributes: List<ULong>,               // 属性
    val annotations: List<Anno>,               // 注解

    // 语义和代码生成信息
    val type: UInt,                             // 类型索引
    val mangledName: String?,                   // 代码生成后的名称
    val exportId: String?,                      // 导出ID

    // 用于AST差异的哈希
    val mangledBeforeSema: String?,
    val hash: DeclHash?,

    // 特定声明类型的详细信息
    val info: DeclInfo
) {

    val name: Name = identifier.toName()
}


/**
 * 表达式类型枚举
 * 定义表达式类型
 */
enum class ExprKind(val value: UShort) {
    InvalidExpr(0u),         // 无效表达式
    WildcardExpr(1u),        // 通配符表达式
    PrimitiveTypeExpr(2u),   // 原始类型表达式
    ReturnExpr(3u),          // 返回表达式
    JumpExpr(4u),            // 跳转表达式
    MemberAccess(5u),        // 成员访问
    RefExpr(6u),             // 引用表达式
    CallExpr(7u),            // 调用表达式
    UnaryExpr(8u),           // 一元表达式
    IncOrDecExpr(9u),        // 自增/自减表达式
    LitConstExpr(10u),       // 字面常量表达式
    BinaryExpr(11u),         // 二元表达式
    SubscriptExpr(12u),      // 下标表达式
    AssignExpr(13u),         // 赋值表达式
    ArrayExpr(14u),          // 数组表达式
    PointerExpr(15u),        // 指针表达式
    TypeConvExpr(16u),       // 类型转换表达式
    ThrowExpr(17u),          // 抛出表达式
    SpawnExpr(18u),          // 生成表达式
    ArrayLit(19u),           // 数组字面量
    TupleLit(20u),           // 元组字面量
    MatchExpr(21u),          // 匹配表达式
    LetPatternDestructor(22u),// 解构表达式
    IfExpr(23u),             // 条件表达式
    TryExpr(24u),            // 尝试表达式
    WhileExpr(25u),          // 循环表达式
    DoWhileExpr(26u),        // 循环表达式
    LambdaExpr(27u),         // Lambda表达式
    Block(28u),              // 块表达式
    MatchCase(29u),          // 匹配案例
    MatchCaseOther(30u),     // 匹配其他案例
    AdjointExpr(31u),        // 伴随表达式
    GradExpr(32u),           // 梯度表达式
    ValWithGradExpr(33u),    // 带梯度值表达式
    VJPExpr(34u),            // VJP表达式
    ForInExpr(35u),          // 循环表达式
    IfAvailableExpr(36u);    // 条件表达式

    companion object {
        fun fromUShort(value: UShort): ExprKind =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown ExprKind value: $value")
    }
}

/**
 * 字面常量类型枚举
 * 定义字面常量类型
 */
enum class LitConstKind(val value: UByte) {
    Integer(0u),    // 整数
    RuneByte(1u),   // 字符
    Float(2u),      // 浮点数
    Rune(3u),       // 字符串
    String(4u),     // 字符串
    JString(5u),    // JSON字符串
    Bool(6u),       // 布尔值
    Unit(7u);       // 单元值

    companion object {
        fun fromUByte(value: UByte): LitConstKind =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown LitConstKind value: $value")
    }
}

/**
 * 字符串类型枚举
 * 定义字符串类型
 */
enum class StringKind(val value: UByte) {
    Normal(0u),         // 普通字符串
    JString(1u),        // JSON字符串
    MultiLine(2u),      // 多行字符串
    MultiLineRaw(3u);   // 多行原始字符串

    companion object {
        fun fromUByte(value: UByte): StringKind =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown StringKind value: $value")
    }
}

/**
 * 调用类型枚举
 * 定义调用类型
 */
enum class CallKind(val value: UByte) {
    NA(0u),                     // 不适用
    CallDeclaredFunction(1u),   // 调用声明函数
    CallObjectCreation(2u),     // 调用对象创建
    CallStructCreation(3u),     // 调用结构体创建
    CallSuperFunction(4u),      // 调用超类函数
    CallVariadicFunction(5u),   // 调用可变参数函数
    CallFunctionPtr(6u),        // 调用函数指针
    CallAnnotation(7u),         // 调用注解
    CallBuiltinFunction(8u),    // 调用内置函数
    CallIntrinsicFunction(9u);  // 调用固有函数

    companion object {
        fun fromUByte(value: UByte): CallKind =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown CallKind value: $value")
    }
}

/**
 * 循环类型枚举
 * 定义循环类型
 */
enum class ForInKind(val value: UByte) {
    NA(0u),         // 不适用
    Range(1u),      // 范围
    String(2u),     // 字符串
    Iterator(3u);   // 迭代器

    companion object {
        fun fromUByte(value: UByte): ForInKind =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown ForInKind value: $value")
    }
}

/**
 * 表达式信息联合体的密封类定义
 */
sealed class ExprInfo {
    data class Call(val info: CallInfo) : ExprInfo()
    data class Unary(val info: UnaryInfo) : ExprInfo()
    data class Binary(val info: BinaryInfo) : ExprInfo()
    data class IncOrDec(val info: IncOrDecInfo) : ExprInfo()
    data class LitConst(val info: LitConstInfo) : ExprInfo()
    data class Reference(val info: ReferenceInfo) : ExprInfo()
    data class Lambda(val info: LambdaInfo) : ExprInfo()
    data class Assign(val info: AssignInfo) : ExprInfo()
    data class Array(val info: ArrayInfo) : ExprInfo()
    data class Jump(val info: JumpInfo) : ExprInfo()
    data class FuncArg(val info: FuncArgInfo) : ExprInfo()
    data class Subscript(val info: SubscriptInfo) : ExprInfo()
    data class Match(val info: MatchInfo) : ExprInfo()
    data class Block(val info: BlockInfo) : ExprInfo()
    data class Try(val info: TryInfo) : ExprInfo()
    data class LetPatternDestructor(val info: LetPatternDestructorInfo) : ExprInfo()
    data class ForIn(val info: ForInInfo) : ExprInfo()
    data class MatchCase(val info: MatchCaseInfo) : ExprInfo()
    data class Spawn(val info: SpawnInfo) : ExprInfo()

    data object None : ExprInfo()
}

data class CallInfo(
    val hasSideEffect: Boolean,
    val callKind: CallKind = CallKind.NA
)

data class UnaryInfo(
    val op: OperatorKind
)

data class BinaryInfo(
    val op: OperatorKind
)

data class IncOrDecInfo(
    val op: OperatorKind
)

data class LitConstInfo(
    val strValue: String,
    val constKind: LitConstKind,
    val strKind: StringKind
)

data class ReferenceInfo(
    val reference: String,
    val target: FullId,
    val instTys: List<UInt>,
    val matchedParentTy: UInt
)

data class LambdaInfo(
    val funcBody: FuncBody,
    val supportMock: Boolean
)

data class AssignInfo(
    val isCompound: Boolean,
    val op: OperatorKind
)

data class ArrayInfo(
    val initFunc: FullId,
    val isValueArray: Boolean
)

data class JumpInfo(
    val isBreak: Boolean
)

data class FuncArgInfo(
    val withInout: Boolean,
    val isDefaultVal: Boolean
)

data class SubscriptInfo(
    val isTupleAccess: Boolean
)

data class MatchInfo(
    val matchMode: Boolean
)

data class BlockInfo(
    val isExpr: List<Boolean>
)

data class TryInfo(
    val resources: List<FullId>,
    val patterns: List<Pattern>
)

data class LetPatternDestructorInfo(
    val patterns: List<Pattern>
)

data class ForInInfo(
    val pattern: Pattern,
    val forInKind: ForInKind = ForInKind.NA
)

data class MatchCaseInfo(
    val patterns: List<Pattern>
)

data class SpawnInfo(
    val future: FullId
)

/**
 * 表达式表中的一行，定义表达式的结构。
 */
data class Expr(
    val kind: ExprKind = ExprKind.InvalidExpr, // 表达式类型
    val begin: Position?,                       // 开始位置
    val end: Position?,                         // 结束位置
    val mapExpr: UInt,                         // 处理副作用的表达式索引
    val operands: List<UInt>,                  // 子表达式索引列表
    val type: UInt,                            // 类型索引
    val overflowPolicy: OverflowPolicy,        // 溢出策略
    val info: ExprInfo                         // 表达式信息
)

/**
 * 复合值表
 * 表示类、结构体、元组、枚举
 * table CompositeValue {
 *   type:uint32;        // 类型索引
 *   fields:[MemberValue]; // 成员列表
 * }
 */
data class CompositeValue(
    val type: UInt,
    val fields: List<MemberValue>
)

/**
 * 成员值表
 * 定义复合值的成员
 * table MemberValue {
 *   field:string;       // 字段名
 *   type:uint32;        // 类型索引
 *   value:ConstValue;   // 值
 * }
 */
data class MemberValue(
    val field: String,
    val type: UInt,
    val value: ConstValue?
)

/**
 * 包类型枚举
 * 定义包类型
 */
enum class PackageKind(val value: UByte) {
    Normal(0u),    // 普通包
    Macro(1u),     // 宏包
    Foreign(2u),   // 外部语言包
    Mock(3u);      // 带有模拟支持的包

    companion object {
        fun fromUByte(value: UByte): PackageKind =
            values().find { it.value == value }
                ?: throw IllegalArgumentException("Unknown PackageKind value: $value")
    }
}

/**
 * 访问级别枚举
 * 定义访问级别
 */
enum class AccessLevel(val value: UByte) {
    Public(0u),      // 公共
    Protected(1u),   // 受保护
    Internal(2u);    // 内部

    companion object {
        fun fromUByte(value: UByte): AccessLevel =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown AccessLevel value: $value")
    }
}
