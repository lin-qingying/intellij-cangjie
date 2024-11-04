package com.linqingying.cangjie.config

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.SmartList
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.serialization.deserialization.ClassData
import com.linqingying.cangjie.serialization.deserialization.DeserializationConfiguration
import com.linqingying.cangjie.serialization.deserialization.MetadataPartProvider


interface PackagePartProvider {
    /**
     * @return JVM internal names of package parts existing in the package with the given FQ name.
     *
     * For example, if a file named foo.Ch in package org.test is compiled to a library, PackagePartProvider for such library
     * must return the list `["org/test/FooCj"]` for the query `"org.test"`
     * (in case the file is not annotated with @CangJieName, @CangJiePackageName or @CangJieMultifileClass).
     */
    fun findPackageParts(packageFqName: String): List<String>

    /**
     * This method is only for sake of optimization
     * @return package names set for which that provider has package parts
     */
    fun computePackageSetWithNonClassDeclarations(): Set<String>

    fun getAnnotationsOnBinaryModule(moduleName: String): List<ClassId>

    fun getAllOptionalAnnotationClasses(): List<ClassData>

    /**
     * Returns `true` if [getAllOptionalAnnotationClasses] may return a non-empty list.
     */
    fun mayHaveOptionalAnnotationClasses(): Boolean

    object Empty : PackagePartProvider {
        override fun findPackageParts(packageFqName: String): List<String> = emptyList()

        override fun getAnnotationsOnBinaryModule(moduleName: String): List<ClassId> = emptyList()

        override fun getAllOptionalAnnotationClasses(): List<ClassData> = emptyList()

        override fun mayHaveOptionalAnnotationClasses(): Boolean = false

        override fun computePackageSetWithNonClassDeclarations(): Set<String> = emptySet()
    }
}

interface PackageAndMetadataPartProvider : PackagePartProvider, MetadataPartProvider

abstract class CangJiePackagePartProviderBase<MappingsKey> : PackageAndMetadataPartProvider {

//    protected data class ModuleMappingInfo<MappingsKey>(val key: MappingsKey, val mapping: ModuleMapping, val name: String)

//    protected abstract val loadedModules: MutableList<ModuleMappingInfo<MappingsKey>>

//    abstract val deserializationConfiguration : DeserializationConfiguration

    override fun findPackageParts(packageFqName: String): List<String> {
//        val rootToPackageParts: Collection<PackageParts> = getPackageParts(packageFqName)
//        if (rootToPackageParts.isEmpty()) return emptyList()

//        val result = linkedSetOf<String>()
//        val visitedMultifileFacades = linkedSetOf<String>()
//        for (packageParts in rootToPackageParts) {
//            for (name in packageParts.parts) {
//                val facadeName = packageParts.getMultifileFacadeName(name)
//                if (facadeName == null || facadeName !in visitedMultifileFacades) {
//                    result.add(name)
//                }
//            }
//            packageParts.parts.mapNotNullTo(visitedMultifileFacades, packageParts::getMultifileFacadeName)
//        }
//        return result.toList()
        return emptyList()
    }

    //    private val allPackageNames: Set<String> by lazy {
//        loadedModules.flatMapTo(mutableSetOf()) { it.mapping.packageFqName2Parts.keys }
//    }
    override fun computePackageSetWithNonClassDeclarations(): Set<String> {
        return emptySet()
    }

    //    override fun computePackageSetWithNonClassDeclarations(): Set<String> = allPackageNames
    override fun findMetadataPackageParts(packageFqName: String): List<String> = emptyList()
//        getPackageParts(packageFqName).flatMap(PackageParts::metadataParts).distinct()

//    private fun getPackageParts(packageFqName: String): Collection<PackageParts> {
//        val result = mutableMapOf<MappingsKey, PackageParts>()
//        for ((root, mapping) in loadedModules) {
//            val newParts = mapping.findPackageParts(packageFqName) ?: continue
//            result[root]?.let { parts -> parts += newParts } ?: result.put(root, newParts)
//        }
//        return result.values
//    }

    override fun getAnnotationsOnBinaryModule(moduleName: String): List<ClassId> {
        return emptyList()
//        return loadedModules.mapNotNull { (_, mapping, name) ->
//            if (name == moduleName) mapping.moduleData.annotations.map(ClassId::fromString) else null
//        }.flatten()
    }

    override fun getAllOptionalAnnotationClasses(): List<ClassData> = emptyList()
//        loadedModules.flatMap { module ->
//            getAllOptionalAnnotationClasses(module.mapping)
//        }

    override fun mayHaveOptionalAnnotationClasses(): Boolean {
        // `loadedModules` is mutable, so even a package part provider without optional annotation classes may have some in the future.
        return true
    }

    companion object {
//        fun getAllOptionalAnnotationClasses(module: ModuleMapping): List<ClassData> {
//            val data = module.moduleData
//            return data.optionalAnnotations.map { proto ->
//                ClassData(data.nameResolver, proto, module.version, SourceElement.NO_SOURCE)
//            }
//        }
    }
}

class CangJiePackagePartProvider(
    languageVersionSettings: LanguageVersionSettings,
    private val scope: GlobalSearchScope
) : CangJiePackagePartProviderBase<VirtualFile>()
