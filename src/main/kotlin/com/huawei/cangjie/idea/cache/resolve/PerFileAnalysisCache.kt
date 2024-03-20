package com.huawei.cangjie.idea.cache.resolve

import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.ValueDescriptor
import java.lang.reflect.Type


interface ComponentProvider {
    fun resolve(request: Type): ValueDescriptor?
    fun <T> create(request: Class<T>): T
}


internal class PerFileAnalysisCache(val file: CjFile, componentProvider: ComponentProvider)  {

//    private val globalContext = componentProvider.get<GlobalContext>()

}