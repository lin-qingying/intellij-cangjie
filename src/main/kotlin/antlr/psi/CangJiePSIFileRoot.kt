//package antlr.psi
//
//import com.huawei.cangjie.icon.CangJieIcons
//import com.huawei.cangjie.lang.CangJieFileType
//import com.huawei.cangjie.lang.CangJieLanguage
//import com.intellij.extapi.psi.PsiFileBase
//import com.intellij.openapi.fileTypes.FileType
//import com.intellij.psi.FileViewProvider
//import com.intellij.psi.PsiElement
//import com.intellij.psi.PsiNamedElement
//import org.antlr.intellij.adaptor.psi.ScopeNode
//import javax.swing.Icon
//
//class CangJiePSIFileRoot(
//    viewProvider: FileViewProvider
//) : PsiFileBase
//    (
//    viewProvider, CangJieLanguage
//), ScopeNode {
//    override fun getContext(): ScopeNode? = null
//
//    override fun getFileType(): FileType {
//        return CangJieFileType
//    }
//
//    override fun resolve(element: PsiNamedElement?): PsiElement? {
//        return null
//    }
//
//    override fun getElementIcon(flags: Int): Icon {
//        return CangJieIcons.CANGJIE_FILE
//    }
//
//    override fun toString(): String {
//        return "CangJie Languge File"
//    }
//}
