package org.embeddedt.blacksmith.impl.transformers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.IllegalClassFormatException;
import java.util.Arrays;
import java.util.List;

public class SJHJarTransformer implements RuntimeTransformer {
    @Override
    public List<String> getTransformedClasses() {
        return Arrays.asList("cpw/mods/cl/JarModuleFinder", "cpw/mods/jarhandling/impl/Jar");
    }

    @Override
    public void transformClass(ClassNode data) throws IllegalClassFormatException {
        if(data.name.equals("cpw/mods/cl/JarModuleFinder")) {
            for(MethodNode method : data.methods) {
                if(method.name.equals("<init>")) {
                    for(AbstractInsnNode insn : method.instructions) {
                        if(insn.getOpcode() == Opcodes.INVOKESTATIC) {
                            MethodInsnNode invokeNode = (MethodInsnNode)insn;
                            if(invokeNode.owner.equals("java/util/Arrays") && invokeNode.name.equals("stream")) {
                                // replace
                                System.out.println("Changed JarModuleReference construction to be parallel");
                                method.instructions.set(insn, RuntimeTransformer.redirectToStaticHook("arrayParallelStream", "([Ljava/lang/Object;)Ljava/util/stream/Stream;"));
                                break;
                            }
                        }
                    }
                }
            }
        } else if(data.name.equals("cpw/mods/jarhandling/impl/Jar")) {
            for(MethodNode method : data.methods) {
                if (method.name.equals("getPackages")) {
                    AbstractInsnNode insn = method.instructions.getFirst();
                    do {
                        if (insn.getOpcode() == Opcodes.INVOKESTATIC || insn.getOpcode() == Opcodes.INVOKEINTERFACE || insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                            MethodInsnNode invokeNode = (MethodInsnNode) insn;
                            if (invokeNode.owner.equals("java/nio/file/Files") && invokeNode.name.equals("walk")) {
                                // replace
                                System.out.println("Changed Jar#getPackages() to skip scanning assets & data");
                                insn = RuntimeTransformer.swapInstruction(method.instructions, insn, RuntimeTransformer.redirectToStaticHook("getPackagesSkippingAssets", "(Ljava/nio/file/Path;[Ljava/nio/file/FileVisitOption;)Ljava/util/stream/Stream;"));
                            } else if (invokeNode.owner.equals("java/util/stream/Stream") && invokeNode.name.equals("filter")) {
                                System.out.println("Removing filter() call");
                                insn = RuntimeTransformer.swapInstruction(method.instructions, insn, RuntimeTransformer.redirectToStaticHook("dummyFilter", "(Ljava/util/stream/Stream;Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"));
                            } else if (invokeNode.owner.equals("java/util/stream/Stream") && invokeNode.name.equals("map"))
                                break;
                        }
                        insn = insn.getNext();
                    } while(insn != null);
                }
            }
        }
    }
}
