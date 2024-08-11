package com.huawei.cangjie.resolve.lazy.descriptors

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.storage.StorageManager

class ClassResolutionScopesSupport(
    private val classDescriptor: ClassDescriptor,
    storageManager: StorageManager,
    private val languageVersionSettings: LanguageVersionSettings,
    private val getOuterScope: () -> LexicalScope
)
