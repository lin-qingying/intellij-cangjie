package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.lang.CangJieFileType
import com.huawei.cangjie.lang.CangJieLanguage

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.stubs.CangJieFileStub
import com.huawei.cangjie.psi.stubs.elements.CjPlaceHolderStubElementType
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.huawei.cangjie.psi.stubs.elements.CjTokenSets
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.psi.*
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayFactory

 open class CjFile(viewProvider: FileViewProvider, val isCompiled: Boolean = false) :
    PsiFileBase(viewProvider, CangJieLanguage),
//    PsiClassOwner,
    PsiNamedElement,
//    PsiModifiableCodeBlock,
    CjDeclarationContainer,
    CjElement {
    override fun getFileType(): FileType = CangJieFileType
//    override fun getClasses(): Array<PsiClass> {
//        val fileClassProvider = project.getService(CjFileClassProvider::class.java)
//        return fileClassProvider?.getFileClasses(this) ?: PsiClass.EMPTY_ARRAY
//    }

//    override fun getPackageName(): String    = packageFqName.asString()
//    override fun setPackageName(packageName: String?) {
////        TODO("更改包名  完全限定名")
//    }

//    override fun shouldChangeModificationCount(place: PsiElement?): Boolean = false
    override fun <D> acceptChildren(visitor: CjVisitor<Void, D>, data: D) {
        CjPsiUtil.visitChildren(this, visitor, data)
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R =
        visitor.visitCjFile(this, data)

    protected open val importLists: List<CjImportList>
        get() = findChildrenByTypeOrClass(CjStubElementTypes.IMPORT_LIST, CjImportList::class.java).asList()

    fun hasImportAlias(): Boolean {
        val hasImportAlias = hasImportAlias
        if (hasImportAlias != null) return hasImportAlias

        val newValue = importLists.any(CjImportList::computeHasImportAlias)
        this.hasImportAlias = newValue
        return newValue
    }

    override fun toString(): String {
        return "CangJie File: $name"
    }

    override fun getPsiOrParent(): CjElement = this
    open val importDirectives: List<CjImportDirective>
        get() = importLists.flatMap { it.imports }

    override fun getContainingCjFile(): CjFile = this

    fun findImportByAlias(name: String): CjImportDirective? {
        if (!hasImportAlias()) return null

        return importDirectives.firstOrNull { name == it.aliasName }
    }

    fun <T : CjElementImplStub<out StubElement<*>>> findChildrenByTypeOrClass(
        elementType: CjPlaceHolderStubElementType<T>,
        elementClass: Class<T>
    ): Array<out T> {
        val stub = stub
        if (stub != null) {
            val arrayFactory: ArrayFactory<T> = elementType.arrayFactory
            return stub.getChildrenByType(elementType, arrayFactory)
        }
        return findChildrenByClass(elementClass)
    }

    val packageFqNameByTree: FqName
        get() = packageDirectiveByTree?.fqName ?: FqName.ROOT


    private val packageDirectiveByTree: CjPackageDirective?
        get() {
            val ast = node.findChildByType(CjNodeTypes.PACKAGE_DIRECTIVE)
            return if (ast != null) ast.psi as CjPackageDirective else null
        }

    override fun getStub(): CangJieFileStub? {
        if (virtualFile !is VirtualFileWithId) return null
        val stub = super.getStub()
        if (stub is CangJieFileStub?) {
            return stub
        }

        error("Illegal stub for CjFile: type=${this.javaClass}, stub=${stub?.javaClass} name=$name")
    }

    val packageDirective: CjPackageDirective?
        get() {
            val stub = stub
            if (stub != null) {
                val packageDirectiveStub = stub.findChildStubByType(CjStubElementTypes.PACKAGE_DIRECTIVE)
                return packageDirectiveStub?.psi
            }
            return packageDirectiveByTree
        }


    @Volatile
    private var hasImportAlias: Boolean? = null


    override val declarations: List<CjDeclaration>
        get() = stub?.getChildrenByType(FILE_DECLARATION_TYPES, CjDeclaration.ARRAY_FACTORY)?.toList()
            ?: PsiTreeUtil.getChildrenOfTypeAsList(this, CjDeclaration::class.java)

    var packageFqName: FqName
        get() = stub?.getPackageFqName() ?: packageFqNameByTree
        set(value) {
            val packageDirective = packageDirective
            if (packageDirective != null) {
                packageDirective.fqName = value
            } else {
                val newPackageDirective = CjPsiFactory(project).createPackageDirectiveIfNeeded(value) ?: return
                addAfter(newPackageDirective, null)
            }
        }


    companion object {
        val FILE_DECLARATION_TYPES = TokenSet.orSet(CjTokenSets.DECLARATION_TYPES)
    }
}

private fun CjImportList.computeHasImportAlias(): Boolean {
    var child: PsiElement? = firstChild
    while (child != null) {
        if (child is CjImportDirective && child.alias != null) {
            return true
        }

        child = child.nextSibling
    }

    return false
}

