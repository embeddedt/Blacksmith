package org.embeddedt.blacksmith.impl.transformers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.instrument.IllegalClassFormatException;
import java.util.Arrays;
import java.util.List;

public class AbstractModProviderTransformer implements RuntimeTransformer {
    @Override
    public List<String> getTransformedClasses() {
        return Arrays.asList("net/minecraftforge/fml/loading/moddiscovery/AbstractModProvider", "net/minecraftforge/fml/loading/moddiscovery/AbstractModLocator");
    }

    private static final String PREDICATE_METHOD = "hookPathPredicateCreation";
    private static final String PREDICATE_DESC = "()Ljava/util/function/BiPredicate;";

    @Override
    public void transformClass(ClassNode data) throws IllegalClassFormatException {
        for(MethodNode method : data.methods) {
            if(method.name.equals("createMod")) {
                for(AbstractInsnNode insn : method.instructions) {
                    if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                        MethodInsnNode invokeNode = (MethodInsnNode) insn;
                        if (invokeNode.owner.equals("cpw/mods/jarhandling/SecureJar") && invokeNode.name.equals("from")) {
                            AbstractInsnNode indyNode = invokeNode.getPrevious();
                            while(indyNode.getOpcode() != Opcodes.INVOKEDYNAMIC) {
                                indyNode = indyNode.getPrevious();
                            }
                            // can't use Hooks here
                            System.out.println("Remove pathFilter");
                            method.instructions.set(indyNode, new MethodInsnNode(Opcodes.INVOKESTATIC, data.name, PREDICATE_METHOD, PREDICATE_DESC, false));
                            break;
                        }
                    }
                }
            }
        }
        MethodNode hookMethod = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, PREDICATE_METHOD, PREDICATE_DESC, null, null);
        hookMethod.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        hookMethod.instructions.add(new InsnNode(Opcodes.ARETURN));
        hookMethod.maxStack = 1;
        data.methods.add(hookMethod);
    }
}
