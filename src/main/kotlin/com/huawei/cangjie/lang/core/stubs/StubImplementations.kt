package com.huawei.cangjie.lang.core.stubs

import com.huawei.cangjie.lang.CjLanguage
import com.huawei.cangjie.lang.core.lexer.CangJieLexer
import com.huawei.cangjie.lang.core.parser.CangJieParser
import com.huawei.cangjie.lang.core.psi.CJ_ITEMS
import com.huawei.cangjie.lang.core.psi.CangJieBlock
import com.huawei.cangjie.lang.core.psi.CangJieMainFunc
import com.huawei.cangjie.lang.core.psi.CjElementTypes
import com.huawei.cangjie.lang.core.psi.CjElementTypes.*
import com.huawei.cangjie.lang.core.psi.impl.CangJieBlockImpl
import com.huawei.cangjie.lang.core.psi.impl.CangJieMainFuncImpl
import com.huawei.cangjie.lang.core.psi.tokenSetOf
import com.intellij.lang.*
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.project.Project
import com.intellij.psi.StubBuilder
import com.intellij.psi.impl.source.tree.LazyParseableElement
import com.intellij.psi.impl.source.tree.RecursiveTreeElementWalkingVisitor
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.tree.*
import com.intellij.util.CharTable
import com.intellij.util.diff.FlyweightCapableTreeStructure


fun factory(name: String): CjStubElementType<*, *> = when (name) {


    "BLOCK" -> CjBlockStubType

    "MAIN_FUNC" -> CjMainFuncStubType


    else -> error("Unknown element $name")
}

private class ItemSeekingVisitor private constructor() : RecursiveTreeElementWalkingVisitor() {
    private var hasItemsOrAttrs = false
    override fun visitNode(element: TreeElement) {
        val elementType = element.elementType
        if (elementType in CJ_ITEMS) {
            hasItemsOrAttrs = true
            stopWalking()
        } else {
            super.visitNode(element)
        }
    }

    companion object {
        fun containsItems(node: ASTNode): Boolean {
            val visitor = ItemSeekingVisitor()
            (node as TreeElement).acceptTree(visitor)
            return visitor.hasItemsOrAttrs
        }
    }
}


object CjMainFuncStubType :
    CjPlaceholderStub.Type<CangJieMainFunc>("MAIN_FUNC", ::CangJieMainFuncImpl),
    ICustomParsingType,
    ICompositeElementType,
    IReparseableElementTypeBase,
    ILightLazyParseableElementType {

       //标记是否已经创建
        var isCreated = false

    override fun parse(text: CharSequence, table: CharTable): ASTNode {
      return  LazyParseableElement(this, text)
    }

    override fun createCompositeNode(): ASTNode {


            return LazyParseableElement(this, null)



    }

    override fun parseContents(chameleon: ASTNode): ASTNode {
        //如果已经定义了一个main方法，则不需要解析
        if (chameleon.treeParent.findChildByType(MAIN_FUNC) != null) {
            return chameleon
        }


        val project = chameleon.treeParent.psi.project
        val builder =
            PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, CjLanguage, chameleon.chars)
        parseMainFunc(builder)


        return builder.treeBuilt.firstChildNode


    }


    override fun parseContents(chameleon: LighterLazyParseableNode?): FlyweightCapableTreeStructure<LighterASTNode> {
        val project = chameleon?.containingFile?.project ?: error("`containingFile` must not be null: $chameleon")
        val builder =
            PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, CjLanguage, chameleon.text)
          parseMainFunc(builder)
        return builder.lightTree
    }

    private fun parseMainFunc(builder: PsiBuilder) {

        val adaptBuilder = GeneratedParserUtilBase.adapt_builder_(MAIN_FUNC, builder, CangJieParser(), null)
        val marker = GeneratedParserUtilBase.enter_section_(adaptBuilder, 0, GeneratedParserUtilBase._COLLAPSE_, null)
        val result = CangJieParser.MainFunc(adaptBuilder, 0)
        GeneratedParserUtilBase.exit_section_(
            adaptBuilder,
            0,
            marker,
            MAIN_FUNC,
            result,
            true,
            GeneratedParserUtilBase.TRUE_CONDITION
        )

    }

}

object CjBlockStubType :
    CjPlaceholderStub.Type<CangJieBlock>("BLOCK", ::CangJieBlockImpl),
    ICustomParsingType,
    ICompositeElementType,
    IReparseableElementTypeBase,
    ILightLazyParseableElementType {
    /**注意：如果[StubBuilder.skipChildProcessingWhenBuildingStubs]为[节点]返回`true`，则必须返回`False`*/
    override fun shouldCreateStub(node: ASTNode): Boolean {
        return if (node.treeParent.elementType == FUNCTION) {
            ItemSeekingVisitor.containsItems(node)
        } else {
            createStubIfParentIsStub(node) || node.findChildByType(CJ_ITEMS) != null
        }
    }

    //延迟解析(函数体)
    override fun parse(text: CharSequence, table: CharTable): ASTNode = LazyParseableElement(this, text)

    //非惰性大小写(`if`Body等)
    override fun createCompositeNode(): ASTNode = LazyParseableElement(this, null)

    override fun parseContents(chameleon: ASTNode): ASTNode? {
        val project = chameleon.treeParent.psi.project
        val builder =
            PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, CjLanguage, chameleon.chars)
        parseBlock(builder)


        return builder.treeBuilt.firstChildNode


    }

    override fun parseContents(chameleon: LighterLazyParseableNode): FlyweightCapableTreeStructure<LighterASTNode> {
        val project = chameleon.containingFile?.project ?: error("`containingFile` must not be null: $chameleon")
        val builder =
            PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, CjLanguage, chameleon.text)
        parseBlock(builder)
        return builder.lightTree
    }


    private fun parseBlock(builder: PsiBuilder) {
        val adaptBuilder = GeneratedParserUtilBase.adapt_builder_(BLOCK, builder, CangJieParser(), null)
        val marker = GeneratedParserUtilBase.enter_section_(adaptBuilder, 0, GeneratedParserUtilBase._COLLAPSE_, null)
        val result = CangJieParser.FuncBlock(adaptBuilder, 0)
        GeneratedParserUtilBase.exit_section_(
            adaptBuilder,
            0,
            marker,
            BLOCK,
            result,
            true,
            GeneratedParserUtilBase.TRUE_CONDITION
        )
    }

    //仅限于函数体，因为它是经过良好测试的用例。可以不限于将来的任何区块
    override fun isReparseable(
        currentNode: ASTNode,
        newText: CharSequence,
        fileLanguage: Language,
        project: Project
    ): Boolean =
        currentNode.treeParent?.elementType == FUNCTION && PsiBuilderUtil.hasProperBraceBalance(
            newText,
            CangJieLexer(),
            LBRACE,
            RBRACE
        )

    //避免重复词法分析
    override fun reuseCollapsedTokens(): Boolean = true


}
