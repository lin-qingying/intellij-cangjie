package com.linqingying.cangjie.doc.parser

import com.linqingying.cangjie.utils.toUpperCaseAsciiOnly


enum class CDocKnownTag(val isReferenceRequired: Boolean, val isSectionStart: Boolean) {
    AUTHOR(false, false),
    THROWS(true, false),
    EXCEPTION(true, false),
    PARAM(true, false),
    RECEIVER(false, false),
    RETURN(false, false),
    SEE(true, false),
    SINCE(false, false),
    CONSTRUCTOR(false, true),
    PROPERTY(true, true),
    SAMPLE(true, false),
    SUPPRESS(false, false);


    companion object {
        fun findByTagName(tagName: CharSequence): CDocKnownTag? {
            val name = if (tagName.startsWith('@')) {
                tagName.subSequence(1, tagName.length)
            } else tagName
            try {
                return valueOf(name.toString().toUpperCaseAsciiOnly())
            } catch (ignored: IllegalArgumentException) {
            }

            return null
        }
    }
}
