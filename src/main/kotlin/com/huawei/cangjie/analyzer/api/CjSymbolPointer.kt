package com.huawei.cangjie.analyzer.api

import com.huawei.cangjie.analyzer.CjAnalysisSession
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * `CjSymbol` is valid only during read action it was created in
 * To pass the symbol from one read action to another the CjSymbolPointer should be used
 *
 * We can restore the symbol
 *  * for symbol which came from Kotlin source it will be restored based on [com.intellij.psi.SmartPsiElementPointer]
 *  * restoring symbols which came from Java source is not supported yet
 *  * for library symbols:
 *    * for function & property symbol if its signature was not changed
 *    * for local variable symbol if code block it was declared in was not changed
 *    * for class & type alias symbols if its qualified name was not changed
 *    * for package symbol if the package is still exists
 *
 * @see org.jetbrains.kotlin.analysis.api.lifetime.CjReadActionConfinementLifetimeToken
 */
public abstract class CjSymbolPointer<out S : CjSymbol> {
    /**
     * @return restored symbol (possibly the new symbol instance) if one is still valid, `null` otherwise
     *
     * Consider using [org.jetbrains.kotlin.analysis.api.CjAnalysisSession.restoreSymbol]
     */
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.CjAnalysisSession.restoreSymbol")
    public abstract fun restoreSymbol(analysisSession: CjAnalysisSession): S?

    /**
     * @return **true** if [other] pointer can be restored to the same symbol. The operation is symmetric and transitive.
     */
    public open fun pointsToTheSameSymbolAs(other: CjSymbolPointer<CjSymbol>): Boolean = this === other

    override fun toString(): String = renderAsDataClassToString()
}

public fun Any.renderAsDataClassToString(): String = prettyPrint {
    append(this@renderAsDataClassToString::class.qualifiedName)
    append("(")
    printCollection(this@renderAsDataClassToString::class.declaredMemberProperties) { property ->
        append(property.name)
        append(": ")
        val getter = property.getter
        try {
            getter.isAccessible = true
            append(getter.call(this@renderAsDataClassToString).toString())
        } catch (e: InvocationTargetException) {
            append("ERROR_RENDERING_FIELD")
        } catch (_: Exception) {
            println("")
        }
    }
    append(")")
}
