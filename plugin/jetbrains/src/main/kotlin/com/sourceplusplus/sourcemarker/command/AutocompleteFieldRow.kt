package com.sourceplusplus.sourcemarker.command

import javax.swing.Icon

interface AutocompleteFieldRow {
    fun getText(): String
    fun getDescription(): String?
    fun getIcon(): Icon?
}
