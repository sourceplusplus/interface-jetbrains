package com.sourceplusplus.portal

/**
 * todo: description.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object PortalBundle : PortalMessageTranslator {

    var messageTranslator: PortalMessageTranslator? = null

    override fun translate(key: String): String {
        if (messageTranslator != null) {
            return messageTranslator!!.translate(key) ?: key
        }
        return key
    }
}

fun interface PortalMessageTranslator {
    fun translate(key: String): String?
}