package com.sourceplusplus.agent.inject;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.pmw.tinylog.Logger;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.2
 * @since 0.1.0
 */
public class GenericClassAdapter extends ClassVisitor {

    private String className;

    public GenericClassAdapter(int asm5, ClassWriter cw) {
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

        Logger.debug("Injecting method: " + name);
        return new SourceApplicationInjector(Opcodes.ASM5, mv, access, name, desc, className);
    }
}
