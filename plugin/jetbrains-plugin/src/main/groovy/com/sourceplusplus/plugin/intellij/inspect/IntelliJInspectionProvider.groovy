package com.sourceplusplus.plugin.intellij.inspect

import com.intellij.codeInsight.daemon.GroupNames
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionToolProvider
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.plugin.PluginBootstrap
import com.sourceplusplus.plugin.SourcePluginDefines
import com.sourceplusplus.plugin.intellij.IntelliJStartupActivity
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.*

/**
 * InspectionToolProvider for the primary source mark inspection.
 *
 * @version 0.2.0
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntelliJInspectionProvider extends AbstractBaseJavaLocalInspectionTool implements InspectionToolProvider {

    private static final Logger log = LoggerFactory.getLogger(this.name)
    public static PsiFile lastFileOpened

    @NotNull
    @Override
    PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly,
                                   @NotNull LocalInspectionToolSession session) {
        if (!(holder.file instanceof PsiClassOwner)) {
            //not source code
            return super.buildVisitor(holder, isOnTheFly, session)
        }
        if (SourcePluginConfig.current.appUuid == null) {
            log.warn("No App UUID found. Ignoring file visit")
            return super.buildVisitor(holder, isOnTheFly, session)
        }

        def sourcePlugin = PluginBootstrap.sourcePlugin
        if (sourcePlugin != null && (lastFileOpened == null || lastFileOpened != holder.file)) {
            log.debug("Visited by file: {}", holder.file.virtualFile)
            lastFileOpened = holder.file

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                void run() {
                    IntelliJStartupActivity.coordinateSourceFileOpened(sourcePlugin, (PsiClassOwner) holder.file)
                }
            })
        }
        return super.buildVisitor(holder, isOnTheFly, session)
    }

    @NotNull
    @Override
    String getDisplayName() {
        return SourcePluginDefines.DISPLAY_NAME
    }

    @NotNull
    @Override
    String getShortName() {
        return SourcePluginDefines.FULL_TEXT_NAME
    }

    @Nullable
    @Override
    String getStaticDescription() {
        return "$SourcePluginDefines.DISPLAY_NAME - Version: $SourcePluginDefines.VERSION"
    }

    @Override
    boolean isEnabledByDefault() {
        return true
    }

    @NotNull
    @Override
    String getGroupDisplayName() {
        return GroupNames.PERFORMANCE_GROUP_NAME
    }

    @NotNull
    @Override
    Class[] getInspectionClasses() {
        return [IntelliJInspectionProvider.class]
    }
}