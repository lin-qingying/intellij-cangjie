package cn.cangnova.cangjie.psi

import cn.cangnova.cangjie.CangJieTestBase

class CjPsiFactoryTest : CangJieTestBase() {

    private lateinit var factory: CjPsiFactory

    override fun setUp() {
        super.setUp()
        factory = CjPsiFactory(project)
    }

    fun `test create property`() {
        val property = factory.createProperty("""
            prop a:Int64 {
                get(){
                    return 1
                }
            }
        """.trimIndent())
        
        check(property.name == "a")
    }
    
    fun `test create mutable property`() {
        val property = factory.createProperty("""
            mut prop b:Int {
                get(){ return 42 }
                set(value){ }
            }
        """.trimIndent())
        
        check(property.name == "b")
    }
    
    fun `test create class`() {
        val clazz = factory.createClass("""
            class Test {
                prop x: Int {
                    get(){ return 0 }
                }
                
                func foo() -> Int {
                    return x
                }
            }
        """.trimIndent())
        
        check(clazz.name == "Test")
    }
    
    fun `test create file`() {
        val file = factory.createFile("""
            package cn.cangnova.test
            
            class Test {
                prop x: Int {
                    get(){ return 0 }
                }
            }
        """.trimIndent())
        
        check(file.declarations.size == 1)
    }

    fun `test create basic function`() {
        val function = factory.createFunction("""
            func add(a: Int64, b: Int64): Int64 {
                return a + b
            }
        """.trimIndent())
        
        check(function.name == "add")
        check(function.valueParameters.size == 2)
    }

    fun `test create function with named parameters`() {
        val function = factory.createFunction("""
            func greet(name!: String = "World"): String {
                return "Hello, " + name
            }
        """.trimIndent())
        
        check(function.name == "greet")
        check(function.valueParameters.size == 1)
    }

    fun `test create function without return type`() {
        val function = factory.createFunction("""
            func add(a: Int64, b: Int64) {
                return a + b
            }
        """.trimIndent())
        
        check(function.name == "add")
    }

    fun `test create function with local variables`() {
        val function = factory.createFunction("""
            func add(a: Int64, b: Int64): Int64 {
                var r = 0
                r = a + b
                return r
            }
        """.trimIndent())
        
        check(function.name == "add")
    }

    fun `test create whitespace`() {
        val space = factory.createWhiteSpace()
        check(space.text == " ")
        
        val newLine = factory.createNewLine()
        check(newLine.text == "\n ")
        
        val indent = factory.createIndent()
        check(indent.text == "    ")
    }
}