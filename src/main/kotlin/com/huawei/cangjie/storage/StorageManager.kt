package com.huawei.cangjie.storage

interface StorageManager{

    fun <T> compute(computable: () -> T): T

}