package com.huawei.cangjie.builtins

//import com.huawei.cangjie.resolve.lazy.declarations.impl.PackageFragmentDescriptorBasicImpl
//import com.huawei.cangjie.resolve.lazy.declarations.impl.craetePackageFragmentDescriptor

//import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import com.google.protobuf.ExtensionRegistry
//import com.google.protobuf.util.JsonFormat
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.PackageFragmentDescriptor
import com.huawei.cangjie.descriptors.PackageFragmentProvider
import com.huawei.cangjie.descriptors.PackageFragmentProviderImpl
//import com.huawei.cangjie.metadata.ProtoBuf
//import com.huawei.cangjie.metadata.builtins.BuiltInsBinaryVersion
//import com.huawei.cangjie.metadata.builtins.BuiltInsProtoBuf
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.resolve.lazy.declarations.impl.craetePackageFragmentDescriptor
import com.huawei.cangjie.storage.StorageManager
import java.io.InputStream
import java.util.*


interface BuiltInsLoader {
    fun createPackageFragmentProvider(
        storageManager: StorageManager,
        builtInsModule: ModuleDescriptor,
//        classDescriptorFactories: Iterable<ClassDescriptorFactory>,
//        platformDependentDeclarationFilter: PlatformDependentDeclarationFilter,
//        additionalClassPartsProvider: AdditionalClassPartsProvider,
        isFallback: Boolean
    ): PackageFragmentProvider

    companion object {
        val Instance: BuiltInsLoader by lazy(LazyThreadSafetyMode.PUBLICATION) {
            val implementations = ServiceLoader.load(BuiltInsLoader::class.java, BuiltInsLoader::class.java.classLoader)
            implementations.firstOrNull() ?: throw IllegalStateException(
                "No BuiltInsLoader implementation was found. Please ensure that the META-INF/services/ is not stripped " +
                        "from your application and that the Java virtual machine is not running under a security manager"
            )
        }
    }
}

//fun InputStream.readBuiltinsPackageFragment(): Pair<ProtoBuf.PackageFragment?, BuiltInsBinaryVersion> =
//    use { stream ->
//        val version = BuiltInsBinaryVersion.readFrom(stream)
//        val proto =
//            if (version.isCompatibleWithCurrentCompilerVersion()) {
//
//                ProtoBuf.PackageFragment.parseFrom(
//                    stream,
////                ExtensionRegistryLite.newInstance().apply(BuiltInsProtoBuf::registerAllExtensions) //轻量级
//                    ExtensionRegistry.newInstance().apply(BuiltInsProtoBuf::registerAllExtensions)
//                )
//            } else null
//
//
//        val printer: JsonFormat.Printer = JsonFormat.printer()
//        val jsonStr: String = printer.print(proto)
//
//        proto to version
//    }

class BuiltInsLoaderImpl : BuiltInsLoader {

    fun createBuiltInPackageFragmentProvider(
        storageManager: StorageManager,
        module: ModuleDescriptor,
        packageFqNames: Set<FqName>,
//        classDescriptorFactories: Iterable<ClassDescriptorFactory>,
//        platformDependentDeclarationFilter: PlatformDependentDeclarationFilter,
//        additionalClassPartsProvider: AdditionalClassPartsProvider = AdditionalClassPartsProvider.None,
        isFallback: Boolean,
        loadResource: (String) -> InputStream?
    ): PackageFragmentProvider {
//        val resourcePath =
//            "kotlin/kotlin.kotlin_builtins"
////            "kotlin/collections/collections.kotlin_builtins"
//        val inputStream =
//            loadResource(resourcePath) ?: throw IllegalStateException("Resource not found in classpath: $resourcePath")
//
//
//        println(inputStream)
//
//        val (proto, version) = inputStream.readBuiltinsPackageFragment()


//        val packageFragments:List<PackageFragmentDescriptor> = packageFqNames.map { fqName ->
//            craetePackageFragmentDescriptor(storageManager,module, fqName)
////            val resourcePath = BuiltInSerializerProtocol.getBuiltInsFilePath(fqName)
//
////            val inputStream = loadResource(resourcePath) ?: throw IllegalStateException("Resource not found in classpath: $resourcePath")
////            BuiltInsPackageFragmentImpl.create(fqName, storageManager, module, inputStream, isFallback)
//        }

        val packageFragments: List<PackageFragmentDescriptor> = packageFqNames.map { fqName ->
            craetePackageFragmentDescriptor(storageManager, module, fqName)

        }

        val provider = PackageFragmentProviderImpl(packageFragments)
//        val provider = PackageFragmentProviderImpl()
//        val notFoundClasses = NotFoundClasses(storageManager, module)
//
//        val components = DeserializationComponents(
//            storageManager,
//            module,
//            DeserializationConfiguration.Default,
//            DeserializedClassDataFinder(provider),
//            AnnotationAndConstantLoaderImpl(module, notFoundClasses, BuiltInSerializerProtocol),
//            provider,
//            LocalClassifierTypeSettings.Default,
//            ErrorReporter.DO_NOTHING,
//            LookupTracker.DO_NOTHING,
//            FlexibleTypeDeserializer.ThrowException,
//            classDescriptorFactories,
//            notFoundClasses,
//            ContractDeserializer.DEFAULT,
//            additionalClassPartsProvider,
//            platformDependentDeclarationFilter,
//            BuiltInSerializerProtocol.extensionRegistry,
//            samConversionResolver = SamConversionResolverImpl(storageManager, samWithReceiverResolvers = emptyList())
//        )
//
//        for (packageFragment in packageFragments) {
//            packageFragment.initialize(components)
//        }

        return provider
    }

    private val resourceLoader = BuiltInsResourceLoader()

    override fun createPackageFragmentProvider(
        storageManager: StorageManager,
        builtInsModule: ModuleDescriptor,
        isFallback: Boolean
    ): PackageFragmentProvider {
        return createBuiltInPackageFragmentProvider(
            storageManager,
            builtInsModule,
            StandardNames.ALL_NAMES,
//            classDescriptorFactories,
//            platformDependentDeclarationFilter,
//            additionalClassPartsProvider,
            isFallback,
            resourceLoader::loadResource
        )
    }
}

class BuiltInsResourceLoader {
    fun loadResource(path: String): InputStream? {
        val classLoader = this::class.java.classLoader ?: return ClassLoader.getSystemResourceAsStream(path)

        // Do not use getResourceAsStream because URLClassLoader's implementation creates InputStream instances which refer to
        // a globally cached JarFile instance, which is closed as soon as URLClassLoader is closed, which instantly invalidates all
        // input streams referring to that JarFile and breaks kotlin-reflect in case it's used from different class loaders.
        val resource = classLoader.getResource(path) ?: return null
        return resource.openConnection().apply { useCaches = false }.getInputStream()
    }
}

