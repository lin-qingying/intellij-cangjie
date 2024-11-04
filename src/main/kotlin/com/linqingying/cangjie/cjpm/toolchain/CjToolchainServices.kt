package com.linqingying.cangjie.cjpm.toolchain

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*


/**
 * 该类存储历史所有可用的工具链路径
 */
@Service
@State(name = "cangjie-toolchains-state", reloadable = false, storages = [Storage("cangjie-toolchains-state.xml")])
class CjToolchainServices : PersistentStateComponent<CjToolchainServices.State> {
    companion object {
        fun getInstance(): CjToolchainServices =
            ApplicationManager.getApplication().getService(CjToolchainServices::class.java)
    }

    class State() : BaseState() {

        constructor(toolchains: MutableList<String>) : this() {
            this.toolchains = toolchains
        }


        var toolchains by list<String>()

    }


    var myState = State(mutableListOf())

    var toolchains = mutableListOf<CjToolchainBase>()


    fun putToolchainPath(path: String) {
        state.toolchains.map {

            if (it == path) {
                return
            }

        }


        state.toolchains.add(path)
    }

    fun getToolchainPaths(): MutableList<String> {
        return state.toolchains
    }

    fun removeToolchain(path: String) {

        state.toolchains.removeIf {
            it == path
        }


    }
//
//    var toolchains = mutableListOf<CjToolchainBase>()
//
//
//    fun putToolchainPath(toolchain: CjToolchainBase) {
//
//
//        toolchains.map {
//            if (it.location == toolchain.location) {
//                return
//            }
//        }
//        toolchains.add(toolchain)
//
//        myState.toolchains.add(toolchain.toSerializedString())
//    }
//
//    fun getToolchainPaths(): MutableList<CjToolchainBase> {
//        return toolchains
//    }
//
//    fun removeToolchain(toolchain: CjToolchainBase) {
//
//        toolchains.removeIf {
//            it.location == toolchain.location
//        }
//
//        myState.toolchains.removeIf {
//            toolchain.toSerializedString() == it
//        }
//    }

//    private fun onStateChanged() {
//        try {
//            toolchains = myState.toolchains.map {
//                CjToolchainBase.fromSerializedString(it)!!
//            }.toMutableList()
//        } catch (e: Exception) {
////            TODO 在重新序列化时捕获null错误是否需要处理
//
////            toolchains = mutableListOf()
////            myState.toolchains = mutableListOf()
//        }
//    }

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        this.myState = state
//        onStateChanged()
    }


}
