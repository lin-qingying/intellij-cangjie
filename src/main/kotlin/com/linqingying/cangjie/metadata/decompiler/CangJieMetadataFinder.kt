package com.linqingying.cangjie.metadata.decompiler

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName
import java.io.InputStream
@DefaultImplementation(CangJieMetadataFinder.Companion.Default::class)
interface CangJieMetadataFinder {
    /**
     * @return an [InputStream] which should be used to load the .kotlin_metadata file for class with the given [classId].
     * [classId] identifies either a real top level class, or a package part (e.g. it can be "foo/bar/_1Kt")
     */
    fun findMetadata(classId: ClassId): InputStream?

    fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String>?

    /**
     * @return `true` iff this finder is able to locate the package with the given [fqName], containing .kotlin_metadata files.
     * Note that returning `true` makes [MetadataPackageFragmentProvider] construct the package fragment for the package,
     * and that fact can alter the qualified name expression resolution in the compiler front-end
     */
    fun hasMetadataPackage(fqName: FqName): Boolean

    /**
     * @return an [InputStream] which should be used to load the .kotlin_builtins file for package with the given [packageFqName].
     */
    fun findBuiltInsData(packageFqName: FqName): InputStream?


    companion object{
        object Default: CangJieMetadataFinder{
            override fun findMetadata(classId: ClassId): InputStream? {
             return null
            }

            override fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String>? {
                return null
            }

            override fun hasMetadataPackage(fqName: FqName): Boolean {
                return false
            }

            override fun findBuiltInsData(packageFqName: FqName): InputStream? {
                return null
            }

        }
    }
}
