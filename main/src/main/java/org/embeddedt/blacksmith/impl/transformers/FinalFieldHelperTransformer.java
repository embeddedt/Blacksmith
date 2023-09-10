package org.embeddedt.blacksmith.impl.transformers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.instrument.IllegalClassFormatException;
import java.util.Collections;
import java.util.List;

public class FinalFieldHelperTransformer implements RuntimeTransformer {
    @Override
    public List<String> getTransformedClasses() {
        if(System.getProperty("java.version", "").startsWith("1."))
            return Collections.emptyList(); // Don't apply on Java 8 and lower
        return Collections.singletonList("net/minecraftforge/registries/ObjectHolderRef$FinalFieldHelper");
    }

    @Override
    public void transformClass(ClassNode data) throws IllegalClassFormatException {
        for(MethodNode m : data.methods) {
            if(m.name.equals("makeWritable")) {
                m.instructions.clear();
                m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                m.instructions.add(new InsnNode(Opcodes.ICONST_0));
                m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V"));
                m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                m.instructions.add(new InsnNode(Opcodes.ARETURN));
                m.maxStack = 2;
            } else if(m.name.equals("setField")) {
                m.instructions.clear();
                m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
                m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", "set", "(Ljava/lang/Object;Ljava/lang/Object;)V"));
                m.instructions.add(new InsnNode(Opcodes.RETURN));
                m.maxStack = 3;
            }
        }
    }
}
