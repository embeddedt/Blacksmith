package org.embeddedt.blacksmith.impl;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;

/**
 * The main agent class. Handles setting up an isolated classloader to get the rest of the agent loaded.
 */
public class Agent {

    public static void premain(String args, Instrumentation instrumentation) {
        /* get a completely isolated classloader as soon as possible */
        CodeSource selfSource = Agent.class.getProtectionDomain().getCodeSource();
        URL[] targetURL = new URL[1];
        targetURL[0] = selfSource.getLocation();
        ClassLoader targetLoader = new URLClassLoader(targetURL, null);
        try {
            Class<?> transformerCore = targetLoader.loadClass("org.embeddedt.blacksmith.impl.TransformerCore");
            Method startMethod = transformerCore.getDeclaredMethod("start", Instrumentation.class);
            startMethod.invoke(null, instrumentation);
        } catch(ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}
