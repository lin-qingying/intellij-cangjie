package org.cangnova.cangjie.ide.decompiled

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjAbstractClassBody
import org.cangnova.cangjie.psi.CjAnnotations
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.CangJieClassStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieModifierListStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieNamedFunctionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePlaceHolderStubImpl
import org.cangnova.cangjie.psi.stubs.impl.ModifierMaskUtils
import org.cangnova.cangjie.test.CangJieLightCodeInsightFixtureTestCase
import kotlin.test.assertTrue

/**
 * 锁定产品插件 classpath 下 `.cjo` decompiled text 的模态修饰符渲染。
 *
 * 这里直接调用 shipped decompiler text builder，确保 `abstract class` 与 `open func`
 * 不会在产品测试模块里丢失。
 */
class CangJieDecompiledRenderingTest : CangJieLightCodeInsightFixtureTestCase() {
    fun testBuildDecompiledTextRendersAbstractClassAndOpenMemberModifiers() {
        val fileStub = CangJieFileStubImpl.forFile(samplePackage)
        val classStub = CangJieClassStubImpl(
            type = CjStubElementTypes.CLASS,
            parent = fileStub,
            qualifiedName = StringRef.fromString(sampleClassFqName.asString()),
            classId = null,
            name = StringRef.fromString(sampleClassName.asString()),
            superNames = emptyArray(),
        )
        createEmptyHeader(classStub, modifierMask = computeModifierMask(abstract = true))
        val classBodyStub = CangJiePlaceHolderStubImpl<CjAbstractClassBody>(classStub, CjStubElementTypes.CLASS_BODY)
        val functionStub = CangJieNamedFunctionStubImpl(
            parent = classBodyStub,
            element = CjStubElementTypes.FUNCTION,
            nameRef = StringRef.fromString("grow"),
            isTopLevel = false,
            fqName = null,
            hasBlockBody = true,
            hasBody = true,
            hasTypeParameterListBeforeFunctionName = false,
            origin = null,
        )
        createEmptyHeader(functionStub, modifierMask = computeModifierMask(open = true))

        val rendered = buildDecompiledText(fileStub)
        assertTrue(rendered.contains("abstract class Widget {"), rendered)
        assertTrue(rendered.contains("open func grow() { /* compiled code */ }"), rendered)
    }

    private fun buildDecompiledText(fileStub: CangJieFileStubImpl): String {
        val owner = Class.forName("org.cangnova.cangjie.analysis.decompiled.psi.text.DecompiledTextBuilderKt")
        val method = owner.getDeclaredMethod("buildDecompiledText", CangJieFileStubImpl::class.java)
        method.isAccessible = true
        return method.invoke(null, fileStub) as String
    }

    private fun createEmptyHeader(parent: StubElement<*>, modifierMask: Long = 0L) {
        CangJiePlaceHolderStubImpl<CjAnnotations>(parent, CjStubElementTypes.ANNOTATIONS)
        CangJieModifierListStubImpl(parent, modifierMask, CjStubElementTypes.MODIFIER_LIST)
    }

    private fun computeModifierMask(open: Boolean = false, abstract: Boolean = false): Long {
        return ModifierMaskUtils.computeMask(
            hasModifier = { modifier ->
                when (modifier) {
                    CjTokens.OPEN_KEYWORD -> open
                    CjTokens.ABSTRACT_KEYWORD -> abstract
                    else -> false
                }
            },
            hasAdditionalModifier = { false },
        )
    }

    private companion object {
        val samplePackage: FqName = FqName("sample")
        val sampleClassName: Name = Name.identifier("Widget")
        val sampleClassFqName: FqName = samplePackage.child(sampleClassName)
    }
}
