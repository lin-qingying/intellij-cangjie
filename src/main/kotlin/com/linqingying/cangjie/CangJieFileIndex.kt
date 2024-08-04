package com.linqingying.cangjie

import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.KeyDescriptor

class CangJieFileIndex: FileBasedIndexExtension<A, List<String>>() {
    override fun getName(): ID<A, List<String>> {
        TODO("Not yet implemented")
    }

    override fun getIndexer(): DataIndexer<A, List<String>, FileContent> {
        TODO("Not yet implemented")
    }

    override fun getKeyDescriptor(): KeyDescriptor<A> {
        TODO("Not yet implemented")
    }

    override fun getValueExternalizer(): DataExternalizer<List<String>> {
        TODO("Not yet implemented")
    }

    override fun getVersion(): Int {
        TODO("Not yet implemented")
    }

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        TODO("Not yet implemented")
    }

    override fun dependsOnFileContent(): Boolean {
        TODO("Not yet implemented")
    }
}
  class A(){

}
