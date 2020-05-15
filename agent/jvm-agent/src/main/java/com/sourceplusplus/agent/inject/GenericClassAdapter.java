package com.sourceplusplus.agent.inject;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.pmw.tinylog.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Used to trace custom functions via artifact subscriptions.
 * Todo: Can likely be replaced with apm-customize-enhance-plugin
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.5
 * @since 0.1.0
 */
public class GenericClassAdapter extends ClassVisitor {

    private static final Set<String> GROOVY_SKIP_METHODS = new HashSet<>();

    static {
        GROOVY_SKIP_METHODS.add("$getCallSiteArray");
        GROOVY_SKIP_METHODS.add("$createCallSiteArray");
        GROOVY_SKIP_METHODS.add("$getStaticMetaClass");
        GROOVY_SKIP_METHODS.add("getMetaClass");
        GROOVY_SKIP_METHODS.add("setMetaClass");
        GROOVY_SKIP_METHODS.add("invokeMethod");
        GROOVY_SKIP_METHODS.add("getProperty");
        GROOVY_SKIP_METHODS.add("setProperty");
        GROOVY_SKIP_METHODS.add("$createCallSiteArray_1");
        GROOVY_SKIP_METHODS.add("super$3$$getStaticMetaClass");
        GROOVY_SKIP_METHODS.add("$static_methodMissing");
        GROOVY_SKIP_METHODS.add("propertyMissing");
        GROOVY_SKIP_METHODS.add("methodMissing");
        GROOVY_SKIP_METHODS.add("$static_propertyMissing");
        GROOVY_SKIP_METHODS.add("doCall");
    }

    private String className;

    GenericClassAdapter(int asm5, ClassWriter cw) {
        super(asm5, cw);
    }

    @Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        className = name;
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        //skip main methods
        if ("main".equals(name) && "([Ljava/lang/String;)V".equals(desc)) {
            return mv;
        }
        //skip generated Apache SkyWalking methods
        if (name.equals("getSkyWalkingDynamicField")) {
            return mv;
        }
        //skip generated Groovy methods
        if (GROOVY_SKIP_METHODS.contains(name)) {
            return mv;
        }

        Logger.debug("Injecting method: " + name);
        return new SourceApplicationInjector(Opcodes.ASM5, mv, access, name, desc, className);
    }
}
