package org.cangnova.cangjie.resolve.declaration

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase
import org.cangnova.cangjie.descriptors.CallableMemberDescriptor
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter

/**
 * 调试测试：验证扩展的 fake override 生成和重写检查
 */
class ExtendOverrideCheckDebugTest : CangJieAnalysisTestBase() {

    /**
     * 测试未实现接口方法时的 fake override 生成
     */
    fun `test fake override generation when method not implemented`() {
        val file = createFile(
            """
            package test

            interface Printable {
                func print(): Unit
            }

            class MyClass {
            }

            extend MyClass <: Printable {
                // 故意不实现 print() 方法
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val extendDecl = PsiTreeUtil.findChildOfType(file, CjExtend::class.java)
            assertNotNull("应该找到扩展声明", extendDecl)

            val extendDescriptor = bindingContext[BindingContext.EXTEND, extendDecl!!]
            assertNotNull("应该创建扩展描述符", extendDescriptor)

            // 获取成员作用域中的所有成员
            val memberScope = extendDescriptor!!.memberScope
            val allDescriptors = memberScope.getContributedDescriptors(
                DescriptorKindFilter.CALLABLES,
                { true }
            )

            println("=== 扩展成员作用域中的所有成员 ===")
            for (desc in allDescriptors) {
                if (desc is CallableMemberDescriptor) {
                    println("成员: ${desc.name}, kind: ${desc.kind}, modality: ${desc.modality}")
                    println("  overriddenDescriptors: ${desc.overriddenDescriptors.map { "${it.name} (${it.modality})" }}")
                }
            }

            // 检查是否有 print 函数
            val printFunctions = memberScope.getContributedFunctions(
                org.cangnova.cangjie.name.Name.identifier("print"),
                org.cangnova.cangjie.incremental.components.NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS
            )

            assertTrue("应该有 print 函数 (fake override)", printFunctions.isNotEmpty())

            val printFunc = printFunctions.first()
            assertEquals(
                "未实现的接口方法应该是 FAKE_OVERRIDE",
                CallableMemberDescriptor.Kind.FAKE_OVERRIDE,
                printFunc.kind
            )

            // 检查诊断信息
            val diagnostics = bindingContext.diagnostics.all()
            println("=== 诊断信息 ===")
            for (diag in diagnostics) {
                println("诊断: ${diag.factory.name} at ${diag.psiElement.text.take(50)}")
            }

            // 检查是否报告了 ABSTRACT_MEMBER_NOT_IMPLEMENTED
            val abstractNotImplDiag = diagnostics.filter {
                it.factory.name == "ABSTRACT_MEMBER_NOT_IMPLEMENTED"
            }
            println("ABSTRACT_MEMBER_NOT_IMPLEMENTED 诊断数量: ${abstractNotImplDiag.size}")

            assertTrue(
                "应该报告 ABSTRACT_MEMBER_NOT_IMPLEMENTED 错误",
                abstractNotImplDiag.isNotEmpty()
            )
        }
    }

    /**
     * 测试实现了接口方法时不应报告错误
     */
    fun `test no error when interface method is implemented`() {
        val file = createFile(
            """
            package test

            interface Printable {
                func print(): Unit
            }

            class MyClass {
            }

            extend MyClass <: Printable {
                func print(): Unit {}
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val diagnostics = bindingContext.diagnostics.all()
            println("=== 诊断信息（已实现） ===")
            for (diag in diagnostics) {
                println("诊断: ${diag.factory.name} at ${diag.psiElement.text.take(50)}")
            }

            val abstractNotImplDiag = diagnostics.filter {
                it.factory.name == "ABSTRACT_MEMBER_NOT_IMPLEMENTED"
            }

            assertTrue(
                "实现了接口方法后不应报告 ABSTRACT_MEMBER_NOT_IMPLEMENTED",
                abstractNotImplDiag.isEmpty()
            )
        }
    }
}
