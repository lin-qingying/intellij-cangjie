package com.linqingying.cangjie.ide.actions.runAnything

import com.linqingying.cangjie.cjpm.CjpmCommands
import javax.swing.Icon



class RunAnythingCjpmItem(command: String, icon: Icon) : CjRunAnythingItem(command, icon) {
    override val helpCommand: String = "cjpm"

    override val commandDescriptions: Map<String, String> =
        CjpmCommands.entries.associate { it.presentableName to it.description }

    override fun getOptionsDescriptionsForCommand(commandName: String): Map<String, String>? {
        val command = CjpmCommands.entries.find { it.presentableName == commandName } ?: return null
        return command.options.associate { it.longName to it.description }
    }
}
