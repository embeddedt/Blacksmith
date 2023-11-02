package org.embeddedt.blacksmith.impl.transformers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.instrument.IllegalClassFormatException;
import java.util.List;

public interface RuntimeTransformer {
    String HOOK_CLASS = "org/embeddedt/blacksmith/impl/hooks/Hooks";
    String HOOK17_CLASS = "org/embeddedt/blacksmith/impl/Hooks17";
    List<String> getTransformedClasses();
    void transformClass(ClassNode data) throws IllegalClassFormatException;

    default int getWriteFlags() { return 0; }

    static MethodInsnNode redirectToStaticHook(String hookName, String hookDesc) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_CLASS, hookName, hookDesc, false);
    }

    static <T extends AbstractInsnNode> T swapInstruction(InsnList list, AbstractInsnNode oldInsn, T newInsn) {
        list.set(oldInsn, newInsn);
        return newInsn;
    }
}
