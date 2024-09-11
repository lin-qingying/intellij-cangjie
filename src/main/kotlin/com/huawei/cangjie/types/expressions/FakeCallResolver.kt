package com.huawei.cangjie.types.expressions

import com.huawei.cangjie.resolve.calls.CallResolver
import com.intellij.openapi.project.Project

class FakeCallResolver(
    private val project: Project,
    private val callResolver: CallResolver
)
