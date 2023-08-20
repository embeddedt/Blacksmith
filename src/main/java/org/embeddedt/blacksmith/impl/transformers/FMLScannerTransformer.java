package org.embeddedt.blacksmith.impl.transformers;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.instrument.IllegalClassFormatException;
import java.util.Collections;
import java.util.List;

public class FMLScannerTransformer implements RuntimeTransformer {
    @Override
    public List<String> getTransformedClasses() {
        return Collections.singletonList("net/minecraftforge/fml/loading/moddiscovery/Scanner");
    }

    @Override
    public void transformClass(ClassNode data) throws IllegalClassFormatException {
        for(MethodNode method : data.methods) {
            if(method.name.equals("fileVisitor")) {
                for(int i = 0; i < method.instructions.size(); i++) {
                    AbstractInsnNode ainsn = method.instructions.get(i);
                    if(ainsn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        MethodInsnNode minsn = (MethodInsnNode)ainsn;
                        if(minsn.name.equals("accept")) {
                            System.out.println("Found accept()");
                            //Agent.LOGGER.info(SELF, "Successfully patched Scanner to skip reading unnecessary data");
                            method.instructions.set(minsn.getPrevious(), new LdcInsnNode(ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES));
                            return;
                        }
                    }
                }
            }
        }
    }
}
