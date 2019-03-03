package com.sourceplusplus.agent.inject;

import com.sourceplusplus.api.model.config.SourceAgentConfig;
import org.mutabilitydetector.asm.NonClassloadingClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.pmw.tinylog.Logger;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.1
 * @since 0.1.0
 */
public class ClassFileTransformerImpl implements ClassFileTransformer {

    private final List<Pattern> patternsForASMClasses = new ArrayList<>();
    private static final Set<String> excludedPackages = new HashSet<>();

    static {
        excludedPackages.add("com/sun/");
        excludedPackages.add("sun/");
        excludedPackages.add("org/apache/skywalking/apm/");
        excludedPackages.add("com/sourceplusplus/agent/sync/");
    }

    public ClassFileTransformerImpl(List<String> transformClassRegexPatterns) {
        for (String entry : transformClassRegexPatterns) {
            patternsForASMClasses.add(Pattern.compile(entry));
        }
    }

    public byte[] transform(ClassLoader loader, String className,
                            Class classBeingRedefined, ProtectionDomain protectionDomain,
                            byte[] classBytes) {
        if (shouldTransformUsingASM(className)) {
            return compileOneClass(className, classBytes);
        } else {
            return classBytes;
        }
    }

    public byte[] compileOneClass(String className, byte[] classBytes) {
        try {
            Logger.debug(String.format("Transforming class: %s", className));
            ClassReader cr = new ClassReader(classBytes);
            ClassWriter cw = new NonClassloadingClassWriter(
                    ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            GenericClassAdapter cv = new GenericClassAdapter(Opcodes.ASM5, cw);
            cr.accept(cv, ClassReader.SKIP_FRAMES);
            return cw.toByteArray();
        } catch (Throwable t) {
            t.printStackTrace();
            Logger.error("Error while transforming class: " + className, t);
            return classBytes;
        }
    }

    private boolean shouldTransformUsingASM(String className) {
        for (String excludedPackage : excludedPackages) {
            if (className.startsWith(excludedPackage)) {
                return false;
            }
        }
        boolean matchFound = false;
        if (!patternsForASMClasses.isEmpty()) {
            for (Object patternsForASMClass : patternsForASMClasses) {
                Pattern p = (Pattern) patternsForASMClass;
                Matcher m = p.matcher(className);
                if (m.matches()) {
                    matchFound = SourceAgentConfig.current.testMode || !className.startsWith("com/sourceplusplus/");
                }
            }
        }
        Logger.debug(String.format("Found traceable class: %s - Is match: %s", className, matchFound));
        return matchFound;
    }
}
