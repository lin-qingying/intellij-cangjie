package com.huawei.cangjie.ide.codeinsight

import com.huawei.cangjie.lexer.CangJieLexer
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.psiUtil.isIdentifier
import com.huawei.cangjie.psi.psiUtil.unquoteCangJieIdentifier
import com.huawei.cangjie.utils.decapitalizeSmart

class CangJieNameSuggester {

    companion object {
        private val ACCESSOR_PREFIXES = arrayOf("get", "is", "set")
        private const val MAX_NUMBER_OF_SUGGESTED_NAME_CHECKS = 1000

        private fun extractIdentifiers(s: String): String {
            return buildString {
                val lexer = CangJieLexer()
                lexer.start(s)
                while (lexer.tokenType != null) {
                    if (lexer.tokenType == CjTokens.IDENTIFIER) {
                        append(lexer.tokenText)
                    }
                    lexer.advance()
                }
            }
        }

        private fun cutAccessorPrefix(name: String): String? {
            if (name === "" || !name.unquoteCangJieIdentifier().isIdentifier()) return null
            val s = extractIdentifiers(name)

            for (prefix in ACCESSOR_PREFIXES) {
                if (!s.startsWith(prefix)) continue

                val len = prefix.length
                if (len < s.length && Character.isUpperCase(s[len])) {
                    return s.substring(len)
                }
            }

            return s
        }

        /**
         * Decapitalizes the passed [name] if [mustStartWithLowerCase] is `true`, checks whether the result is a valid identifier,
         * validates it using [validator], and improves it by adding a numeric suffix in case of conflicts.
         */
        fun suggestNameByValidIdentifierName(
            name: String?,
            validator: (String) -> Boolean,
            mustStartWithLowerCase: Boolean = true
        ): String? {
            if (name == null) return null
            if (mustStartWithLowerCase) return suggestNameByValidIdentifierName(
                name.decapitalizeSmart(),
                validator,
                false
            )
            val correctedName = when {
                name.isIdentifier() -> name
                name == "class" -> "clazz"
                else -> return null
            }
            return suggestNameByName(correctedName, validator)
        }

        /**
         * Validates [name] and slightly improves it by adding a numeric suffix in case of conflicts.
         *
         * @param name to check in scope
         * @return [name] or nameI, where I is an integer
         */
        fun suggestNameByName(name: String, validator: (String) -> Boolean): String {
            if (validator(name)) return name
            var i = 1
            while (i <= MAX_NUMBER_OF_SUGGESTED_NAME_CHECKS && !validator(name + i)) {
                ++i
            }

            return name + i
        }

        fun getCamelNames(
            name: String,
            validator: (String) -> Boolean,
            startLowerCase: Boolean = true
        ): Sequence<String> {
            val s = cutAccessorPrefix(name) ?: return emptySequence()

            var upperCaseLetterBefore = false
            return sequence {
                for (i in s.indices) {
                    val c = s[i]
                    val upperCaseLetter = Character.isUpperCase(c)

                    if (i == 0) {
                        suggestNameByValidIdentifierName(s, validator, startLowerCase)?.let { yield(it) }
                    } else {
                        if (upperCaseLetter && !upperCaseLetterBefore) {
                            val substring = s.substring(i)
                            suggestNameByValidIdentifierName(substring, validator, startLowerCase)?.let { yield(it) }
                        }
                    }

                    upperCaseLetterBefore = upperCaseLetter
                }
            }
        }

    }
}
