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

    public static String removePackageNames(String qualifiedMethodName) {
        if (qualifiedMethodName == null || qualifiedMethodName.isEmpty() || !qualifiedMethodName.contains(".")) {
            return qualifiedMethodName;
        }
        String className = qualifiedMethodName.substring(0, qualifiedMethodName.substring(
                0, qualifiedMethodName.indexOf("(")).lastIndexOf("."));
        if (className.contains("$")) {
            className = className.substring(0, className.indexOf("$"));
        }

        String arguments = qualifiedMethodName.substring(qualifiedMethodName.indexOf("("));
        String[] argArray = arguments.substring(1, arguments.length() - 1).split(",");
        StringBuilder argText = new StringBuilder("(");
        for (int i = 0; i < argArray.length; i++) {
            String qualifiedArgument = argArray[i];
            String newArgText = qualifiedArgument.substring(qualifiedArgument.lastIndexOf(".") + 1);
            if (qualifiedArgument.startsWith(className + "$")) {
                newArgText = qualifiedArgument.substring(qualifiedArgument.lastIndexOf("$") + 1);
            }
            argText.append(newArgText);

            if ((i + 1) < argArray.length) {
                argText.append(",");
            }
        }
        argText.append(")");

        String[] methodNameArr = qualifiedMethodName.substring(0, qualifiedMethodName.indexOf("(")).split("\\.");
        if (methodNameArr.length == 1) {
            return methodNameArr[0] + argText.toString();
        } else {
            return methodNameArr[methodNameArr.length - 2] + "." + methodNameArr[methodNameArr.length - 1] + argText.toString();
        }
    }

    public static String removePackageAndClassName(String qualifiedMethodName) {
        if (qualifiedMethodName == null || qualifiedMethodName.isEmpty()
                || !qualifiedMethodName.contains(".") || !qualifiedMethodName.contains("(")) {
            return qualifiedMethodName;
        }
        return qualifiedMethodName.substring(qualifiedMethodName.substring(
                0, qualifiedMethodName.indexOf("(")).lastIndexOf(".") + 1);
    }
}
