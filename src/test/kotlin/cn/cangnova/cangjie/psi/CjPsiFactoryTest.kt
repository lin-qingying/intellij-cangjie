package cn.cangnova.cangjie.psi

import cn.cangnova.cangjie.CangJieTestBase
import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.name.Name

class CjPsiFactoryTest : CangJieTestBase() {

    private lateinit var factory: CjPsiFactory

    override fun setUp() {
        super.setUp()
        factory = CjPsiFactory(project)
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

    fun `test create file`() {
        val file = factory.createFile(
            """
            package cn.cangnova.test
            
            class Test {
                prop x: Int {
                    get(){ return 0 }
                }
            }
        """.trimIndent()
        )

        check(file.declarations.size == 1)
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
}