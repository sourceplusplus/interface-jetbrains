package com.sourceplusplus.protocol.utils

/**
 * Useful methods for formatting artifact names.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ArtifactNameUtils {

    fun isArtifactQualifiedName(artifactQualifiedName: String): Boolean {
        return artifactQualifiedName.contains(".") && artifactQualifiedName.contains("(") &&
                artifactQualifiedName.contains(")")
    }

    fun getShortQualifiedClassName(qualifiedName: String?): String {
        return getQualifiedClassName(qualifiedName)!!.replace("\\B\\w+(\\.)".toRegex(), "$1")
    }

    fun getShortQualifiedFunctionName(qualifiedName: String): String {
        return getShortQualifiedClassName(qualifiedName) + "." + getShortFunctionSignature(qualifiedName)
    }

    fun getQualifiedClassName(qualifiedName: String?): String? {
        if (qualifiedName == null || qualifiedName.isEmpty()
            || !qualifiedName.contains(".") || !qualifiedName.contains("(")
        ) {
            return qualifiedName
        }
        var withoutArgs = qualifiedName.substring(0, qualifiedName.indexOf("("))
        if (withoutArgs.contains("<")) {
            withoutArgs = withoutArgs.substring(0, withoutArgs.indexOf("<"))
        }
        return withoutArgs.substring(withoutArgs.lastIndexOf("?") + 1, withoutArgs.lastIndexOf("."))
    }

    fun getClassName(qualifiedMethodName: String?): String? {
        return if (qualifiedMethodName == null
            || qualifiedMethodName.isEmpty()
            || !qualifiedMethodName.contains(".")
        ) {
            qualifiedMethodName
        } else {
            val qualifiedClassName = qualifiedMethodName.substring(
                0, qualifiedMethodName.substring(
                    0, qualifiedMethodName.indexOf("(")
                ).lastIndexOf(".")
            )
            qualifiedClassName.substringAfterLast(".")
        }
    }

    fun getShortFunctionSignature(qualifiedName: String): String {
        return getFunctionSignature(qualifiedName).replace("\\B\\w+(\\.)".toRegex(), "$1")
    }

    fun getFunctionSignature(qualifiedName: String): String {
        val withoutClassName = qualifiedName.replace(getQualifiedClassName(qualifiedName)!!, "")
        return withoutClassName.substring(
            withoutClassName.substring(0, withoutClassName.indexOf("(")).lastIndexOf("?") + 2
        )
            .replace("\\(java.lang.".toRegex(), "\\(").replace("<java.lang.".toRegex(), "<")
            .replace(",java.lang.".toRegex(), ",").replace("~".toRegex(), "")
    }

    fun removePackageNames(qualifiedMethodName: String?): String? {
        if (qualifiedMethodName == null || qualifiedMethodName.isEmpty() || !qualifiedMethodName.contains(".")) {
            return qualifiedMethodName
        }
        var className = qualifiedMethodName.substring(
            0, qualifiedMethodName.substring(
                0, qualifiedMethodName.indexOf("(")
            ).lastIndexOf(".")
        )
        if (className.contains("$")) {
            className = className.substring(0, className.indexOf("$"))
        }
        val arguments = qualifiedMethodName.substring(qualifiedMethodName.indexOf("("))
        val argArray = arguments.substring(1, arguments.length - 1).split(",").toTypedArray()
        val argText = StringBuilder("(")
        for (i in argArray.indices) {
            val qualifiedArgument = argArray[i]
            var newArgText = qualifiedArgument.substring(qualifiedArgument.lastIndexOf(".") + 1)
            if (qualifiedArgument.startsWith("$className$")) {
                newArgText = qualifiedArgument.substring(qualifiedArgument.lastIndexOf("$") + 1)
            }
            argText.append(newArgText)
            if (i + 1 < argArray.size) {
                argText.append(",")
            }
        }
        argText.append(")")
        val methodNameArr =
            qualifiedMethodName.substring(0, qualifiedMethodName.indexOf("(")).split("\\.").toTypedArray()
        return if (methodNameArr.size == 1) {
            methodNameArr[0] + argText.toString()
        } else {
            methodNameArr[methodNameArr.size - 2] + "." + methodNameArr[methodNameArr.size - 1] + argText.toString()
        }
    }

    fun removePackageAndClassName(qualifiedMethodName: String?): String? {
        return if (qualifiedMethodName == null || qualifiedMethodName.isEmpty()
            || !qualifiedMethodName.contains(".") || !qualifiedMethodName.contains("(")
        ) {
            qualifiedMethodName
        } else qualifiedMethodName.substring(
            qualifiedMethodName.substring(
                0, qualifiedMethodName.indexOf("(")
            ).lastIndexOf(".") + 1
        )
    }
}
