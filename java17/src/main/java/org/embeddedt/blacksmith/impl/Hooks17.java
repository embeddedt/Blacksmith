package org.embeddedt.blacksmith.impl;

import org.embeddedt.blacksmith.impl.modules.ConfigurationUtil;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class Hooks17 {
    private static final boolean DEBUG = false;
    public static Configuration resolveAndBind(ModuleFinder before, List<Configuration> parents, ModuleFinder after, Collection<String> roots) {
        long time = System.nanoTime();
        Configuration c1 = ConfigurationUtil.resolveAndBind(before, parents, roots);
        long totalTime = System.nanoTime() - time;
        System.out.println("[Blacksmith] Module resolution took " + TimeUnit.NANOSECONDS.toMillis(totalTime) + " ms");
        if(DEBUG) {
            System.out.println("Validating resolution...");
            Configuration c2 = Configuration.resolveAndBind(before, parents, after, roots);
            String s1 = ConfigurationUtil.dumpConfiguration(c1);
            String s2 = ConfigurationUtil.dumpConfiguration(c2);
            if(!s1.equals(s2)) {
                System.out.println("NOT MATCHED!");
                System.out.println("Fast:");
                System.out.println(s1);
                System.out.println("Slow:");
                System.out.println(s2);
                throw new AssertionError();
            }
        }
        return c1;
    }
}
