package com.huawei.cangjie.ide.completion.back

import com.intellij.patterns.ElementPattern
import com.intellij.patterns.StandardPatterns

fun singleCharPattern(char: Char) = StandardPatterns.character().equalTo(char)
infix fun <T> ElementPattern<T>.or(rhs: ElementPattern<T>) = StandardPatterns.or(this, rhs)

fun cangjieIdentifierPartPattern(): ElementPattern<Char> =
    StandardPatterns.character().javaIdentifierPart().andNot(singleCharPattern('$')) or singleCharPattern('@')
