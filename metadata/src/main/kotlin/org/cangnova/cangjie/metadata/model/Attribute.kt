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

package org.cangnova.cangjie.metadata.model

/**
 * 节点属性：声明修饰符、无效状态等
 */
enum class Attribute {
    /**
     * 标记名义类型是否形成循环，或类型别名声明是否形成循环。
     * 用于检查名义类型和别名类型时。
     * 写入（W）: PreCheck。
     * 读取（R）: TypeManager, TypeChecker。
     */
    IN_REFERENCE_CYCLE,

    /**
     * 标记代码是否不可达。
     * 写入（W）: Collector (Sema), TypeCheckMatchExpr, Utils (Sema), LLVMCodeGen。
     * 读取（R）: AST2CHIR, LLVMCodeGen。
     * 考虑移除此属性，仅被读取一次。
     */
    UNREACHABLE,

    /**
     * 标记声明是否会被其他包隐式使用。
     * 写入（W）: Sema。
     * 读取（R）: ASTSerialization。
     */
    IMPLICIT_USED,

    /**
     * 标记声明是否是从其他包导入的。用于类型检查。
     * 写入（W）: HLIRCodeGen, ASTSerialization, ImportManager, GenericInstantiation。
     * 读取（R）: TypeChecker, ASTSerialization, FFISerialization, ADReverse, HLIRCodeGen, HLIR2LLVM。
     */
    IMPORTED,

    /**
     * 标记声明是否为全局的。用于 ADReverse, AST2GIR, HLIR, LLVM 等。
     * 写入（W）: ADReverse, CHIRRewrite, FFISerialization, Collector, TypeChecker。
     * 读取（R）: ADReverse, AST2CHIR, HLIRCodeGen, LLVMCodeGen, TypeChecker, CompilerInstance。
     */
    GLOBAL,

    /**
     * 标记变量声明是否已被初始化。
     * 写入（W）: ASTSerialization, ImportManager, CheckInitialization (TypeChecker)。
     * 读取（R）: CheckInitialization (TypeChecker)。
     */
    INITIALIZED,

    /**
     * 标记 AST 节点是否由编译器添加。
     * 写入（W）: Parser, PreCheck, TypeChecker, Desugar, Collector, GIR2AST, Clone, Create, MacroExpansion。
     * 读取（R）: ASTContext, ADReverse, PrintNode, TypeChecker。
     */
    COMPILER_ADD,

    /**
     * 标记 AST 节点是否为泛型声明。
     * 写入（W）: Parser, GenericInstantiation。
     * 读取（R）: AST2CHIR, Walker, HLIRCodeGen, LLVMCodeGen, Sema, TypeManager, Collector, GenericInstantiation, TypeChecker。
     */
    GENERIC,

    /**
     * 标记接口中的函数是否有默认实现，
     * 或变量声明是否有默认初始化器（例如：var a = 1 为 true，let a : Int64 为 false）。
     * 写入（W）: Parser。
     * 读取（R）: ASTSerialization, Sema, CHIR Rewrite。
     */
    DEFAULT,

    /**
     * 标记成员是否为静态（static）成员。
     * 写入（W）: PreCheck, TypeChecker。
     * 读取（R）: AST2CHIR, HLIRCodeGen, LLVMCodeGen, parser, Collector, PreCheck, TypeChecker。
     */
    STATIC,

    /**
     * 标记成员是否为公共（public）成员。
     * 写入（W）: PreCheck。
     * 读取（R）: PreCheck, TypeChecker。
     */
    PUBLIC,

    /**
     * 标记成员是否为私有（private）成员。
     * 写入（W）: PreCheck。
     * 读取（R）: LLVMCodeGen, CheckInitialization (Sema), PreCheck, TypeChecker, Utils (Sema)。
     */
    PRIVATE,

    /**
     * 标记成员是否为受保护（protected）成员。
     * 写入（W）: FFISerialization, PreCheck。
     * 读取（R）: PreCheck, TypeChecker。
     */
    PROTECTED,

    /**
     * 标记声明是否为外部（external）声明。
     * 写入（W）: ADReverse, Parser, Desugar, PreCheck, TypeChecker。
     * 读取（R）: ASTSerialization, TypeChecker, Types.cpp, AST2CHIR, ASTSerialization, Parser, PreCheck。
     */
    EXTERNAL,

    /**
     * 标记声明是否为内部（internal）声明。
     * 写入（W）: ADReverse, Parser, Desugar, PreCheck, TypeChecker。
     * 读取（R）: ASTSerialization, TypeChecker, Types.cpp, AST2CHIR, ASTSerialization, Parser, PreCheck。
     */
    INTERNAL,

