package org.embeddedt.blacksmith.impl;

import org.embeddedt.blacksmith.impl.modules.ConfigurationUtil;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class Hooks17 {
    public static Configuration resolveAndBind(ModuleFinder before, List<Configuration> parents, ModuleFinder after, Collection<String> roots) {
        System.out.println("FAST RESOLVE");
        return ConfigurationUtil.resolveAndBind(before, parents, after, roots);
    }
}
