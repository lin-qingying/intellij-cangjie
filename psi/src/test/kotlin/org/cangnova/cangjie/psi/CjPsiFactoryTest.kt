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

package org.cangnova.cangjie.psi


import org.cangnova.cangjie.CangJieTestBase
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

class CjPsiFactoryTest : CangJieTestBase() {

    private lateinit var factory: CjPsiFactory

    override fun setUp() {
        super.setUp()
        factory = CjPsiFactory(project)
    }

    fun `test emtry test`(){
        println(11)
    }

    fun `test create file`() {
        val file = """
            /*
             * @Copyright (c) Huawei Technologies Co., Ltd. 2022-2024. All rights reserved.
             */

            /**
             * @file
             * whatwg.org Automatic document generation
             */

            package charset4cj.charset.traditionchinese

            let Big5DecodeMapping: Array<UInt32> = [
                0x974a,
                0x9218,
                0x79d0,
                0x7a32,
                0x6660,
                0x6a29,
                0x889d,
                0x744c,
                0x7bc5,
                0x6782,
                0x7a2c,
                0x524f,
                0x9046,
                0x34e6,
                0x73c4,
                0x25db9,
                0x74c6,
                0x9fc7,
                0x57b3,
                0x492f,
                0x544c,
                0x4131,
                0x2368e,
                0x5818,
                0x7a72,
                0x27b65,
                0x8b8f,
                0x46ae,
                0x26e88,
                0x4181,
                0x25d99,
                0x7bae,
                0x224bc,
                0x9fc8,
                0x224c1,
                0x224c9,
                0x224cc,
                0x9fc9,
                0x8504,
                0x235bb,
                0x40b4,
                0x9fca,
                0x44e1
            ]

            let Big5EncodeMapping = HashMap<UInt32, UInt32>([
            ])


        """.trimIndent()

        val cjfile = factory.createFile(file)

    }

    fun `test create property`() {
        val property = factory.createProperty(
            """
            prop a:Int64 {
                get(){
                    return 1
                }
            }
        """.trimIndent()
        )

        check(property.name == "a")
    }

    fun `test create mutable property`() {
        val property = factory.createProperty(
            """
            mut prop b:Int {
                get(){ return 42 }
                set(value){ }
            }
        """.trimIndent()
        )

        check(property.name == "b")
    }

    fun `test create class`() {
        val clazz = factory.createClass(
            """
            class Test {
                prop x: Int {
                    get(){ return 0 }
                }
                
                func foo(): Int {
                    return x
                }
            }
        """.trimIndent()
        )

        check(clazz.name == "Test")
    }


    fun `test create basic function`() {
        val function = factory.createFunction(
            """
            func add(a: Int64, b: Int64): Int64 {
                return a + b
            }
        """.trimIndent()
        )

        check(function.name == "add")
        check(function.valueParameters.size == 2)
    }

    fun `test create function with named parameters`() {
        val function = factory.createFunction(
            """
            func greet(name!: String = "World"): String {
                return "Hello, " + name
            }
        """.trimIndent()
        )

        check(function.name == "greet")
        check(function.valueParameters.size == 1)
        val param = function.valueParameters[0]

        check(param.isNamed)
    }

    fun `test create function without return type`() {
        val function = factory.createFunction(
            """
            func add(a: Int64, b: Int64) {
                return a + b
            }
        """.trimIndent()
        )

        check(function.name == "add")
    }

    fun `test create function with local variables`() {
        val function = factory.createFunction(
            """
            func sum(numbers: Array<Int64>): Int64 {
                let result = 0
                for num in numbers {
                    result = result + num
                }
                return result
            }
        """.trimIndent()
        )

        check(function.name == "sum")
        check(function.hasBlockBody())
    }

    fun `test create whitespace`() {
        val space = factory.createWhiteSpace()
        check(space.text == " ")

        val newLine = factory.createNewLine()
        check(newLine.text == "\n ")

        val indent = factory.createIndent()
        check(indent.text == "    ")
    }

