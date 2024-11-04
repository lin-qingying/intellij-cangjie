package com.linqingying.cangjie.doc.psi.impl

import com.linqingying.cangjie.psi.psiUtil.getChildrenOfType
import com.intellij.lang.ASTNode


/**
 *文档注释中描述单个类、方法或属性的部分由被记录的元素产生。例如，类的文档注释可以有类本身、其主构造函数和每个在主构造函数中定义的属性
 */
class CDocSection(node: ASTNode) : CDocTag(node) {
    /**
     *返回节的名称(引导节的文档标签的名称或对于默认部分为NULL)
     */
    override fun getName(): String? =
        (firstChild as? CDocTag)?.name

    override fun getSubjectName(): String? =
        (firstChild as? CDocTag)?.getSubjectName()

    override fun getContent(): String =
        (firstChild as? CDocTag)?.getContent() ?: super.getContent()

    fun findTagsByName(name: String): List<CDocTag> {
        return getChildrenOfType<CDocTag>().filter { it.name == name }
    }

    fun findTagByName(name: String): CDocTag? = findTagsByName(name).firstOrNull()
}
