package com.linqingying.cangjie.download.stdlib

import kotlin.io.path.Path


const val STDLIB_INDEX_URL = "https://gitee.com/Lin_Qing_Ying/intellij-cangjie-index/raw/master/stdlib/index.json"


//标准库位置
val STDLIB_PATH_LOCAL = Path(System.getProperty("user.home")).resolve(".cangjie").resolve("stdlib")