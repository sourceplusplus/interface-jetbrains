package com.sourceplusplus.sourcemarker.command

import javax.swing.Icon

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface AutocompleteFieldRow {
    fun getText(): String
    fun getDescription(): String?
    fun getIcon(): Icon?
}
