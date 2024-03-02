package com.huawei.cangjie.references

import com.huawei.cangjie.psi.CjElement
import com.intellij.psi.PsiPolyVariantReference

//
//interface CjReference : PsiPolyVariantReference {
//    val resolver: ResolveCache.PolyVariantResolver<CjReference>
//    override fun getElement(): CjElement
//    val resolvesByNames: Collection<Name>
//
//}
//
//abstract class AbstractCjReference<T : CjElement>(element: T) : PsiPolyVariantReferenceBase<T>(element), CjReference {
//    open fun canRename(): Boolean = false
//
//    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
//        ResolveCache.getInstance(expression.project).resolveWithCaching(this, resolver, false, incompleteCode)
//
//
//
////    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
////        // 这里是实现多重引用解析的逻辑
////        // 例如，遍历 PSI 树、查询符号表等方式来定位引用的所有目标
////        val resolveResults = findAllResolveResults()
////        return resolveResults.toTypedArray()
////    }
//
////    private fun findAllResolveResults(): List<ResolveResult> {
////
////
////        // 这里是查找所有目标元素的逻辑
////        // 例如，通过遍历 PSI 树找到所有目标元素
////        // 以下是一个简单示例，假设引用引用了同名的多个变量
////        val resolveResults = mutableListOf<ResolveResult>()
////        val referenceName = myElement.text
////        // 假设 parent 是引用所在的上下文
////        for (child in myElement.parent.children) {
////            // 这里假设每个子元素都是变量声明语句
////            if (child is CjNameReferenceExpression) {
////                // 假设声明语句中的第一个子元素是变量名
////                val targetElement = child.firstChild
////                // 检查变量名是否与引用名称匹配
////                if (referenceName == targetElement.text) {
////                    // 创建 ResolveResult 对象并添加到列表中
////                    val resolveResult = PsiElementResolveResult(targetElement)
////                    resolveResults.add(resolveResult)
////                }
////            }
////        }
////        // 返回所有 ResolveResult 对象的列表
////        return resolveResults
////
////    }
//
//    protected open fun canBeReferenceTo(candidateTarget: PsiElement): Boolean = true
//
//
//    val expression: T
//        get() = element
//
//
//    override val resolver: ResolveCache.PolyVariantResolver<CjReference>
//        get() = CjPolyVariantResolver
//}
//
//
//abstract class CjSimpleReference<T : CjReferenceExpression>(expression: T) : AbstractCjReference<T>(expression)
interface CjReference : PsiPolyVariantReference {

    override fun getElement(): CjElement

    override fun resolve(): CjElement?

    fun multiResolve(): List<CjElement>
}




