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
 */

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.CangJieTestBase
import org.cangnova.cangjie.name.FqName

/**
 * 测试导入指令的解析和 FqName 生成
 */
class CjImportParsingTest : CangJieTestBase() {

    private lateinit var factory: CjPsiFactory

    override fun setUp() {
        super.setUp()
        factory = CjPsiFactory(project)
    }

    // ========== 单个导入测试 ==========

    fun `test single import`() {
        val file = factory.createFile("""
            package test
            import a.b.C

            main() {}
        """.trimIndent())

        val importDirectives = file.importDirectives
        assertEquals(1, importDirectives.size)

        val directive = importDirectives[0]
        val items = directive.importItems
        assertEquals(1, items.size)

        val item = items[0]
        assertEquals(FqName("a.b.C"), item.importedFqName)
        assertFalse(item.isAllUnder)
        assertNull(item.aliasName)
    }
    fun `test single public import with wildcard`() {
        val file = factory.createFile("""
            package test
           public import a.b.*

            main() {}
        """.trimIndent())

        val importDirectives = file.importDirectives
        assertEquals(1, importDirectives.size)

        val directive = importDirectives[0]
        val items = directive.importItems
        assertEquals(1, items.size)

        val item = items[0]
        assertEquals(FqName("a.b"), item.importedFqName)
        assertTrue(item.isAllUnder)
        assertNull(item.aliasName)
    }
    fun `test single import with wildcard`() {
        val file = factory.createFile("""
            package test
            import a.b.*

            main() {}
        """.trimIndent())

        val importDirectives = file.importDirectives
        assertEquals(1, importDirectives.size)

        val directive = importDirectives[0]
        val items = directive.importItems
        assertEquals(1, items.size)

        val item = items[0]
        assertEquals(FqName("a.b"), item.importedFqName)
        assertTrue(item.isAllUnder)
        assertNull(item.aliasName)
    }

    fun `test single import with alias`() {
        val file = factory.createFile("""
            package test
            import a.b.C as MyC

            main() {}
        """.trimIndent())

        val importDirectives = file.importDirectives
        assertEquals(1, importDirectives.size)

        val directive = importDirectives[0]
        val items = directive.importItems
        assertEquals(1, items.size)

        val item = items[0]
        assertEquals(FqName("a.b.C"), item.importedFqName)
        assertFalse(item.isAllUnder)
        assertEquals("MyC", item.aliasName)
    }

    // ========== 同包多项导入测试 ==========

    fun `test same package multiple imports`() {
        val file = factory.createFile("""
            package test
            import a.b.{C, D, E}

            main() {}
        """.trimIndent())

        val importDirectives = file.importDirectives
        assertEquals(1, importDirectives.size)

        val directive = importDirectives[0]
        val items = directive.importItems
        assertEquals(3, items.size)

        // 验证第一个导入项
        assertEquals(FqName("a.b.C"), items[0].importedFqName)
        assertFalse(items[0].isAllUnder)
        assertNull(items[0].aliasName)

        // 验证第二个导入项
        assertEquals(FqName("a.b.D"), items[1].importedFqName)
        assertFalse(items[1].isAllUnder)
        assertNull(items[1].aliasName)

        // 验证第三个导入项
        assertEquals(FqName("a.b.E"), items[2].importedFqName)
        assertFalse(items[2].isAllUnder)
        assertNull(items[2].aliasName)
    }

    fun `test same package multiple imports with alias`() {
        val file = factory.createFile("""
            package test
            import a.b.{C as CC, D, E as EE}

            main() {}
        """.trimIndent())

        val importDirectives = file.importDirectives
        assertEquals(1, importDirectives.size)

        val directive = importDirectives[0]
        val items = directive.importItems
        assertEquals(3, items.size)

        // 验证带别名的导入
        assertEquals(FqName("a.b.C"), items[0].importedFqName)
        assertEquals("CC", items[0].aliasName)

        // 验证不带别名的导入
        assertEquals(FqName("a.b.D"), items[1].importedFqName)
        assertNull(items[1].aliasName)

        // 验证带别名的导入
        assertEquals(FqName("a.b.E"), items[2].importedFqName)
        assertEquals("EE", items[2].aliasName)
    }

