package com.sourceplusplus.agent.inject;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.0
 * @since 0.1.0
 */
public class SourceApplicationInjector extends AdviceAdapter {

    private final String className;
    private final String plainMethodName;
    private final int access;
    private String methodDesc;

    public SourceApplicationInjector(int asm5, MethodVisitor mv, int access,
                                     String name, String methodDesc, String className) {
        super(asm5, mv, access, name, methodDesc);

        this.className = className.replace("/", ".");
        this.access = access;
        this.methodDesc = methodDesc.substring(1, methodDesc.indexOf(")"));
        this.plainMethodName = name;

        int arrayDimensions = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.methodDesc.length(); i++) {
            char type = this.methodDesc.charAt(i);

            if (type == '[') {
                arrayDimensions++;
                continue;
            }

            switch (type) {
                case 'L':
                    int endIndex = this.methodDesc.substring(i + 1).indexOf(";") + 1 + i;
                    sb.append(this.methodDesc.substring(i + 1, endIndex).replace("/", "."));
                    i = endIndex;
                    break;
                case 'I':
                    sb.append("int");
                    break;
                case 'D':
                    sb.append("double");
                    break;
                case 'J':
                    sb.append("long");
                    break;
                case 'Z':
                    sb.append("boolean");
                    break;
                case 'B':
                    sb.append("byte");
                    break;
                case 'S':
                    sb.append("short");
                    break;
                case 'F':
                    sb.append("float");
                    break;
                case 'C':
                    sb.append("char");
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown: " + type);
            }

            int tmpSize = arrayDimensions;
            for (int a = 0; a < tmpSize; a++) {
                sb.append("[]");
                arrayDimensions--;
            }
            if (i + 1 < this.methodDesc.length()) {
                sb.append(",");
            }
        }
        this.methodDesc = "(" + sb.toString() + ")";
    }

    protected void onMethodEnter() {
        mv.visitLdcInsn(className + "." + plainMethodName + methodDesc);
        mv.visitMethodInsn(INVOKESTATIC, "com/sourceplusplus/agent/SourceAgent",
                "triggerStart", "(Ljava/lang/String;)V", false);
    }

    protected void onMethodExit(int opcode) {
        if (opcode != ATHROW) {
            mv.visitLdcInsn(className + "." + plainMethodName + methodDesc);
            mv.visitMethodInsn(INVOKESTATIC, "com/sourceplusplus/agent/SourceAgent",
                    "triggerEnd", "(Ljava/lang/String;)V", false);
        } else {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(className + "." + plainMethodName + methodDesc);
            mv.visitMethodInsn(INVOKESTATIC, "com/sourceplusplus/agent/SourceAgent",
                    "triggerEnd", "(Ljava/lang/Throwable;Ljava/lang/String;)V", false);
        }
    }
}
