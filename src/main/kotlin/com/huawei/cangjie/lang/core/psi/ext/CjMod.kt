package com.huawei.cangjie.lang.core.psi.ext

import com.huawei.cangjie.lang.core.psi.CjFile
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDirectory

interface CjMod :CjQualifiedNamedElement{


    val `super`: CjMod?

    /**。
     *XXX：这可能与[com.intellij.psi.PsiNamedElement.getName]不同。
     *。
     *此扭曲是必需的，因为[org.rust.lang.core.psi.CjFile]是 [CjMod]，但不应该覆盖它的名称。
     */
    val modName: String?

    /**。
     *返回该模块相关的`path`属性值。
     *如果模块没有`path`属性，则返回NULL。
     *。
     *注意，如果是非内联模块(即通过`mod foo；`声明)。
     *`path`属性属于模块声明，但不属于模块项本身。
     */
    val pathAttribute: String?

    val ownsDirectory: Boolean



    val isCrateRoot: Boolean
}
val CjMod.superMods: List<CjMod>
    get() {
        // For malformed programs, chain of `super`s may be infinite
        // because of cycles, and we need to detect this situation.
        val visited = HashSet<CjMod>()
        return generateSequence(this) { it.`super` }
            .takeWhile { visited.add(it) }
            .toList()
    }

