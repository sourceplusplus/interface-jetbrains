package com.sourceplusplus.plugin.intellij.util

import com.intellij.ide.util.JavaAnonymousClassesHelper
import com.intellij.psi.*
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import org.jetbrains.annotations.Nullable
import org.jetbrains.uast.UMethod

/**
 * General IntelliJ utility functions.
 *
 * @version 0.2.6
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
final class IntelliUtils {

    private IntelliUtils() {
    }

    static String getArtifactQualifiedName(UMethod method) {
        def classQualifiedName = method.containingClass.qualifiedName
        def methodName = method.name
        def methodParams = ""
        method.parameterList.parameters.each {
            if (!methodParams.isEmpty()) {
                methodParams += ","
            }

            def qualifiedType = PsiUtil.resolveClassInType(it.type)
            int arrayDimensions = getArrayDimensions(it.type.toString())
            if (qualifiedType != null) {
                if (qualifiedType.containingClass != null) {
                    methodParams += qualifiedType.containingClass.qualifiedName + '$' + qualifiedType.name
                } else {
                    methodParams += qualifiedType.qualifiedName
                }

                for (int i = 0; i < arrayDimensions; i++) {
                    methodParams += "[]"
                }
            } else {
                methodParams += it.typeElement.text
            }
        }
        return "$classQualifiedName.$methodName($methodParams)"
    }

    @Nullable
    static String getClassVMName(PsiClass containingClass) {
        if (containingClass instanceof PsiAnonymousClass) {
            final PsiClass containingClassOfAnonymous = PsiTreeUtil.getParentOfType(containingClass, PsiClass.class)
            if (containingClassOfAnonymous == null) {
                return null
            }
            return getClassVMName(containingClassOfAnonymous) +
                    JavaAnonymousClassesHelper.getName((PsiAnonymousClass) containingClass)
        }
        return ClassUtil.getJVMClassName(containingClass)
    }

    private static int getArrayDimensions(final String s) {
        int arrayDimensions = 0
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '[' as char) {
                arrayDimensions++
            }
        }
        return arrayDimensions
    }
}