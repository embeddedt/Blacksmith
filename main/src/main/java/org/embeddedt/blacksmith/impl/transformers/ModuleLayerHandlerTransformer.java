package org.embeddedt.blacksmith.impl.transformers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.IllegalClassFormatException;
import java.util.Collections;
import java.util.List;

public class ModuleLayerHandlerTransformer implements RuntimeTransformer {
    @Override
    public List<String> getTransformedClasses() {
        return Collections.singletonList("cpw/mods/modlauncher/ModuleLayerHandler");
    }

    @Override
    public void transformClass(ClassNode data) throws IllegalClassFormatException {
        for(MethodNode m : data.methods) {
            if(m.name.equals("buildLayer")) {
                for(AbstractInsnNode i : m.instructions) {
                    if(i.getOpcode() == Opcodes.INVOKESTATIC) {
                        MethodInsnNode invokeNode = (MethodInsnNode)i;
                        if(invokeNode.name.equals("resolveAndBind")) {
                            System.out.println("Using faster resolveAndBind impl");
                            invokeNode.owner = "org/embeddedt/blacksmith/impl/Hooks17";
                        }
                    }
                }
            }
        }
    }
}
