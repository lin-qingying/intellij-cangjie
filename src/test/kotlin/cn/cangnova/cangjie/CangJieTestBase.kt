package cn.cangnova.cangjie

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.Test
import org.junit.internal.MethodSorter
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.TestClass
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.test.asserter

/**
 * 这是一个自定义的 JUnit4 测试运行器，它支持两种风格的测试方法：
 * 1. JUnit4 风格 - 使用 @Test 注解
 * 2. JUnit3 风格 - 方法名以 "test" 开头
 */
class CangJieJUnit4TestRunner(testClass: Class<*>) : BlockJUnit4ClassRunner(testClass) {

    override fun computeTestMethods(): List<FrameworkMethod> =
        addJUnit3Methods(super.computeTestMethods(), testClass)

    companion object {
        fun addJUnit3Methods(junit4Methods: List<FrameworkMethod>, testClass: TestClass): List<FrameworkMethod> {
            val junit3Methods = computeJUnit3TestMethods(testClass)

            return if (junit3Methods.isEmpty()) {
                junit4Methods
            } else {
                val all = junit4Methods.toMutableList()
                junit3Methods.mapTo(all) { FrameworkMethod(it) }
                Collections.unmodifiableList(all)
            }
        }

        private fun computeJUnit3TestMethods(testClass: TestClass): List<Method> {
            val theClass = testClass.javaClass
            var superClass = theClass
            val names = mutableSetOf<String>()
            val testMethods = mutableListOf<Method>()
            while (Test::class.java.isAssignableFrom(superClass)) {
                for (method in MethodSorter.getDeclaredMethods(superClass)) {
                    val name = method.name
                    if (!names.contains(name) && isJUnit3TestMethod(method)) {
                        names.add(name)
                        testMethods += method
                    }
                }
                superClass = superClass.superclass
            }

            return testMethods
        }

        private fun isJUnit3TestMethod(m: Method): Boolean {
            return m.parameterTypes.isEmpty()
                    && m.name.startsWith("test")
                    && m.returnType == Void.TYPE
                    && Modifier.isPublic(m.modifiers)
                    && m.getAnnotation(org.junit.Test::class.java) == null
        }
    }
}

/**
 * 不需要Intellij平台的测试类,可以提升运行速度
 */
@RunWith(CangJieJUnit4TestRunner::class)
abstract class CangJieNoPlatformTestBase : junit.framework.TestCase() {

}

/**
 * Intellij平台测试类
 */
@RunWith(CangJieJUnit4TestRunner::class)
abstract class CangJieTestBase : BasePlatformTestCase(), CangJieTestCase {
    open val dataPath: String = ""

    override fun getTestDataPath(): String = "${TestCase.testResourcesPath}/$dataPath"
    protected val fileName: String
        get() = "$testName.cj"

    /** Asserts that the [actual] value is not `null`, with an optional [message]. */
    @OptIn(ExperimentalContracts::class)
    public fun <T : Any> assertNotNull(actual: T?, message: String? = null): T {
        contract { returns() implies (actual != null) }
        asserter.assertNotNull(message, actual)
        return actual!!
    }

    private val testName: String
        get() = getTestName(true)

    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val camelCase = super.getTestName(lowercaseFirstLetter)
        return TestCase.camelOrWordsToSnake(camelCase)
    }
}

interface TestCase {
    val testFileExtension: String
    fun getTestDataPath(): String
    fun getTestName(lowercaseFirstLetter: Boolean): String

    companion object {
        const val testResourcesPath = "src/test/resources"

        @JvmStatic
        fun camelOrWordsToSnake(name: String): String {
            if (' ' in name) return name.trim().replace(" ", "_")

            return name.split("(?=[A-Z])".toRegex()).joinToString("_", transform = String::lowercase)
        }
    }
}

interface CangJieTestCase : TestCase {
    override val testFileExtension: String get() = "cj"
}