    // 测试创建表达式
    fun `test create expression`() {
        val expr = factory.createExpression("1 + 2")
        check(expr.text == "1 + 2")
    }

    // 测试创建类型引用
    fun `test create type reference`() {

        val typeRef = factory.createType("Int64")
        check(typeRef.text == "Int64")
    }

    // 测试创建参数列表
    fun `test create parameter list`() {
        val paramList = factory.createParameterList("(x: Int64, y: String)")
        check(paramList.parameters.size == 2)
    }

    // 测试创建代码块片段
    fun `test create code fragments`() {
        val blockFragment = factory.createBlockCodeFragment(
            """
            let x = 1
            let y = 2
            x + y
        """.trimIndent(), null
        )

        val exprFragment = factory.createExpressionCodeFragment("1 + 2", null)
    }

    // 测试创建注释
    fun `test create comments`() {
        val lineComment = factory.createComment("// This is a line comment")
        check(lineComment.text == "// This is a line comment")

        val blockComment = factory.createComment("/* This is a block comment */")
        check(blockComment.text == "/* This is a block comment */")
    }

    // 测试创建 lambda 表达式
    fun `test create lambda expression`() {
        val lambda = factory.createLambdaExpression("x: Int64", "x + 1")
        check(lambda.text == "{ x: Int64 -> x + 1 }")
    }

    // 测试创建修饰符列表
    fun `test create modifier list`() {
        val modList = factory.createModifierList("public")
        check(modList.hasModifier(CjTokens.PUBLIC_KEYWORD))
    }

    // 测试创建标识符
    fun `test create identifier`() {
        val id = factory.createIdentifier("testId")
        check(id?.text == "testId")
    }

    // 测试创建导入指令
    fun `test create import directive`() {
        // 测试基本导入
        val basicImport = ImportPath(FqName("test.package.Class"), isAllUnder = false)
        val importDirective = factory.createImportDirective(basicImport)
        check(importDirective.items.size == 1)
        check(importDirective.items[0].importedFqName?.asString() == "test.package.Class")

        // 测试全部导入
        val allImport = ImportPath(FqName("test.package"), isAllUnder = true)
        val allImportDirective = factory.createImportDirective(allImport)
        check(allImportDirective.items.size == 1)
        check(allImportDirective.items[0].isAllUnder)

        // 测试带别名的导入
        val aliasImport = ImportPath(
            fqName = FqName("test.package.Class"),
            isAllUnder = false,
            alias = Name.identifier("MyClass")
        )
        val aliasImportDirective = factory.createImportDirective(aliasImport)
        check(aliasImportDirective.items.size == 1)
        check(aliasImportDirective.items[0].aliasName == "MyClass")
        check(aliasImportDirective.items[0].isValidImport)

        // 测试非法导入（根路径）
        try {
            factory.createImportDirective(ImportPath(FqName.ROOT, false))
            error("Should throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    fun `test create void function`() {
        val function = factory.createFunction(
            """
            func printMessage(msg: String) {
                println(msg)
            }
        """.trimIndent()
        )

        check(function.name == "printMessage")
        check(!function.hasDeclaredReturnType())
    }


    fun `test create function with multiple parameters`() {
        val function = factory.createFunction(
            """
            func calculate(a: Int64, b!: Int64 = 0, c!: Int64 = 1): Int64 {
                return a + b * c
            }
        """.trimIndent()
        )

        check(function.name == "calculate")
        check(function.valueParameters.size == 3)

        function.valueParameters.getOrNull(0)?.let {
            check(!it.isNamed)
        }
        function.valueParameters.getOrNull(1)?.let {
            check(it.isNamed)
        }
        function.valueParameters.getOrNull(2)?.let {
            check(it.isNamed)
        }
    }

    // 测试创建基本结构体
    fun `test create basic struct`() {
        val struct = factory.createStruct(
            """
            struct Point {
                x: Int64
                y: Int64
            }
        """.trimIndent()
        )

        check(struct.name == "Point")
    }

    // 测试创建带方法的结构体
    fun `test create struct with methods`() {
        val struct = factory.createStruct(
            """
            struct Rectangle {
                width: Int64
                height: Int64
                
                func area(): Int64 {
                    return width * height
                }
                
                mut func scale(factor: Int64) {
                    width = width * factor
                    height = height * factor
                }
            }
        """.trimIndent()
        )

        check(struct.name == "Rectangle")
    }

    // 测试创建带构造函数的结构体
    fun `test create struct with constructor`() {
        val struct = factory.createStruct(
            """
            struct Circle {
                radius: Int64
                
                init(r: Int64) {
                    radius = r
                }
                
                func area(): Float64 {
                    return 3.14159 * radius * radius
                }
            }
        """.trimIndent()
        )

        check(struct.name == "Circle")
    }

    // 测试创建泛型结构体
    fun `test create generic struct`() {
        val struct = factory.createStruct(
            """
            struct Pair<T, U> {
                first: T
                second: U
                
                init(first: T, second: U) {
                    this.first = first
                    this.second = second
                }
                
                func swap(): Pair<U, T> {
                    return Pair(second, first)
                }
            }
        """.trimIndent()
        )

        check(struct.name == "Pair")
    }

    // 测试创建带可变字段的结构体
    fun `test create struct with mutable fields`() {
        val struct = factory.createStruct(
            """
            struct Counter {
                mut count: Int64
                
                init() {
                    count = 0
                }
                
                mut func increment() {
                    count = count + 1
                }
                
                func getValue(): Int64 {
                    return count
                }
            }
        """.trimIndent()
        )

        check(struct.name == "Counter")
    }

    // ==================== 内置注解测试 ====================

    // 测试创建简单的 @Deprecated 注解
    fun `test create Deprecated annotation`() {
        val annotation = factory.createAnnotations("@Deprecated")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "Deprecated")
        check(entry.valueArguments.isEmpty())
    }

    // 测试创建 @Frozen 注解
    fun `test create Frozen annotation`() {
        val annotation = factory.createAnnotations("@Frozen")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "Frozen")
    }

