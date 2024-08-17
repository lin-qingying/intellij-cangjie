//package com.huawei.cangjie.psi
//
//import com.huawei.cangjie.lexer.CjTokens
//import com.huawei.cangjie.name.FqName
//import com.huawei.cangjie.name.Name
//import com.huawei.cangjie.psi.psiUtil.CangJieImportField
//import com.huawei.cangjie.psi.stubs.CangJieImportDirectiveStub
//import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
//import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
//import com.huawei.cangjie.psi.stubs.elements.CjTokenSets
//import com.huawei.cangjie.resolve.ImportPath
//import com.intellij.lang.ASTNode
//import com.intellij.psi.util.PsiTreeUtil
//import java.util.*
//import kotlin.concurrent.Volatile
//
//class CjImportDirective : CjExpressionImplStub<CangJiePlaceHolderStub<CjImportDirective>>/*, CjImportInfo*/ {
//    //获取该导入语句的多个语句
//    val cangJieImportFieldList: MutableList<CangJieImportField> = mutableListOf()
//
//    @Volatile
//    var _importedFqName: FqName? = null
////
//    @get:IfNotParsed
//    @Volatile
//    var importedFqNames: List<FqName?>? = null
//        //    @Override
//        get() {
//            //        CangJieImportDirectiveStub stub = getStub();
//            //        if (stub != null) {
//            //            return stub.getImportedFqNames();
//            //        }
//
//            var importedFqNames = field
//            if (importedFqNames != null) return importedFqNames
//            val importedReference = importedReferences
//            // in case it's not parsed
//            if (importedReference.size <= 0) return null
//
//            importedFqNames = fqNameFromExpressions(importedReference)
//            field = importedFqNames
//            return importedFqNames
//        }
//        private set
//
//    constructor(node: ASTNode) : super(node)
//
//
//    constructor(stub: CangJiePlaceHolderStub<CjImportDirective>) : super(stub, CjStubElementTypes.IMPORT_DIRECTIVE)
//
////      val aliasName: String?
////        get() {
////            val alias = alias
////            return alias?.name
////        }
////
////    val alias: CjImportAlias?
////        get() = getStubOrPsiChild(CjStubElementTypes.IMPORT_ALIAS)
//
////    val importDirectiveItem: Mu<CjImportDirectiveItem?>
////        get() = getStubOrPsiChildren(
////            CjStubElementTypes.IMPORT_DIRECTIVE_ITEM,
////            CjExpression.ARRAY_FACTORY
////        ) as Array<CjImportDirectiveItem?>
//
//    //
//    //    @Nullable
//    //    public CjImportAlias[] getAliass() {
//    //        return getStubOrPsiChild(CjStubElementTypes.IMPORT_ALIAS );
//    //    }
//    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
//        return visitor.visitImportDirective(this, data)
//    }
//
//    @get:IfNotParsed
//    val importedReferences: Array<CjExpression?>
//        /**
//         * 一条导入语句可能导入了多个包或者类
//         * 需要拆分
//         *
//         * @return
//         */
//        get() = getStubOrPsiChildren(CjTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS, CjExpression.ARRAY_FACTORY)
//
//    private fun fqNameFromExpressions(importedReference: Array<CjExpression?>): List<FqName?> {
//        val fqNames: MutableList<FqName?> = ArrayList()
//
//        for (cjExpression in importedReference) {
//            if (cjExpression is CjImportDirectiveItem) {
//                val s = cjExpression.importedReferences
//                if (s.size > 1 && s[0] !is CjImportDirectiveItem && s[1] is CjImportDirectiveItem) {
////                    先处理前缀 ，在处理后面的内容
//                    val packageHead = fqNameFromExpression(s[0])
//                    //                    去掉第一个
//                    val tempexpr = Arrays.copyOfRange(s, 1, s.size)
//
//                    for (expression in tempexpr) {
//                        for (name in fqNameFromExpressions(arrayOf(expression))) {
//                            if (packageHead != null) {
//                                val name1 = packageHead.child(name!!)
//
//
//                                val aliasName =( expression as CjImportDirectiveItem).aliasName
//                                //                            cangJieImportFields
////                            CjPsiUtilKt.addIf(cangJieImportFields,);
//                                cangJieImportFieldList.add(CangJieImportField(
//                                    name1,
//                                    aliasName,
//                                    expression.isAllUnder,
//expression.importContent
//                                ))
//                                fqNames.add(name1)
//                            }
//                        }
//                    }
//                } else {
//                    fqNames.addAll(fqNameFromExpressions(s))
//                }
//            } else {
//                val fqName = fqNameFromExpression(cjExpression)
//                if (fqName != null) fqNames.add(fqName)
//            }
//        }
//        return fqNames
//    }
//
//    @get:IfNotParsed
//    val importedReference: CjExpression?
//        get() {
//            val references = importedReferences
//            if (references.size > 0) {
//                return references[0]
//            }
//            return null
//        }
//
////    override val isAllUnder: Boolean
////        get() {
////            val stub = stub
////            if (stub != null) {
////                return stub.isAllUnder()
////            }
////            return node.findChildByType(CjTokens.MUL) != null
////        }
//
////    override val importContent: CjImportInfo.ImportContent?
////        get() {
////            val reference = importedReference ?: return null
////            return CjImportInfo.ImportContent.ExpressionBased(reference)
////        }
//
////    @get:IfNotParsed
////    override val importedFqName: FqName?
////        get() {
////            val stub = stub
////            if (stub != null) {
////                return stub.getImportedFqName()
////            }
////
////            var importedFqName = this._importedFqName
////            if (importedFqName != null) return importedFqName
////            val importedReference = importedReference ?: return null
////            // in case it's not parsed
////
////            importedFqName = fqNameFromExpression(importedReference)
////            this._importedFqName = importedFqName
////            return importedFqName
////        }
//
//    @get:IfNotParsed
//    val importPath: ImportPath?
//        get() {
//            val importFqn = _importedFqName ?: return null
//
//            val alias: Name? = null
//
//            //        String aliasName = getAliasName();
//            //        if (aliasName != null) {
//            //            alias = Name.identifier(aliasName);
//            //        }
//            return ImportPath(importFqn, isAllUnder, alias)
//        }
//
//    val isValidImport: Boolean
//        get() {
//            val stub = stub
//            if (stub != null) {
//                return stub.isValid()
//            }
//            return !PsiTreeUtil.hasErrorElements(this)
//        }
//
//    override fun subtreeChanged() {
//        super.subtreeChanged()
//        _importedFqName = null
//    }
//
//    val isPublic: Boolean
//        get() = false
//    //        return getStubOrPsiChild(CjStubElementTypes.IMPORT_PUBLIC) != null;
//    //
//    //    @Nullable
//    //    @Override
//    //    public Name getImportedName() {
//    //        return CjImportInfo.super.getImportedName();
//    //    }
//
//
//    companion object {
//        private fun fqNameFromExpression(expression: CjExpression?): FqName? {
//            if (expression == null) {
//                return null
//            }
//
//
//            if (expression is CjDotQualifiedExpression) {
//                val parentFqn = fqNameFromExpression(expression.receiverExpression)
//                val child = nameFromExpression(expression.selectorExpression)
//                    ?: return parentFqn
//                if (parentFqn != null) {
//                    return parentFqn.child(child)
//                }
//                return null
//            } else if (expression is CjSimpleNameExpression) {
//                return FqName.topLevel(expression.getReferencedNameAsName())
//            } else {
//                throw IllegalArgumentException("Can't construct fqn for: " + expression.javaClass)
//            }
//        }
//
//        private fun nameFromExpression(expression: CjExpression?): Name? {
//            if (expression == null) {
//                return null
//            }
//
//            if (expression is CjSimpleNameExpression) {
//                return expression.getReferencedNameAsName()
//            } else {
//                throw IllegalArgumentException("Can't construct name for: " + expression.javaClass)
//            }
//        }
//    }
//}
