package org.embeddedt.blacksmith.impl.transformers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.instrument.IllegalClassFormatException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Fix excessive CPU usage by the file watcher thread.
 */
public class FileWatcherTransformer implements RuntimeTransformer {
    @Override
    public List<String> getTransformedClasses() {
        return Collections.singletonList("com/electronwill/nightconfig/core/file/FileWatcher$WatcherThread");
    }

    @Override
    public void transformClass(ClassNode data) throws IllegalClassFormatException {
        for(MethodNode m : data.methods) {
            if(m.name.equals("run")) {
                for(AbstractInsnNode i : m.instructions) {
                    if(i.getOpcode() == Opcodes.INVOKESTATIC) {
                        MethodInsnNode invoke = (MethodInsnNode)i;
                        if(invoke.name.equals("parkNanos")) {
                            if(invoke.getPrevious() instanceof LdcInsnNode) {
                                LdcInsnNode constant = (LdcInsnNode)invoke.getPrevious();
                                System.out.println(constant.cst);
                                // use longs
                                if(Objects.equals(constant.cst, 1000L)) {
                                    System.out.println("Increased watcher park time");
                                    constant.cst = TimeUnit.MILLISECONDS.toNanos(1000);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
