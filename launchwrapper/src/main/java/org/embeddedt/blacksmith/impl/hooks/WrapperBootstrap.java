package org.embeddedt.blacksmith.impl.hooks;

import net.minecraft.launchwrapper.Launch;

public class WrapperBootstrap {
    public static void bootstrap() {
        System.out.println(WrapperBootstrap.class.getClassLoader());
        Launch.classLoader.addClassLoaderExclusion("org.objectweb.asm.");
        Launch.classLoader.registerTransformer("org.embeddedt.blacksmith.impl.hooks.LaunchWrapperTransformer");
    }
}