    /**
     * 标记一个声明是否实际上重写了继承的声明（即使用户没有标注 '@override'）。
     * 写入（W）: TypeChecker。
     * 读取（R）: PreCheck, TypeChecker。
     * 问题：既然流程是 PreCheck --> TypeChecker，这种情况是如何发生的？
     */
    OVERRIDE,

    /**
     * 标记一个声明是否实际上重定义了继承的声明（即使用户没有标注 '@redef'）。
     * 写入（W）: TypeChecker。
     * 读取（R）: PreCheck, TypeChecker。
     * 问题：既然流程是 PreCheck --> TypeChecker，这种情况是如何发生的？
     */
    REDEF,

    /**
     * 标记函数是否为抽象（abstract）函数。
     * 写入（W）: Parser。
     * 读取（R）: ABSTRACT, AST2CHIR, CodeGenHLIR, FFTSerialization, PreCheck, TypeChecker。
     */
    ABSTRACT,

    /**
     * 标记声明是否为密封（sealed）声明。
     * 写入（W）: Parser。
     * 读取（R）: TypeChecker。
     */
    SEALED,

    /**
     * 标记一个声明是否实际上是开放（open）的（即使用户没有标注 '@open'）。
     * 写入（W）: FFISerialization, PreCheck。
     * 读取（R）: CHIRRewrite, TypeChecker。
     */
    OPEN,

    /**
     * 标记声明是否为操作符（operator）声明。
     * 写入（W）: TypeChecker。
     * 读取（R）: CodeGenLLVM, Parser, PreCheck, TypeChecker。
     */
    OPERATOR,

    /**
     * 标记声明是否为外部（foreign）声明。
     * 写入（W）: PreCheck。
     * 读取（R）: AST2CHIR, CodeGenHLIR, CodeGenJs, WellFormednessChecker, Parser, TypeChecker, PreCheck。
     */
    FOREIGN,

    /**
     * 标记声明是否为不安全（unsafe）声明。
     * 写入（W）: Parser, Desugar。
     * 读取（R）: Parser, CheckInitialization (Sema), Desugar, TypeCHecker。
     */
    UNSAFE,

    /**
     * 标记声明是否为可变（mutable）声明。
     * 写入（W）: PreCheck。
     * 读取（R）: AST2CHIR, CodeGenHLIR, TypeChecker。
     */
    MUT,

    /**
     * 检查节点是否有损坏的子节点。
     * 写入（W）: Parser。
     * 读取（R）: Parser。
     */
    HAS_BROKEN,

    /**
     * 标记声明是否在结构体（Struct）内定义。
     * 写入（W）: Parser, PreCheck, TypeChecker, Utils (Sema)。
     * 读取（R）: CheckInitialization (Sema), CodeGenHLIR, CodeGenLLVM, CHIRRewrite。
     */
    IN_STRUCT,

    /**
     * 标记声明是否在扩展（extend）内定义。
     * 写入（W）: PreCheck, TypeChecker。
     * 读取（R）: TypeChecker, AST2CHIR, CodeGenHLIR。
     */
    IN_EXTEND,

    /**
     * 标记声明是否在枚举（enum）内定义。
     * 写入（W）: PreCheck。
     * 读取（R）: Sema, AST2CHIR。
     */
    IN_ENUM,

    /**
     * 标记声明是否在接口或类（class-like）内定义。
     * 写入（W）: PreCheck, TypeChecker, Utils (Sema), ASTSerialization, Parser, Collector, Desugar。
     * 读取（R）: PreCheck, TypeChecker, CodeGenHLIR。
     */
    IN_CLASSLIKE,

    /**
     * 标记节点是否在宏体内。
     * 写入（W）: 无。
     * 读取（R）: 无。
     * 问题：此属性似乎未被使用。
     */
    IN_MACRO,

    /**
     * 标记构造函数是否为主构造函数（primary constructor）。
     * 写入（W）: Desugar.DesugarPrimaryCtorSetPrimaryFunc (Sema)。
     * 读取（R）: CheckInitialization (Sema), TypeChecker。
     */
    PRIMARY_CONSTRUCTOR,

    /**
     * 标记函数是否为构造函数。
     * 写入（W）: Parser, Collector, Desugar, Utils (Sema)。
     * 读取（R）: AllCodeGen, Parser, Desugar, PreCheck, TypeChecker, CheckInitialization (Sema), Expand, AST2CHIR。
     * 问题：相同的标识符在 Space.h 中定义并在 Space.cpp 中广泛使用。
     */
    CONSTRUCTOR,

