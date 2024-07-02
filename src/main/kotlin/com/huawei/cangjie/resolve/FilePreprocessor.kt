package com.huawei.cangjie.resolve

import com.google.common.collect.Sets
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.BindingContext.PACKAGE_TO_FILES
import com.huawei.cangjie.utils.slicedMap.WritableSlice

interface FilePreprocessorExtension {
    fun preprocessFile(file: CjFile)
}

fun <K, T> BindingTrace.addElementToSlice(
    slice: WritableSlice<K, MutableCollection<T>>, key: K, element: T
) {
    val elements = get(slice, key) ?: Sets.newIdentityHashSet()
    elements.add(element)
    record(slice, key, elements)
}

class FilePreprocessor(
    private val trace: BindingTrace,
    private val extensions: Iterable<FilePreprocessorExtension>
) {
    fun preprocessFile(file: CjFile) {
        registerFileByPackage(file)

        for (extension in extensions) {
            extension.preprocessFile(file)
        }
    }

    private fun registerFileByPackage(file: CjFile) {
        // Register files corresponding to this package
        // The trace currently does not support bi-di multimaps that would handle this task nicer
        trace.addElementToSlice(PACKAGE_TO_FILES, file.packageFqName, file)
    }
}