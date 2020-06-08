package com.sourceplusplus.api.util;

/**
 * Useful methods for formatting artifact names.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.3.0
 * @since 0.3.0
 */
public class ArtifactNameUtils {

    public static String getShortQualifiedClassName(String qualifiedName) {
        return getQualifiedClassName(qualifiedName).replaceAll("\\B\\w+(\\.)", "$1");
    }

    public static String getShortQualifiedFunctionName(String qualifiedName) {
        return getShortQualifiedClassName(qualifiedName) + "." + getShortFunctionSignature(qualifiedName);
    }

    public static String getQualifiedClassName(String qualifiedName) {
        String withoutArgs = qualifiedName.substring(0, qualifiedName.indexOf("("));
        if (withoutArgs.contains("<")) {
            withoutArgs = withoutArgs.substring(0, withoutArgs.indexOf("<"));
        }
        return withoutArgs.substring(withoutArgs.lastIndexOf("?") + 1, withoutArgs.lastIndexOf("."));
    }

    public static String getShortFunctionSignature(String qualifiedName) {
        return getFunctionSignature(qualifiedName).replaceAll("\\B\\w+(\\.)", "$1");
    }

    public static String getFunctionSignature(String qualifiedName) {
        String withoutClassName = qualifiedName.replace(getQualifiedClassName(qualifiedName), "");
        return withoutClassName.substring(withoutClassName.substring(0, withoutClassName.indexOf("(")).lastIndexOf("?") + 2)
                .replaceAll("\\(java.lang.", "\\(").replaceAll("<java.lang.", "<")
                .replaceAll(",java.lang.", ",").replaceAll("~", "");
    }
}
