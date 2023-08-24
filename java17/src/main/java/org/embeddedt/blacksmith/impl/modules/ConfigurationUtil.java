package org.embeddedt.blacksmith.impl.modules;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.module.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Includes a faster implementation of resolveAndBind that does not suffer from bad time complexity with many
 * automatic modules. Written by Technici4n, permission granted to include in Blacksmith under LGPL3.
 *
 * Currently needs --add-opens java.base/java.lang.invoke=ALL-UNNAMED.
 */
public class ConfigurationUtil {
    private static final MethodHandle MODULE_DESCRIPTOR__AUTOMATIC__SETTER;
    private static final MethodHandle MODULE_DESCRIPTOR__PROVIDES__SETTER;
    private static final MethodHandle CONFIGURATION__GRAPH__GETTER;

    static {
        try {
            var hackfield = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            hackfield.setAccessible(true);
            MethodHandles.Lookup hack = (MethodHandles.Lookup) hackfield.get(null);

            MODULE_DESCRIPTOR__AUTOMATIC__SETTER = hack.findSetter(ModuleDescriptor.class, "automatic", boolean.class);
            MODULE_DESCRIPTOR__PROVIDES__SETTER = hack.findSetter(ModuleDescriptor.class, "provides", Set.class);
            CONFIGURATION__GRAPH__GETTER = hack.findGetter(Configuration.class, "graph", Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to collect method handles", e);
        }
    }

    public static Configuration resolveAndBind(ModuleFinder before, List<Configuration> parents, ModuleFinder after, Collection<String> roots) {
        Set<ModuleDescriptor> automaticModules = collectAutomaticModules(before, after);

        // Temporarily disable automatic flag for faster resolution
        setAutomatic(automaticModules, false);
        // We need to clear the provides flag to avoid export checks for provided packages (which might fail due to service-providing modules being unreadable at resolution time).
        var savedProvides = clearProvides(automaticModules);

        var conf = Configuration.resolveAndBind(before, parents, after, roots);

        // Restore
        setAutomatic(automaticModules, true);
        restoreProvides(savedProvides);

        // Readability graph
        Map<ResolvedModule, Set<ResolvedModule>> g1 = getReadabilityGraph(conf);

        // Fix dependencies involving automatic modules
        var allModules = Stream.concat(Stream.of(conf), parents.stream())
                .flatMap(c -> c.modules().stream())
                .collect(Collectors.toSet());
        var allAutomaticModules = allModules.stream()
                .filter(m -> m.reference().descriptor().isAutomatic())
                .collect(Collectors.toSet());

        for (var module : conf.modules()) {
            if (module.reference().descriptor().isAutomatic()) {
                // Automatic modules can read all modules
                var moduleReads = g1.get(module);
                moduleReads.addAll(allModules);
            } else {
                // A single dependency on an automatic module in this layer makes this module read all automatic modules
                boolean dependsOnAutomatic = g1.getOrDefault(module, Set.of()).stream().anyMatch(m -> allAutomaticModules.contains(m) && m.configuration() == conf);
                if (dependsOnAutomatic) {
                    var moduleReads = g1.get(module);
                    moduleReads.addAll(allAutomaticModules);
                }
            }
        }

        return conf;
    }

    private static Set<ModuleDescriptor> collectAutomaticModules(ModuleFinder before, ModuleFinder after) {
        return Stream.concat(before.findAll().stream(), after.findAll().stream())
                .map(ModuleReference::descriptor)
                .filter(ModuleDescriptor::isAutomatic)
                .collect(Collectors.toSet());
    }

    private static void setAutomatic(Set<ModuleDescriptor> modules, boolean automatic) {
        modules.forEach(m -> {
            try {
                MODULE_DESCRIPTOR__AUTOMATIC__SETTER.invoke(m, automatic);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static Map<ModuleDescriptor, Set<ModuleDescriptor.Provides>> clearProvides(Set<ModuleDescriptor> modules) {
        Map<ModuleDescriptor, Set<ModuleDescriptor.Provides>> ret = new IdentityHashMap<>();
        modules.forEach(m -> {
            ret.put(m, m.provides());
            try {
                MODULE_DESCRIPTOR__PROVIDES__SETTER.invoke(m, Set.of());
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
        return ret;
    }

    private static void restoreProvides(Map<ModuleDescriptor, Set<ModuleDescriptor.Provides>> map) {
        map.forEach((m, provides) -> {
            try {
                MODULE_DESCRIPTOR__PROVIDES__SETTER.invoke(m, provides);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static Map<ResolvedModule, Set<ResolvedModule>> getReadabilityGraph(Configuration conf) {
        try {
            return (Map<ResolvedModule, Set<ResolvedModule>>) CONFIGURATION__GRAPH__GETTER.invoke(conf);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to get readability graph", e);
        }
    }

    public static String dumpConfiguration(Configuration conf) {
        StringBuilder sb = new StringBuilder("Dumping configuration\n");
        List<ResolvedModule> modList = new ArrayList<>(conf.modules());
        modList.sort(Comparator.comparing(ResolvedModule::name));
        modList.forEach(m -> {
            sb.append(m.name()).append(" with descriptor ").append(m.reference().descriptor()).append('\n');
            List<String> reads = m.reads().stream().map(ResolvedModule::name).collect(Collectors.toCollection(ArrayList::new));
            reads.sort(Comparator.naturalOrder());
            sb.append("  reads: ").append(String.join(",", reads)).append('\n');
        });
        return sb.toString();
    }
}
