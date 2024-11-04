
package com.linqingying.cangjie.metadata.serialization

interface StringTable {
    fun getStringIndex(string: String): Int

    /**
     * @param className the fully qualified name of some class in the format: `org/foo/bar/Test.Inner`
     */
    fun getQualifiedClassNameIndex(className: String, isLocal: Boolean): Int

    fun getPackageFqNameIndexByString(fqName: String): Int
}
