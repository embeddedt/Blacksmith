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

    private static final boolean USE_SCAN_CACHING = false;

    @Override
    public void transformClass(ClassNode data) throws IllegalClassFormatException {
        for(MethodNode method : data.methods) {
            if(method.name.equals("<clinit>")) {
                for (int i = 0; i < method.instructions.size(); i++) {
                    AbstractInsnNode ainsn = method.instructions.get(i);
                    if (ainsn.getOpcode() == Opcodes.INVOKESTATIC) {
                        MethodInsnNode minsn = (MethodInsnNode)ainsn;
                        if(minsn.name.equals("getLogger") && minsn.owner.contains("LogUtils")) {
                            System.out.println("Disabling scanner logging");
                            method.instructions.set(minsn, new FieldInsnNode(Opcodes.GETSTATIC, "org/slf4j/helpers/NOPLogger", "NOP_LOGGER", "Lorg/slf4j/helpers/NOPLogger;"));
                            break;
                        }
                    }
                }
            } else if(method.name.equals("fileVisitor")) {
                for(int i = 0; i < method.instructions.size(); i++) {
                    AbstractInsnNode ainsn = method.instructions.get(i);
                    if(ainsn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        MethodInsnNode minsn = (MethodInsnNode)ainsn;
                        if(minsn.name.equals("accept")) {
                            System.out.println("Found accept()");
                            //Agent.LOGGER.info(SELF, "Successfully patched Scanner to skip reading unnecessary data");
                            method.instructions.set(minsn.getPrevious(), new LdcInsnNode(ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES));
                            break;
                        }
                    }
                }
            } else if(USE_SCAN_CACHING && method.name.equals("scan")) {
                for(int i = 0; i < method.instructions.size(); i++) {
                    AbstractInsnNode ainsn = method.instructions.get(i);
                    if(ainsn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        MethodInsnNode minsn = (MethodInsnNode)ainsn;
                        if(minsn.name.equals("scanFile")) {
                            System.out.println("Wrapped scanner");
                            minsn.setOpcode(Opcodes.INVOKESTATIC);
                            minsn.owner = "org/embeddedt/blacksmith/impl/hooks/ForgeHooks";
                            minsn.name = "doScanning";
                            minsn.desc = "(Ljava/lang/Object;Ljava/util/function/Consumer;)V";
                            InsnList after = new InsnList();
                            after.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            after.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraftforge/fml/loading/moddiscovery/Scanner", "fileToScan", "Lnet/minecraftforge/fml/loading/moddiscovery/ModFile;"));
                            after.add(new VarInsnNode(Opcodes.ALOAD, 1));
                            after.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/embeddedt/blacksmith/impl/hooks/ForgeHooks", "postScanning", "(Ljava/lang/Object;Lnet/minecraftforge/forgespi/language/ModFileScanData;)V"));
                            method.instructions.insert(minsn, after);
                            break;
                        }
                    }
                }
            }
        }
    }
}