    /**
     * 标记函数是否为枚举构造函数。
     * 写入（W）: Collector。
     * 读取（R）: AST2CHIR, PreCheck, TypeChecker, TypeManager, Space, LLVMCodeGen。
     * 问题：同种类的属性使用似乎很“随意”... 没有明确的规则和模式。
     */
    ENUM_CONSTRUCTOR,

    /**
     * 标记函数是否为终结器（finalizer）。
     * 写入（W）: Parser。
     * 读取（R）: Parser。
     */
    FINALIZER,

    /**
     * 标记源代码是否为克隆的。LSP 需要。
     * 写入（W）: Clone, MacroExpansion, Parser, Utils (Sema)。
     * 读取（R）: 无。
     */
    IS_CLONED_SOURCE_CODE,

    /**
     * 标记变量是否被 lambda 或函数捕获。
     * 写入（W）: TypeChecker。
     * 读取（R）: TypeChecker, CodeGenLLVM, CodeGenHLIR。
     */
    IS_CAPTURE,

    /**
     * 标记节点的目标是否在核心（core）中定义。
     * 写入（W）: Desugar。
     * 读取（R）: Desugar。
     */
    IN_CORE,

    /**
     * 标记一个类型是否需要被自动装箱（boxed）到一个具有统一内存表示的类类型中。
     * 写入（W）: Desugar。
     * 读取（R）: Desugar。
     */
    NEED_AUTO_BOX,

    /**
     * 标记节点是否从宏扩展而来。
     * 写入（W）: MacroExpansion。
     * 读取（R）: 无。
     */
    MACRO_EXPANDED_NODE,

    /**
     * 标记函数定义是否为宏定义。
     * 写入（W）: Parser。
     * 读取（R）: TypeChecker, Parser。
     */
    MACRO_FUNC,

    /**
     * 标记函数定义是否是从某个宏定义生成的包装函数。
     * 写入（W）: Parser。
     * 读取（R）: CodeGenHLIR, TypeCheckExpr。
     */
    MACRO_INVOKE_FUNC,

    /**
     * 标记函数定义体是否是从某个宏定义生成的包装函数体。
     * 写入（W）: Parser。
     * 读取（R）: TypeCheckExpr, CodeGenLLV, CodeGenHLIR。
     */
    MACRO_INVOKE_BODY,

    /**
     * 标记表达式是否出现在赋值表达式的左侧（左值）。
     * 写入（W）: Collector。
     * 读取（R）: TypeCHeckExpr, TypeCheckExtend。
     */
    LEFT_VALUE,

    /**
     * 标记节点是否为 C 调用（Native node kind, for @C etc.）。
     * 写入（W）: Parser。
     * 读取（R）: Parser, Sema, HLIRCodeGen。
     */
    C,

    /**
     * 标记函数或变量是否导入了定义体。
     * 写入（W）: Modules。
     * 读取（R）: CHIR。
     */
    SRC_IMPORTED,

    /**
     * 标记节点是否为 Java 应用调用（foreign call）。
     * 写入（W）: Parser, AST2CHIR。
     * 读取（R）: Parser, ImportManager, Sema, AST2CHIR, HLIRCodeGen。
     */
    JAVA_APP,

    /**
     * 标记节点是否为 Java 扩展调用。
     * 写入（W）: Parser, AST2CHIR。
     * 读取（R）: Parser, ImportManager, Sema, AST2CHIR, HLIRCodeGen。
     */
    JAVA_EXT,

    /**
     * 标记节点是否为标准调用（std call）。
     * 写入（W）: Parser。
     * 读取（R）: HLIRCodeGen。
     */
    STD_CALL,

    /**
     * 标记节点是否不能被名称修饰（mangle）。
     * 在以下情况下设置此属性：
     *   - 所有类型的 CFunc，包括外部函数（foreign func）、@C 函数和 CFunc lambda。
     *   - 基自动装箱类声明。
     *   - 对宏进行脱糖时创建的包装函数。
     * 写入（W）: Parser, Sema。
     * 读取（R）: Sema, Mangler。
     */
    NO_MANGLE,

    /**
     * 标记节点是否已经过初始化检查以避免无限循环。
     * 写入（W）: Sema。
     * 读取（R）: Sema。
     */
    INITIALIZATION_CHECKED,

    /**
     * 标记一个损坏的节点，无需进一步检查。
     * 写入（W）: Parser, Sema。
     * 读取（R）: Parser, Sema。
     */
    IS_BROKEN,

    /**
     * 标记此声明是否为实例化的泛型声明。
     * 写入（W）: GenericInstantiator。
     * 读取（R）: Sema, ImportManager, HLIRCodeGen, LLVMCodeGen。
     */
    GENERIC_INSTANTIATED,

