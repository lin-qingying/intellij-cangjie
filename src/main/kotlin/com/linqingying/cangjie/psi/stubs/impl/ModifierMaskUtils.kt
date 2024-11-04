package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.lexer.CjKeywordToken
import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.linqingying.cangjie.lexer.CjTokens.MODIFIER_KEYWORDS_ARRAY
import com.linqingying.cangjie.psi.CjModifierList


object ModifierMaskUtils {
    init {
        assert(MODIFIER_KEYWORDS_ARRAY.size <= 64) { "Current implementation depends on the ability to represent modifier list as bit mask" }
    }

    @JvmStatic
    fun computeMaskFromModifierList(modifierList: CjModifierList): Long = computeMask { modifierList.hasModifier(it) }



    @JvmStatic
    fun computeMask(hasModifier: (CjModifierKeywordToken) -> Boolean): Long {
        var mask = 0L
        for ((index, modifierKeywordToken) in MODIFIER_KEYWORDS_ARRAY.withIndex()) {
            if (hasModifier(modifierKeywordToken)) {
                mask = mask or (1L shl index)
            }
        }
        return mask
    }

    @JvmStatic
    fun maskHasModifier(mask: Long, modifierToken: CjKeywordToken): Boolean {
        val index = MODIFIER_KEYWORDS_ARRAY.indexOf(modifierToken)
        assert(index >= 0) { "All CjModifierKeywordTokens should be present in MODIFIER_KEYWORDS_ARRAY" }
        return (mask and (1L shl index)) != 0L
    }

    @JvmStatic
    fun maskToString(mask: Long): String {
        val sb = StringBuilder()
        sb.append("[")
        var first = true
        for (modifierKeyword in MODIFIER_KEYWORDS_ARRAY) {
            if (maskHasModifier(mask, modifierKeyword)) {
                if (!first) {
                    sb.append(" ")
                }
                sb.append(modifierKeyword.value)
                first = false
            }
        }
        sb.append("]")
        return sb.toString()
    }

}
