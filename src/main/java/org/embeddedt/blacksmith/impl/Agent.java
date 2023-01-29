package org.embeddedt.blacksmith.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The main agent class. Handles scanning for mods that want an instrumentation instance.
 */
public class Agent {
    public static Instrumentation instrumentation = null;
    public static boolean fallbackNeeded = false;
    public static void premain(String args, Instrumentation instrumentation) {
        Agent.instrumentation = instrumentation;
        System.out.println("[Blacksmith] Main agent loaded");
        ArrayList<AbstractMap.SimpleEntry<Path, String>> classesToLoad = new ArrayList<>();
        try {
            Path agentPath = new File(Agent.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).toPath();
            try(Stream<Path> stream = Files.walk(agentPath.getParent())) {
                List<Path> jars = stream.filter(path -> path.toString().endsWith(".jar")).collect(Collectors.toList());
                for(Path jarPath : jars) {
                    try {
                        try(ZipFile zFile = new ZipFile(jarPath.toFile())) {
                            ZipEntry entry = zFile.getEntry("META-INF/MANIFEST.MF");
                            if(entry != null) {
                                InputStream manifestStream = zFile.getInputStream(entry);
                                Manifest jarManifest = new Manifest(manifestStream);
                                manifestStream.close();
                                Attributes header = jarManifest.getMainAttributes();
                                String classToLoad = header.getValue("Blacksmith-Class");
                                if(classToLoad != null) {
                                    classesToLoad.add(new AbstractMap.SimpleEntry<>(jarPath, classToLoad));
                                }
                            }
                        }
                    } catch(IOException e) {
                        System.err.println("[Blacksmith] Encountered IOException while scanning file " + jarPath);
                        e.printStackTrace();
                    }
                }
            }
        } catch(URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
        String devClass = System.getenv("BLACKSMITH_DEV_CLASS");
        if(devClass != null) {
            classesToLoad.add(new AbstractMap.SimpleEntry<>(null, devClass));
        }
        classesToLoad.forEach(entry -> {
            System.out.println("[Blacksmith] Loading " + entry.getValue() + " from " + entry.getKey());
            ClassLoader modAgentLoader;
            if(entry.getKey() != null) {
                try {
                    modAgentLoader = new URLClassLoader(new URL[] { entry.getKey().toUri().toURL() });
                } catch(MalformedURLException e) {
                    e.printStackTrace();
                    return;
                }
            } else {
                modAgentLoader = ClassLoader.getSystemClassLoader();
            }

            try {
                JarFile modFile = new JarFile(new File(entry.getKey().toUri()));
                instrumentation.appendToSystemClassLoaderSearch(modFile);
            } catch(IOException e) {
                e.printStackTrace();
                return;
            }

            Class<?> clz;
            try {
                clz = modAgentLoader.loadClass(entry.getValue());
            } catch(ClassNotFoundException e) {
                e.printStackTrace();
                return;
            }
            try {
                Method initMethod = clz.getDeclaredMethod("agentmain", String.class, Instrumentation.class);
                initMethod.invoke(null, args, instrumentation);
            } catch(ReflectiveOperationException e) {
                e.printStackTrace();
                return;
            }
        });
    }
}
