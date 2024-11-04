package com.linqingying.cangjie.serialization.deserialization

interface MetadataPartProvider {
    /**
     * @return simple names of .kotlin_metadata files that store data for top level declarations in the package with the given FQ name
     */
    fun findMetadataPackageParts(packageFqName: String): List<String>

    object Empty : MetadataPartProvider {
        override fun findMetadataPackageParts(packageFqName: String): List<String> = emptyList()
    }
}