    // 测试创建 @ConstSafe 注解
    fun `test create ConstSafe annotation`() {
        val annotation = factory.createAnnotations("@ConstSafe")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "ConstSafe")
    }

    // 测试创建 @C 注解
    fun `test create C annotation for FFI`() {
        val annotation = factory.createAnnotations("@C")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "C")
    }

    // 测试创建 @Java 注解
    fun `test create Java annotation for FFI`() {
        val annotation = factory.createAnnotations("@Java")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "Java")
    }

    // 测试创建带参数的 @ForeignName 注解
    fun `test create ForeignName annotation with arguments`() {
        val annotation = factory.createAnnotations("@ForeignName[name: \"native_method\"]")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "ForeignName")
        check(entry.valueArguments.isNotEmpty())
    }

    // 测试创建 @CallingConv 注解
    fun `test create CallingConv annotation`() {
        val annotation = factory.createAnnotations("@CallingConv[CDECL]")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "CallingConv")
        check(entry.callingConvention != null)
    }

    // 测试创建 @OverflowThrowing 注解
    fun `test create OverflowThrowing annotation`() {
        val annotation = factory.createAnnotations("@OverflowThrowing")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "OverflowThrowing")
    }

    // 测试创建 @OverflowWrapping 注解
    fun `test create OverflowWrapping annotation`() {
        val annotation = factory.createAnnotations("@OverflowWrapping")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "OverflowWrapping")
    }

    // 测试创建 @OverflowSaturating 注解
    fun `test create OverflowSaturating annotation`() {
        val annotation = factory.createAnnotations("@OverflowSaturating")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "OverflowSaturating")
    }

    // 测试创建 @Intrinsic 注解
    fun `test create Intrinsic annotation`() {
        val annotation = factory.createAnnotations("@Intrinsic")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "Intrinsic")
    }

    // 测试创建 @When 条件编译注解
    fun `test create When annotation for conditional compilation`() {
        val annotation = factory.createAnnotations("@When[os == \"windows\"]")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "When")
        check(entry.whenCondition != null)
        check(entry.whenConditionExpression != null)
    }

    // 测试创建 @Attribute 注解
    fun `test create Attribute annotation`() {
        val annotation = factory.createAnnotations("@Attribute[name: \"inline\"]")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "Attribute")
    }

    // 测试创建 @FastNative 注解
    fun `test create FastNative annotation`() {
        val annotation = factory.createAnnotations("@FastNative")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "FastNative")
    }

    // 测试创建 @Annotation 元注解
    fun `test create Annotation meta-annotation`() {
        val annotation = factory.createAnnotations("@Annotation[target: [AnnotationTarget.TYPE]]")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "Annotation")
        check(entry.valueArguments.isNotEmpty())
    }

    // 测试创建 @EnsurePreparedToMock 注解
    fun `test create EnsurePreparedToMock annotation`() {
        val annotation = factory.createAnnotations("@EnsurePreparedToMock")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "EnsurePreparedToMock")
    }

    // 测试创建 @JavaMirror 注解
    fun `test create JavaMirror annotation`() {
        val annotation = factory.createAnnotations("@JavaMirror[name: \"java.lang.String\"]")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "JavaMirror")
        check(entry.valueArguments.isNotEmpty())
    }

    // 测试创建 @JavaImpl 注解
    fun `test create JavaImpl annotation`() {
        val annotation =
            factory.createAnnotations("@JavaImpl[className: \"com.example.Utils\", methodName: \"process\"]")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "JavaImpl")
        check(entry.valueArguments.size >= 1)
    }

    // 测试创建 @ObjCMirror 注解
    fun `test create ObjCMirror annotation`() {
        val annotation = factory.createAnnotations("@ObjCMirror[name: \"NSString\"]")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "ObjCMirror")
        check(entry.valueArguments.isNotEmpty())
    }

    // 测试创建 @ObjCImpl 注解
    fun `test create ObjCImpl annotation`() {
        val annotation = factory.createAnnotations("@ObjCImpl[className: \"MyClass\", methodName: \"init\"]")

        check(annotation.entries.isNotEmpty())
        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "ObjCImpl")
        check(entry.valueArguments.isNotEmpty())
    }

    // 测试创建多个注解
    fun `test create multiple annotations`() {
        val annotation = factory.createAnnotations(
            """
            @Deprecated
            @Frozen
            @ConstSafe
            """.trimIndent()
        )

        check(annotation.entries.size == 3)
        check(annotation.entries[0].shortName?.asString() == "Deprecated")
        check(annotation.entries[1].shortName?.asString() == "Frozen")
        check(annotation.entries[2].shortName?.asString() == "ConstSafe")
    }

    // 测试所有内置 FFI 注解
    fun `test create all FFI annotations`() {
        val ffiAnnotations = listOf("C", "Java", "JavaMirror", "JavaImpl", "ObjCMirror", "ObjCImpl", "CallingConv", "ForeignName")

        for (annotationName in ffiAnnotations) {
            val annotation = when (annotationName) {
                "CallingConv" -> factory.createAnnotations("@$annotationName[CDECL]")
                "ForeignName", "JavaMirror", "JavaImpl", "ObjCMirror", "ObjCImpl" ->
                    factory.createAnnotations("@$annotationName[name: \"test\"]")

                else -> factory.createAnnotations("@$annotationName")
            }

            check(annotation.entries.isNotEmpty())
            check(annotation.entries.first().shortName?.asString() == annotationName)
        }
    }

    // 测试所有溢出控制注解
    fun `test create all overflow control annotations`() {
        val overflowAnnotations = listOf("OverflowThrowing", "OverflowWrapping", "OverflowSaturating")

        for (annotationName in overflowAnnotations) {
            val annotation = factory.createAnnotations("@$annotationName")

            check(annotation.entries.isNotEmpty())
            check(annotation.entries.first().shortName?.asString() == annotationName)
        }
    }

    // 测试所有编译器指令注解
    fun `test create all compiler directive annotations`() {
        val compilerAnnotations = listOf("Intrinsic", "Attribute", "FastNative", "ConstSafe")

        for (annotationName in compilerAnnotations) {
            val annotation = if (annotationName == "Attribute") {
                factory.createAnnotations("@$annotationName[name: \"test\"]")
            } else {
                factory.createAnnotations("@$annotationName")
            }

            check(annotation.entries.isNotEmpty())
            check(annotation.entries.first().shortName?.asString() == annotationName)
        }
    }

    // 测试所有语义标记注解
    fun `test create all semantic annotations`() {
        val semanticAnnotations = listOf("Deprecated", "Frozen")

        for (annotationName in semanticAnnotations) {
            val annotation = factory.createAnnotations("@$annotationName")

            check(annotation.entries.isNotEmpty())
            check(annotation.entries.first().shortName?.asString() == annotationName)
        }
    }

    // 测试验证内置注解枚举的完整性
    fun `test built-in annotation enum completeness`() {
        // 验证 CjBuiltInAnnotation 枚举中定义的所有注解都能被创建
        val allBuiltInNames = CjBuiltInAnnotation.entries.map { it.annotationName }

        for (annotationName in allBuiltInNames) {
            try {
                val annotation = when (annotationName) {
                    "ForeignName", "JavaMirror", "JavaImpl", "ObjCMirror", "ObjCImpl" -> {
                        factory.createAnnotations("@$annotationName[name: \"test\"]")
                    }

                    "CallingConv" -> {
                        factory.createAnnotations("@$annotationName[CDECL]")

                    }

                    "Attribute" -> {
                        factory.createAnnotations("@$annotationName[\"test\"]")
                    }

                    "When" -> {
                        factory.createAnnotations("@$annotationName[DEBUG]")
                    }
                    else -> {
                        factory.createAnnotations("@$annotationName")
                    }
                }

                check(annotation.entries.isNotEmpty()) {
                    "Failed to create annotation: $annotationName"
                }
            } catch (e: Exception) {
                error("Failed to create built-in annotation: $annotationName - ${e.message}")
            }
        }
    }

    // 测试注解参数访问
    fun `test annotation argument access`() {
        val annotation = factory.createAnnotations("@ForeignName[name: \"test_function\"]")

        val entry = annotation.entries.first()
        check(entry.shortName?.asString() == "ForeignName")

        val arguments = entry.valueArguments
        check(arguments.isNotEmpty())

        // 验证可以访问参数
        val firstArg = arguments.first()
        check(firstArg != null)
    }

    // 测试在函数上使用注解
    fun `test annotation on function declaration`() {
        val function = factory.createFunction(
            """
            @Deprecated
            @C
            func nativeFunction(): Unit {}
            """.trimIndent()
        )

        check(function.name == "nativeFunction")
        check(function.annotationEntries.size == 2)
        check(function.annotationEntries[0].shortName?.asString() == "Deprecated")
        check(function.annotationEntries[1].shortName?.asString() == "C")
    }

    // 测试在类上使用注解
    fun `test annotation on class declaration`() {
        val clazz = factory.createClass(
            """
            @Frozen
            @JavaMirror[name: "java.lang.Object"]
            class AnnotatedClass {}
            """.trimIndent()
        )

        check(clazz.name == "AnnotatedClass")
        check(clazz.annotationEntries.size == 2)
        check(clazz.annotationEntries[0].shortName?.asString() == "Frozen")
        check(clazz.annotationEntries[1].shortName?.asString() == "JavaMirror")
    }

    // 测试在结构体上使用注解
    fun `test annotation on struct declaration`() {
        val struct = factory.createStruct(
            """
            @Frozen
            struct Point {
                x: Int64
                y: Int64
            }
            """.trimIndent()
        )

        check(struct.name == "Point")
        check(struct.annotationEntries.size == 1)
        check(struct.annotationEntries.first().shortName?.asString() == "Frozen")
    }

    // ========== 测试内置注解识别和属性访问 ==========

    // 测试 isBuiltInAnnotation 属性
    fun `test isBuiltInAnnotation property`() {
        // 测试内置注解
        val deprecatedAnnotation = factory.createAnnotations("@Deprecated")
        val deprecatedEntry = deprecatedAnnotation.entries.first()
        check(deprecatedEntry.isBuiltInAnnotation) { "Deprecated should be a built-in annotation" }

        val cAnnotation = factory.createAnnotations("@C")
        val cEntry = cAnnotation.entries.first()
        check(cEntry.isBuiltInAnnotation) { "C should be a built-in annotation" }

        val javaAnnotation = factory.createAnnotations("@Java")
        val javaEntry = javaAnnotation.entries.first()
        check(javaEntry.isBuiltInAnnotation) { "Java should be a built-in annotation" }

    }

    // 测试 builtInAnnotation 属性
    fun `test builtInAnnotation property`() {
        val deprecatedAnnotation = factory.createAnnotations("@Deprecated")
        val deprecatedEntry = deprecatedAnnotation.entries.first()
        check(deprecatedEntry.builtInAnnotation == CjBuiltInAnnotation.DEPRECATED) {
            "Should return DEPRECATED enum value"
        }

        val frozenAnnotation = factory.createAnnotations("@Frozen")
        val frozenEntry = frozenAnnotation.entries.first()
        check(frozenEntry.builtInAnnotation == CjBuiltInAnnotation.FROZEN) {
            "Should return FROZEN enum value"
        }


    }

    // 测试 callingConvention 属性
    fun `test callingConvention property`() {
        val annotation = factory.createAnnotations("@CallingConv[CDECL]")
        val entry = annotation.entries.first()

        check(entry.isBuiltInAnnotation) { "CallingConv should be a built-in annotation" }
        check(entry.builtInAnnotation == CjBuiltInAnnotation.CALLING_CONV) {
            "Should be CALLING_CONV annotation"
        }

        val convention = entry.callingConvention
        check(convention == CallingConvention.CDECL) {
            "Should extract CDECL calling convention, got: $convention"
        }

        // 测试 STDCALL
        val stdcallAnnotation = factory.createAnnotations("@CallingConv[STDCALL]")
        val stdcallEntry = stdcallAnnotation.entries.first()
        check(stdcallEntry.callingConvention == CallingConvention.STDCALL) {
            "Should extract STDCALL calling convention"
        }

        // 测试非 CallingConv 注解
        val otherAnnotation = factory.createAnnotations("@Deprecated")
        val otherEntry = otherAnnotation.entries.first()
        check(otherEntry.callingConvention == null) {
            "Non-CallingConv annotation should return null"
        }
    }

    // 测试 whenCondition 属性
    fun `test whenCondition property`() {
        // 测试简单条件
        val annotation1 = factory.createAnnotations("@When[os == \"windows\"]")
        val entry1 = annotation1.entries.first()

        check(entry1.isBuiltInAnnotation) { "When should be a built-in annotation" }
        check(entry1.builtInAnnotation == CjBuiltInAnnotation.WHEN) {
            "Should be WHEN annotation"
        }

        val condition1 = entry1.whenCondition
        check(condition1 != null) {
            "Should have a condition"
        }
        check(condition1.contains("os") && condition1.contains("windows")) {
            "Condition should contain 'os' and 'windows', got: $condition1"
        }

        // 测试另一个条件
        val annotation2 = factory.createAnnotations("@When[target == \"x86_64\"]")
        val entry2 = annotation2.entries.first()
        val condition2 = entry2.whenCondition
        check(condition2 != null && condition2.contains("target")) {
            "Condition should contain 'target', got: $condition2"
        }

        // 测试布尔条件
        val annotation3 = factory.createAnnotations("@When[debug]")
        val entry3 = annotation3.entries.first()
        val condition3 = entry3.whenCondition
        check(condition3 != null && condition3.contains("debug")) {
            "Condition should contain 'debug', got: $condition3"
        }

        // 测试非 When 注解
        val otherAnnotation = factory.createAnnotations("@Deprecated")
        val otherEntry = otherAnnotation.entries.first()
        check(otherEntry.whenCondition == null) {
            "Non-When annotation should return null"
        }
    }

    // 测试 overflowStrategy 属性
    fun `test overflowStrategy property`() {
        // 测试 OverflowThrowing
        val throwingAnnotation = factory.createAnnotations("@OverflowThrowing")
        val throwingEntry = throwingAnnotation.entries.first()
        check(throwingEntry.overflowStrategy == OverflowStrategy.THROWING) {
            "OverflowThrowing should return THROWING strategy"
        }

        // 测试 OverflowWrapping
        val wrappingAnnotation = factory.createAnnotations("@OverflowWrapping")
        val wrappingEntry = wrappingAnnotation.entries.first()
        check(wrappingEntry.overflowStrategy == OverflowStrategy.WRAPPING) {
            "OverflowWrapping should return WRAPPING strategy"
        }

        // 测试 OverflowSaturating
        val saturatingAnnotation = factory.createAnnotations("@OverflowSaturating")
        val saturatingEntry = saturatingAnnotation.entries.first()
        check(saturatingEntry.overflowStrategy == OverflowStrategy.SATURATING) {
            "OverflowSaturating should return SATURATING strategy"
        }

        // 测试非溢出注解
        val otherAnnotation = factory.createAnnotations("@Deprecated")
        val otherEntry = otherAnnotation.entries.first()
        check(otherEntry.overflowStrategy == null) {
            "Non-overflow annotation should return null"
        }
    }

    // 测试 FFI 注解分类
    fun `test isFFIAnnotation property`() {
        // 测试 FFI 注解
        val ffiAnnotations = listOf(
            "@C",
            "@Java",
            "@JavaMirror",
            "@JavaImpl",
            "@ObjCMirror",
            "@ObjCImpl",
            "@ForeignName[name: \"test\"]",
            "@CallingConv[CDECL]"
        )

        for (annotationText in ffiAnnotations) {
            val annotation = factory.createAnnotations(annotationText)
            val entry = annotation.entries.first()
            check(entry.isFFIAnnotation) {
                "$annotationText should be an FFI annotation"
            }
        }

        // 测试非 FFI 注解
        val nonFFIAnnotations = listOf("@Deprecated", "@Frozen", "@Intrinsic", "@When[os == \"windows\"]")

        for (annotationText in nonFFIAnnotations) {
            val annotation = factory.createAnnotations(annotationText)
            val entry = annotation.entries.first()
            check(!entry.isFFIAnnotation) {
                "$annotationText should not be an FFI annotation"
            }
        }
    }

    // 测试编译器指令注解分类
    fun `test isCompilerDirectiveAnnotation property`() {
        // 测试编译器指令注解
        val compilerDirectiveAnnotations = listOf(
            "@Intrinsic",
            "@When[os == \"windows\"]",
            "@ConstSafe",
            "@FastNative",
            "@Attribute[a,b,,c]",
            "@OverflowThrowing",
            "@OverflowWrapping",
            "@OverflowSaturating"
        )

        for (annotationText in compilerDirectiveAnnotations) {
            val annotation = factory.createAnnotations(annotationText)
            val entry = annotation.entries.first()
            check(entry.isCompilerDirectiveAnnotation) {
                "$annotationText should be a compiler directive annotation"
            }
        }

        // 测试非编译器指令注解
        val nonCompilerDirectiveAnnotations = listOf("@Deprecated", "@Frozen", "@C", "@Java")

        for (annotationText in nonCompilerDirectiveAnnotations) {
            val annotation = factory.createAnnotations(annotationText)
            val entry = annotation.entries.first()
            check(!entry.isCompilerDirectiveAnnotation) {
                "$annotationText should not be a compiler directive annotation"
            }
        }
    }

    // 测试语义标记注解分类
    fun `test isSemanticAnnotation property`() {
        // 测试语义标记注解
        val semanticAnnotations = listOf("@Deprecated", "@Frozen")

        for (annotationText in semanticAnnotations) {
            val annotation = factory.createAnnotations(annotationText)
            val entry = annotation.entries.first()
            check(entry.isSemanticAnnotation) {
                "$annotationText should be a semantic annotation"
            }
        }

        // 测试非语义标记注解
        val nonSemanticAnnotations = listOf("@C", "@Java", "@Intrinsic", "@When[os == \"windows\"]")

        for (annotationText in nonSemanticAnnotations) {
            val annotation = factory.createAnnotations(annotationText)
            val entry = annotation.entries.first()
            check(!entry.isSemanticAnnotation) {
                "$annotationText should not be a semantic annotation"
            }
        }
    }

    // 综合测试：混合注解识别
    fun `test mixed annotations recognition`() {
        val function = factory.createFunction(
            """
            @Deprecated
            @C
            @CallingConv[CDECL]
            @OverflowThrowing
            func complexFunction(): Unit {}
            """.trimIndent()
        )

        check(function.annotationEntries.size == 4) {
            "Should have 4 annotations"
        }

        val annotations = function.annotationEntries

        // @Deprecated - 语义标记注解
        val deprecated = annotations[0]
        check(deprecated.isBuiltInAnnotation)
        check(deprecated.isSemanticAnnotation)
        check(!deprecated.isFFIAnnotation)
        check(!deprecated.isCompilerDirectiveAnnotation)

        // @C - FFI 注解
        val cAnnotation = annotations[1]
        check(cAnnotation.isBuiltInAnnotation)
        check(cAnnotation.isFFIAnnotation)
        check(!cAnnotation.isSemanticAnnotation)
        check(!cAnnotation.isCompilerDirectiveAnnotation)

        // @CallingConv - FFI 注解
        val callingConv = annotations[2]
        check(callingConv.isBuiltInAnnotation)
        check(callingConv.isFFIAnnotation)
        check(callingConv.builtInAnnotation == CjBuiltInAnnotation.CALLING_CONV)
        check(callingConv.callingConvention == CallingConvention.CDECL) {
            "Should extract CDECL calling convention"
        }

        // @OverflowThrowing - 编译器指令注解
        val overflowThrowing = annotations[3]
        check(overflowThrowing.isBuiltInAnnotation)
        check(overflowThrowing.isCompilerDirectiveAnnotation)
        check(overflowThrowing.overflowStrategy == OverflowStrategy.THROWING)
        check(!overflowThrowing.isFFIAnnotation)
        check(!overflowThrowing.isSemanticAnnotation)
    }

    // 测试所有内置注解的识别完整性
    fun `test all built-in annotations are recognized`() {
        val allBuiltInAnnotations = CjBuiltInAnnotation.entries

        for (builtIn in allBuiltInAnnotations) {
            val annotationText = when (builtIn) {
                CjBuiltInAnnotation.CALLING_CONV -> "@CallingConv[CDECL]"
                CjBuiltInAnnotation.WHEN -> "@When[os == \"windows\"]"
                CjBuiltInAnnotation.ATTRIBUTE -> "@Attribute[name, \"test\"]"
                CjBuiltInAnnotation.FOREIGN_NAME -> "@ForeignName[name: \"test\"]"
                else -> "@${builtIn.annotationName}"
            }

            val annotation = factory.createAnnotations(annotationText)
            val entry = annotation.entries.first()

            check(entry.isBuiltInAnnotation) {
                "${builtIn.annotationName} should be recognized as built-in"
            }

            check(entry.builtInAnnotation == builtIn) {
                "${builtIn.annotationName} should map to correct enum value"
            }

            check(entry.shortName?.asString() == builtIn.annotationName) {
                "${builtIn.annotationName} shortName should match"
            }
        }
    }
}
