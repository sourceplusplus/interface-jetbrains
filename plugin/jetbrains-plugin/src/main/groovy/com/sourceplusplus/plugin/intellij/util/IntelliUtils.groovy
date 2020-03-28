package com.sourceplusplus.plugin.intellij.util

import com.intellij.ide.util.JavaAnonymousClassesHelper
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.uast.UMethod

import java.awt.*

/**
 * todo: description
 *
 * @version 0.2.3
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

    @Nullable
    static PsiClass findClass(@NotNull PsiElement psiElement) {
        PsiClass containingClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class, false)
        while (containingClass instanceof PsiTypeParameter) {
            containingClass = PsiTreeUtil.getParentOfType(containingClass, PsiClass.class)
        }
        if (containingClass == null) {
            return null
        }
        return containingClass
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