    // ========== 多包导入测试 ==========

    fun `test multiple package imports`() {
        val file = factory.createFile("""
            package test
            import {a.b.C, x.y.Z}

            main() {}
        """.trimIndent())

        val importDirectives = file.importDirectives
        assertEquals(1, importDirectives.size)

        val directive = importDirectives[0]
        val items = directive.importItems
        assertEquals(2, items.size)

        // 验证第一个导入项
        assertEquals(FqName("a.b.C"), items[0].importedFqName)
        assertFalse(items[0].isAllUnder)

        // 验证第二个导入项
        assertEquals(FqName("x.y.Z"), items[1].importedFqName)
        assertFalse(items[1].isAllUnder)
    }

    fun `test multiple package imports with wildcard`() {
        val file = factory.createFile("""
            package test
            import {a.b.*, x.y.*}

            main() {}
        """.trimIndent())

        val importDirectives = file.importDirectives
        assertEquals(1, importDirectives.size)

        val directive = importDirectives[0]
        val items = directive.importItems
        assertEquals(2, items.size)

        // 验证第一个通配符导入
        assertEquals(FqName("a.b"), items[0].importedFqName)
        assertTrue(items[0].isAllUnder)

        // 验证第二个通配符导入
        assertEquals(FqName("x.y"), items[1].importedFqName)
        assertTrue(items[1].isAllUnder)
    }

    fun `test mixed imports with wildcard and specific`() {
        val file = factory.createFile("""
            package test
            import {a.b.*, c.d.E}

            main() {}
        """.trimIndent())

        val importDirectives = file.importDirectives
        assertEquals(1, importDirectives.size)

        val directive = importDirectives[0]
        val items = directive.importItems
        assertEquals(2, items.size)

        // 验证通配符导入
        assertEquals(FqName("a.b"), items[0].importedFqName)
        assertTrue(items[0].isAllUnder)

        // 验证具名导入
        assertEquals(FqName("c.d.E"), items[1].importedFqName)
        assertFalse(items[1].isAllUnder)
    }

    fun `test mixed imports with alias`() {
        val file = factory.createFile("""
            package test
            import {a.b.*, c.d.E as MyE}

            main() {}
        """.trimIndent())

        val importDirectives = file.importDirectives
        assertEquals(1, importDirectives.size)

        val directive = importDirectives[0]
        val items = directive.importItems
        assertEquals(2, items.size)

        // 验证通配符导入
        assertEquals(FqName("a.b"), items[0].importedFqName)
        assertTrue(items[0].isAllUnder)
        assertNull(items[0].aliasName)

        // 验证带别名的具名导入
        assertEquals(FqName("c.d.E"), items[1].importedFqName)
        assertFalse(items[1].isAllUnder)
        assertEquals("MyE", items[1].aliasName)
    }

    // ========== 访问修饰符测试 ==========

    fun `test public import`() {
        val file = factory.createFile("""
            package test
            public import a.b.C

            main() {}
        """.trimIndent())

        val importDirectives = file.importDirectives
        assertEquals(1, importDirectives.size)

        val directive = importDirectives[0]
        // 验证导入项正确解析
        val items = directive.importItems
        assertEquals(1, items.size)
        assertEquals(FqName("a.b.C"), items[0].importedFqName)
    }

    // ========== FqName 生成边界测试 ==========

    fun `test fqName generation for simple name`() {
        val file = factory.createFile("""
            package test
            import foo

            main() {}
        """.trimIndent())

        val items = file.importDirectives[0].importItems
        assertEquals(FqName("foo"), items[0].importedFqName)
    }

