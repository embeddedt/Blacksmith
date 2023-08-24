package org.embeddedt.blacksmith.impl.hooks;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public class LaunchWrapperTransformer implements IClassTransformer {
    private static final boolean SHOULD_RUN = true;
    public LaunchWrapperTransformer() {
        System.out.println("Initialized Blacksmith LaunchWrapper transformer");
        try {
            Class<?> clz = Class.forName("org.objectweb.asm.Item");
            System.out.println(clz.getClassLoader());
        } catch(ReflectiveOperationException e) {

        }
    }

    private static final String OBJECT_HOLDER = "Lnet/minecraftforge/fml/common/registry/GameRegistry$ObjectHolder;"; //Don't directly reference this to prevent class loading.

    private static final Set<String> VANILLA_HOLDERS = new HashSet<>();

    static {
        VANILLA_HOLDERS.add("net.minecraft.init.Blocks");
        VANILLA_HOLDERS.add("net.minecraft.init.Items");
        VANILLA_HOLDERS.add("net.minecraft.init.MobEffects");
        VANILLA_HOLDERS.add("net.minecraft.init.Biomes");
        VANILLA_HOLDERS.add("net.minecraft.init.Enchantments");
        VANILLA_HOLDERS.add("net.minecraft.init.SoundEvents");
        VANILLA_HOLDERS.add("net.minecraft.init.PotionTypes");
    }


    private byte[] handleTransformation(String name, byte[] classBytes) {
        ClassReader reader;
        try {
            reader = new ClassReader(classBytes);
        } catch(IllegalArgumentException e) {
            return classBytes;
        }
        ClassNode node = new ClassNode(Opcodes.ASM5);
        AtomicBoolean madeChange = new AtomicBoolean(false);
        reader.accept(node, 0);

        for(FieldNode f : node.fields) {
            if(f.visibleAnnotations != null && f.desc.startsWith("L") && f.visibleAnnotations.stream().anyMatch(ann -> ann.desc.equals(OBJECT_HOLDER))) {
                int oldAcc = f.access;
                f.access = (f.access & ~Opcodes.ACC_FINAL) | Opcodes.ACC_SYNTHETIC;
                if(oldAcc != f.access) {
                    f.access = oldAcc;
                    madeChange.set(true);
                }
            }
        }

        if(VANILLA_HOLDERS.contains(name)) {
            int targetFlags = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;
            for(FieldNode f : node.fields) {
                if(f.desc.startsWith("L") && (f.access & targetFlags) == targetFlags) {
                    f.access = (f.access & ~Opcodes.ACC_FINAL) | Opcodes.ACC_SYNTHETIC;
                    madeChange.set(true);
                }
            }
        }

        if(!madeChange.get())
            return classBytes;

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if(SHOULD_RUN && basicClass != null && basicClass.length > 0 && transformedName != null) {
            return handleTransformation(transformedName, basicClass);
        }
        return basicClass;
    }
}
