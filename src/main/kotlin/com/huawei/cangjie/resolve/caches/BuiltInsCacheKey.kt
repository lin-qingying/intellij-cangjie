package com.huawei.cangjie.resolve.caches

import com.huawei.cangjie.analyzer.ModuleInfo

interface BuiltInsCacheKey {
    object DefaultBuiltInsKey : BuiltInsCacheKey
}

class CangJieModuleBuiltInsKey(moduleInfo: ModuleInfo) : BuiltInsCacheKey

private var _builtinsKey: CangJieModuleBuiltInsKey? = null
fun ModuleInfo.getKeyForBuiltIns(): BuiltInsCacheKey {
//    if (_builtinsKey == null) {
//        _builtinsKey = CangJieModuleBuiltInsKey(this)
//    }
//    return _builtinsKey!!

    return CangJieModuleBuiltInsKey(this)
}