    /**
     * 标记参数或实参是否有初始值。
     * 写入（W）: Sema。
     * 读取（R）: Sema, AST2CHIR, HLIRCodeGen, LLVMCodeGen。
     */
    HAS_INITIAL,

    /**
     * 标记外部变量在 cjo 中是否为 volatile。
     * 写入（W）: AUTOSDK。
     * 读取（R）: Sema。
     */
    IS_VOLATILE,

    /**
     * 标记节点是否使用溢出策略。
     * 写入（W）: Parser, Sema。
     * 读取（R）: Sema, HLIRCodeGen。
     */
    NUMERIC_OVERFLOW,

    /**
     * 标记此函数是否将被内联函数（intrinsic）替换。
     * 写入（W）: Parser, Sema。
     * 读取（R）: Sema, AST2CHIR, HLIRCodeGen, LLVMCodeGen。
     */
    INTRINSIC,

    /**
     * 标记节点是否由外部工具生成。
     * 写入（W）: cjogen, Parser。
     * 读取（R）: Modules, Sema, ImportManager, HLIRCodeGen, Macro。
     */
    TOOL_ADD,

    /**
     * 标记节点是否已被类型检查访问过。
     * 写入（W）: Sema, ImportManager (在导出和导入 AST 时清除此属性)。
     * 读取（R）: Sema。
     */
    IS_CHECK_VISITED,

    /**
     * 标记：1. 节点是一个包且将要进行增量编译。
     *       2. 节点是一个声明，已被增量缓存，不需要类型检查和 IR 生成。
     * 写入（W）: IncrementalCompilerInstance, ASTSerialization。
     * 读取（R）: GenericInstantiation。
     */
    INCRE_COMPILE,

    /**
     * 标记节点是否为具有副作用的脱糖调用表达式，并且在子节点中包含 'mapExpr'。
     * 写入（W）: Sema。
     * 读取（R）: AST2CHIR。
     */
    SIDE_EFFECT,

    /**
     * 标记节点是否为编译器隐式添加的附加节点。
     * 写入（W）: ImportManager, Desugar。
     * 读取（R）: ImportManager, CodeGen-Debug, CHIR。
     */
    IMPLICIT_ADD,

    /**
     * 标记函数是否是从 main 声明脱糖而来。
     * 写入（W）: Desugar。
     * 读取（R）: Sema, CodeGen。
     */
    MAIN_ENTRY,

    /**
     * 标记那些为模拟（mocking）目的而生成的声明或表达式。
     * 此类 AST 节点不会为后续的模拟准备（如用访问器替换调用）进行处理。
     * 写入（W）: MockSupportManager, MockManager。
     * 读取（R）: MockSupportManager, MockManager。
     */
    GENERATED_TO_MOCK,

    /**
     * 标记表达式是否为最外层的二元表达式。
     * 例如，在 `let _ = 1 + 2 * 3` 中，加法是最外层表达式。
     * 写入（W）: Sema。
     * 读取（R）: Sema。
     */
    IS_OUTERMOST,

    /**
     * 标记声明是否由 @Annotation 修饰，或者数组字面量是否为注解的集合。
     * 写入（W）: Sema。
     * 读取（R）: Sema。
     */
    IS_ANNOTATION,

    /**
     * 标记声明是否需要反射信息。
     * 写入（W）: Sema。
     * 读取（R）: Codegen。
     */
    NO_REFLECT_INFO,

    /**
     * 标记此类或包是否支持模拟（mocking）。
     * 如果使用特殊的编译模式来编译某个包，则应为此包的每个声明设置此标志。
     * 写入（W）: ASTLoader, MockSupportManager。
     * 读取（R）: ASTWriter, MockSupportManager, TestManager。
     */
    MOCK_SUPPORTED,

    /**
     * 标记此类是否仅为模拟目的而改为开放（open）。
     * 写入（W）: MockSupportManager。
     * 读取（R）: MockSupportManager。
     */
    OPEN_TO_MOCK,

    /**
     * 标记扩展体中的此函数或属性是否实现了继承接口中的抽象函数。
     * 写入（W）: StructInheritanceChecker。
     * 读取（R）: AnalyzeFunctionLinkage。
     */
    INTERFACE_IMPL,

    /**
     * 标记声明是否正在为测试中使用而编译。它启用了扩展的导出能力。
     * 写入（W）: DesugarAfterInstantiation。
     * 读取（R）: ImportManager。
     */
    FOR_TEST,

    /**
     * 标记此声明内部是否包含 createMock/createSpy 调用。
     * 写入（W）: TestManager。
     * 读取（R）: PartialInstantiation, TestManager。
     */
    CONTAINS_MOCK_CREATION_CALL,

    /**
     * 属性枚举结束标记。
     */
    AST_ATTR_END,
}