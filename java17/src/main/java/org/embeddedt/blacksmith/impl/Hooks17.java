package org.embeddedt.blacksmith.impl;

import org.embeddedt.blacksmith.impl.modules.ConfigurationUtil;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class Hooks17 {
    private static final boolean DEBUG = true;
    public static Configuration resolveAndBind(ModuleFinder before, List<Configuration> parents, ModuleFinder after, Collection<String> roots) {
        System.out.println("FAST RESOLVE");
        Configuration c1 = ConfigurationUtil.resolveAndBind(before, parents, after, roots);
        if(DEBUG) {
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
