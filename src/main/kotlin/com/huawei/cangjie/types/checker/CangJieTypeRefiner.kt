package com.huawei.cangjie.types.checker

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.types.AbstractTypeRefiner


@DefaultImplementation(impl = AbstractTypeRefiner.Default::class)
abstract class CangJieTypeRefiner : AbstractTypeRefiner()