package com.werner.analyze;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * 对继承自AppCompatActivity的Activity进行插桩
 */
public class TraceVisitor extends ClassVisitor {

    private String className;
    private String superName;

    public TraceVisitor(String className, ClassVisitor classVisitor) {
        super(Opcodes.ASM8, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        this.superName = superName;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
                                     String[] exceptions) {
        MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
        methodVisitor = new AdviceAdapter(Opcodes.ASM5, methodVisitor, access, name, desc) {

            @Override
            protected void onMethodEnter() {
                if (isInject()) {
                    System.out.println("visitMethod,name:"+name);
                    if ("onCreate".equals(name)) {
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "com/werner/aopanalyze/TraceUtil",
                                "onActivityCreate", "(Landroid/app/Activity;)V",
                                false);
                    } else if ("onDestroy".equals(name)) {
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitMethodInsn(INVOKESTATIC, "com/werner/aopanalyze/TraceUtil"
                                , "onActivityDestroy", "(Landroid/app/Activity;)V", false);
                    }
                }
            }
        };
        return methodVisitor;
    }


    private boolean isInject() {
        return superName.contains("AppCompatActivity");
    }


    @Override
    public void visitEnd() {
        if (isInject()){
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC,"onResume","()V",null,null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "onResume", "()V", false);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/werner/aopanalyze/TraceUtil"
                    , "onActivityResume", "(Landroid/app/Activity;)V", false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(1,1);
            mv.visitEnd();
        }
        super.visitEnd();
    }
}
