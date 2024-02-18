package com.huawei.cangjie.cjpm.project.model.impl

import com.huawei.cangjie.cjpm.project.model.CjpmProjectsService
import com.intellij.openapi.vfs.newvfs.BulkFileListener

/**
 *文件更改监听器，检测`module.json`文件内部的更改
 *和创建`*.cj`文件作
 */
class CjpmJsonWatcher(
    private val cjpmProjects: CjpmProjectsService,
    private val onCjpmJsonChange: () -> Unit
) : BulkFileListener