package org.embeddedt.blacksmith.impl.transformers;

import org.embeddedt.blacksmith.impl.TransformerCore;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.instrument.IllegalClassFormatException;
import java.util.Collections;
import java.util.List;

public class BackgroundScanHandlerTransformer implements RuntimeTransformer {
    @Override
    public List<String> getTransformedClasses() {
        return Collections.singletonList("net/minecraftforge/fml/loading/moddiscovery/BackgroundScanHandler");
    }

    @Override
    public void transformClass(ClassNode data) throws IllegalClassFormatException {
        for(MethodNode m : data.methods) {
            if(m.name.equals("<init>")) {
                for(AbstractInsnNode n : m.instructions) {
                    if(n.getOpcode() == Opcodes.INVOKESTATIC) {
                        MethodInsnNode mNode = (MethodInsnNode)n;
                        if(mNode.name.equals("newSingleThreadExecutor")) {
                            mNode.name = "makeScanningExecutor";
                            // TODO: decide this more intelligently
                            mNode.owner = TransformerCore.MODERN_FML ? RuntimeTransformer.HOOK17_CLASS : RuntimeTransformer.HOOK_CLASS;
                            System.out.println("Parallelized scanner");
                            break;
                        }
                    }
                }
            } else if(m.name.equals("addCompletedFile")) {
                /* synchronize */
                m.access |= Opcodes.ACC_SYNCHRONIZED;
            }
        }
    }
}
