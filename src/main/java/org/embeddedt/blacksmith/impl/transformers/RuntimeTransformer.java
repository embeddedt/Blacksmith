package org.embeddedt.blacksmith.impl.transformers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.instrument.IllegalClassFormatException;
import java.util.List;

public interface RuntimeTransformer {
    List<String> getTransformedClasses();
    void transformClass(ClassNode data) throws IllegalClassFormatException;

    default int getWriteFlags() { return 0; }

    static MethodInsnNode redirectToStaticHook(String hookName, String hookDesc) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, "org/embeddedt/blacksmith/impl/hooks/Hooks", hookName, hookDesc, false);
    }
}
