package spp.jetbrains.sourcemarker.command

import javax.swing.Icon

/**
 * todo: description.
 *
 * @since 0.3.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
interface AutocompleteFieldRow {
    fun getText(): String
    fun getDescription(): String?
    fun getIcon(): Icon?
}
