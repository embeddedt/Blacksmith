package org.embeddedt.blacksmith.impl.hooks;

import com.google.gson.Gson;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.embeddedt.blacksmith.impl.scanner.ModFileScanDataCacher;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ForgeHooks {
    private static Method pathGetter, scanFile;

    private static final Map<Object, ModFileScanDataCacher.Holder> cacheableModFiles = Collections.synchronizedMap(new IdentityHashMap<>());

    private static File getPathForCacheFile(Path path) {
        return new File("blacksmith" + File.separator + "scan_cache" + File.separator + path.getFileName());
    }

    static {
        new File("blacksmith" + File.separator + "scan_cache").mkdirs();
    }

    private static final boolean USE_SCAN_CACHING = false;

    public static void doScanning(Object modFile, Consumer<Path> pathConsumer) {
        if(pathGetter == null) {
            try {
                pathGetter = modFile.getClass().getDeclaredMethod("getFilePath");
                pathGetter.setAccessible(true);
                scanFile = modFile.getClass().getDeclaredMethod("scanFile", Consumer.class);
                scanFile.setAccessible(true);
            } catch(ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        // try to use cache
        try {
            Path filePath = (Path)pathGetter.invoke(modFile);
            ModFileScanDataCacher.Holder holder = null;
            if(USE_SCAN_CACHING && filePath.getFileSystem() == FileSystems.getDefault()) {
                try(Reader reader = new FileReader(getPathForCacheFile(filePath))) {
                    holder = ModFileScanDataCacher.GSON.fromJson(reader, ModFileScanDataCacher.Holder.class);
                } catch(FileNotFoundException ignored) {}
                catch(Exception e) {
                    e.printStackTrace();
                }
                cacheableModFiles.put(modFile, holder);
            }
            if(holder == null)
                scanFile.invoke(modFile, pathConsumer);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void postScanning(Object modFile, ModFileScanData data) {
        if(USE_SCAN_CACHING && cacheableModFiles.containsKey(modFile)) {
            ModFileScanDataCacher.Holder holder = cacheableModFiles.get(modFile);
            if(holder != null) {
                data.getClasses().addAll(holder.classes);
                data.getAnnotations().addAll(holder.annotations);
            } else {
                holder = new ModFileScanDataCacher.Holder();
                holder.classes.addAll(data.getClasses());
                holder.annotations.addAll(data.getAnnotations());
                try (Writer writer = new FileWriter(getPathForCacheFile((Path)pathGetter.invoke(modFile)))) {
                    ModFileScanDataCacher.GSON.toJson(holder, writer);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                System.out.println("cache miss");
            }
        }
    }
}
