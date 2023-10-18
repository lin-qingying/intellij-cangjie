package com.huawei.cangjie1.name


class Name private constructor(private val name: String, val isSpecial: Boolean) : Comparable<Name> {

    fun asString(): String {
        return name
    }

    val identifier: String
        get() {
            check(!isSpecial) { "not identifier: $this" }
            return asString()
        }

    fun asStringStripSpecialMarkers(): String {
        return if (isSpecial) asString().substring(1, asString().length - 1) else asString()
    }

    override operator fun compareTo(that: Name): Int {
        return name.compareTo(that.name)
    }

    val identifierOrNullIfSpecial: String?
        get() = if (isSpecial) null else asString()

    override fun toString(): String {
        return name
    }


    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is Name) return false
        val name1 = o
        if (isSpecial != name1.isSpecial) return false
        return name == name1.name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + if (isSpecial) 1 else 0
        return result
    }

    companion object {
        @JvmStatic
        fun identifier(name: String): Name {
            return Name(name, false)
        }
        @JvmStatic
        fun isValidIdentifier(name: String): Boolean {
            if (name.isEmpty() || name.startsWith("<")) return false
            for (i in 0 until name.length) {
                val ch = name[i]
                if (ch == '.' || ch == '/' || ch == '\\') {
                    return false
                }
            }
            return true
        }
        @JvmStatic
        fun identifierIfValid(name: String): Name? {
            return if (!isValidIdentifier(name)) null else identifier(name)
        }

        @JvmStatic
        fun special(name: String): Name {
            require(name.startsWith("<")) { "special name must start with '<': $name" }
            return Name(name, true)
        }
        @JvmStatic
        fun guessByFirstCharacter(name: String): Name {
            return if (name.startsWith("<")) {
                special(name)
            } else {
                identifier(name)
            }
        }




    }
}