    fun `test fqName generation for deeply nested path`() {
        val file = factory.createFile("""
            package test
            import a.b.c.d.e.f.G

            main() {}
        """.trimIndent())

        val items = file.importDirectives[0].importItems
        assertEquals(FqName("a.b.c.d.e.f.G"), items[0].importedFqName)
    }

    fun `test fqName generation for std library import`() {
        val file = factory.createFile("""
            package test
            import std.core.String

            main() {}
        """.trimIndent())

        val items = file.importDirectives[0].importItems
        assertEquals(FqName("std.core.String"), items[0].importedFqName)
    }

    // ========== 多个导入指令测试 ==========

    fun `test multiple import directives`() {
        val file = factory.createFile("""
            package test
            import a.b.C
            import x.y.Z
            import {m.n.*, p.q.R}

            main() {}
        """.trimIndent())

        val importDirectives = file.importDirectives
        assertEquals(3, importDirectives.size)

        // 第一个导入指令
        val directive1 = importDirectives[0]
        assertEquals(1, directive1.importItems.size)
        assertEquals(FqName("a.b.C"), directive1.importItems[0].importedFqName)

        // 第二个导入指令
        val directive2 = importDirectives[1]
        assertEquals(1, directive2.importItems.size)
        assertEquals(FqName("x.y.Z"), directive2.importItems[0].importedFqName)

        // 第三个导入指令
        val directive3 = importDirectives[2]
        assertEquals(2, directive3.importItems.size)
        assertEquals(FqName("m.n"), directive3.importItems[0].importedFqName)
        assertTrue(directive3.importItems[0].isAllUnder)
        assertEquals(FqName("p.q.R"), directive3.importItems[1].importedFqName)
        assertFalse(directive3.importItems[1].isAllUnder)
    }

    // ========== importDirectivesItem 兼容性测试 ==========

    fun `test file importDirectivesItem property`() {
        val file = factory.createFile("""
            package test
            import a.b.{C, D}
            import x.y.Z

            main() {}
        """.trimIndent())

        // 验证 importDirectivesItem 返回所有扁平化的导入项
        val allImportItems = file.importDirectivesItem
        assertEquals(3, allImportItems.size)

        // 验证 FqName
        assertEquals(FqName("a.b.C"), allImportItems[0].importedFqName)
        assertEquals(FqName("a.b.D"), allImportItems[1].importedFqName)
        assertEquals(FqName("x.y.Z"), allImportItems[2].importedFqName)
    }

    fun `test find import by alias`() {
        val file = factory.createFile("""
            package test
            import a.b.C as MyC
            import x.y.{Z as MyZ, W}

            main() {}
        """.trimIndent())

        // 测试查找别名
        val myC = file.findImportByAlias("MyC")
        assertNotNull(myC)
        assertEquals(FqName("a.b.C"), myC.importedFqName)

        val myZ = file.findImportByAlias("MyZ")
        assertNotNull(myZ)
        assertEquals(FqName("x.y.Z"), myZ.importedFqName)

        // 测试查找不存在的别名
        val notFound = file.findImportByAlias("NotExists")
        assertNull(notFound)
    }

    // ========== ImportPath 转换测试 ==========

    fun `test importPath conversion`() {
        val file = factory.createFile("""
            package test
            import a.b.C as MyC
            import x.y.*

            main() {}
        """.trimIndent())

        val items = file.importDirectives.flatMap { it.importItems }

        // 测试带别名的 ImportPath
        val path1 = items[0].importPath
        assertNotNull(path1)
        assertEquals(FqName("a.b.C"), path1.fqName)
        assertFalse(path1.isAllUnder )
        assertEquals("MyC", path1.alias?.asString())

        // 测试通配符的 ImportPath
        val path2 = items[1].importPath
        assertNotNull(path2)
        assertEquals(FqName("x.y"), path2.fqName)
        assertTrue(path2.isAllUnder)
        assertNull(path2.alias)
    }
}
