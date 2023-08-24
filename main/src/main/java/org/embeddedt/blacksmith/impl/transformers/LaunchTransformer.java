package org.embeddedt.blacksmith.impl.transformers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.IllegalClassFormatException;
import java.util.Collections;
import java.util.List;

/**
 * Java 9+ support for LaunchWrapper
 */
public class LaunchTransformer implements RuntimeTransformer {

    @Override
    public List<String> getTransformedClasses() {
        return Collections.singletonList("net/minecraft/launchwrapper/Launch");
    }

    @Override
    public void transformClass(ClassNode data) throws IllegalClassFormatException {
        for(MethodNode m : data.methods) {
            if(m.name.equals("<init>")) {
                for(AbstractInsnNode i : m.instructions) {
                    if(i.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        MethodInsnNode invokeNode = (MethodInsnNode)i;
                        if(invokeNode.name.equals("getClassLoader")) {
                            System.out.println("Making LaunchWrapper work on Java 9+");
                            m.instructions.set(invokeNode, RuntimeTransformer.redirectToStaticHook("getClassLoaderJ9", "(Ljava/lang/Class;)Ljava/lang/ClassLoader;"));
                            break;
                        }
                    }
                }
                m.instructions.insertBefore(m.instructions.getLast().getPrevious(), new MethodInsnNode(Opcodes.INVOKESTATIC, "org/embeddedt/blacksmith/impl/hooks/WrapperBootstrap", "bootstrap", "()V"));
            }
        }
    }
}
