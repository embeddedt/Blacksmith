package org.embeddedt.blacksmith.impl.transformers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.IllegalClassFormatException;
import java.util.Collections;
import java.util.List;

public class LaunchClassLoaderTransformer implements RuntimeTransformer {
    @Override
    public List<String> getTransformedClasses() {
        return Collections.singletonList("net/minecraft/launchwrapper/LaunchClassLoader");
    }

    @Override
    public void transformClass(ClassNode data) throws IllegalClassFormatException {
        for(MethodNode method : data.methods) {
            for(int i = 0; i < method.instructions.size(); i++) {
                AbstractInsnNode insn = method.instructions.get(i);
                if(insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                    MethodInsnNode invoke = (MethodInsnNode)insn;
                    if(invoke.name.equals("getManifest") && invoke.owner.equals("java/util/jar/JarFile")) {
                        System.out.println("Replacing getManifest call in " + method.name);
                        method.instructions.set(invoke, RuntimeTransformer.redirectToStaticHook( "jarFileGetManifest", "(Ljava/util/jar/JarFile;)Ljava/util/jar/Manifest;"));
                    }
                }
            }
        }
    }
}
