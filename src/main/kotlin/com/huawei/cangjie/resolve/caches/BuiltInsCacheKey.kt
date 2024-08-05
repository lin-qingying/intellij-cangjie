package com.huawei.cangjie.resolve.caches

import com.huawei.cangjie.analyzer.ModuleInfo

interface BuiltInsCacheKey {
    object DefaultBuiltInsKey : BuiltInsCacheKey
}

class CangJieModuleBuiltInsKey(moduleInfo: ModuleInfo) : BuiltInsCacheKey


fun ModuleInfo. getKeyForBuiltIns():BuiltInsCacheKey{

    return CangJieModuleBuiltInsKey(this)

}
