package org.embeddedt.blacksmith.impl.scanner;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraftforge.forgespi.language.ModFileScanData;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class ModFileScanDataCacher {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ModFileScanDataCacher.Holder.class, new ScanDataHandler())
            .registerTypeAdapter(ModFileScanData.ClassData.class, new ClassDataHandler())
            .registerTypeAdapter(ModFileScanData.AnnotationData.class, new AnnotationDataHandler())
            .registerTypeAdapter(org.objectweb.asm.Type.class, new TypeTypeAdapter())
            .create();

    public static class Holder {
        public List<ModFileScanData.ClassData> classes = new ArrayList<>();
        public List<ModFileScanData.AnnotationData> annotations = new ArrayList<>();
    }

    private static class TypeTypeAdapter extends TypeAdapter<org.objectweb.asm.Type> {
        @Override
        public void write(JsonWriter out, org.objectweb.asm.Type value) throws IOException {
            if(value == null) {
                out.nullValue();
                return;
            }
            out.value(value.getDescriptor());
        }

        @Override
        public org.objectweb.asm.Type read(JsonReader reader) throws IOException {
            if(reader.peek() == JsonToken.NULL)
                return null;
            String descriptor = reader.nextString();
            return org.objectweb.asm.Type.getType(descriptor);
        }
    }

    private static class AnnotationDataHandler implements JsonDeserializer<ModFileScanData.AnnotationData>, JsonSerializer<ModFileScanData.AnnotationData> {
        private static final ThreadLocal<JsonDeserializationContext> deserContext = ThreadLocal.withInitial(() -> null);
        private static final ThreadLocal<JsonSerializationContext> serContext = ThreadLocal.withInitial(() -> null);

        static Object deserializeAnnotationValue(JsonElement element) {
            if(element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if(primitive.isNumber())
                    return primitive.getAsNumber();
                else if(primitive.isString()){
                    return primitive.getAsString();
                } else if(primitive.isBoolean())
                    return primitive.getAsBoolean();
            } else if(element.isJsonArray()) {
                return new ArrayList<>(element.getAsJsonArray().asList().stream().map(AnnotationDataHandler::deserializeAnnotationValue).collect(Collectors.toList()));
            } else if(element.isJsonObject()) {
                return deserContext.get().deserialize(element, org.objectweb.asm.Type.class);
            }
            throw new IllegalArgumentException(element.toString());
        }

        static JsonElement serializeAnnotationValue(Object element) {
            if(element instanceof Boolean) {
                return new JsonPrimitive((Boolean)element);
            } else if(element instanceof Number) {
                return new JsonPrimitive((Number)element);
            } else if(element instanceof String) {
                return new JsonPrimitive((String)element);
            } else if(element.getClass().isArray()) {
                int len = Array.getLength(element);
                JsonArray array = new JsonArray();
                for(int i = 0; i < len; i++) {
                    array.add(serializeAnnotationValue(Array.get(element, i)));
                }
                return array;
            } else {
                return serContext.get().serialize(element);
            }
        }

        @Override
        public ModFileScanData.AnnotationData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            Map<String, Object> annotationData = new HashMap<>();
            JsonObject annDataObj = object.getAsJsonObject("annotationData");
            if(annDataObj != null) {
                deserContext.set(context);
                try {
                    for(Map.Entry<String, JsonElement> e : annDataObj.entrySet()) {
                        annotationData.put(e.getKey(), deserializeAnnotationValue(e.getValue()));
                    }
                } finally {
                    deserContext.set(null);
                }
            }
            return new ModFileScanData.AnnotationData(
                    context.deserialize(object.get("annotationType"), org.objectweb.asm.Type.class),
                    context.deserialize(object.get("targetType"), org.objectweb.asm.Type.class),
                    context.deserialize(object.get("clazz"), org.objectweb.asm.Type.class),
                    object.getAsJsonPrimitive("memberName").getAsString(),
                    annotationData
            );
        }

        private static final Field annotationType, targetType, clazz, memberName, annotationData;
        static {
            try {
                clazz = ModFileScanData.AnnotationData.class.getDeclaredField("clazz");
                clazz.setAccessible(true);
                annotationType = ModFileScanData.AnnotationData.class.getDeclaredField("annotationType");
                annotationType.setAccessible(true);
                targetType = ModFileScanData.AnnotationData.class.getDeclaredField("targetType");
                targetType.setAccessible(true);
                memberName = ModFileScanData.AnnotationData.class.getDeclaredField("memberName");
                memberName.setAccessible(true);
                annotationData = ModFileScanData.AnnotationData.class.getDeclaredField("annotationData");
                annotationData.setAccessible(true);
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public JsonElement serialize(ModFileScanData.AnnotationData src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            try {
                obj.add("clazz", context.serialize(clazz.get(src)));
                obj.add("annotationType", context.serialize(annotationType.get(src)));
                obj.add("targetType", context.serialize(targetType.get(src)));
                obj.add("memberName", context.serialize(memberName.get(src)));
                Map<String, Object> map = (Map<String, Object>)annotationData.get(src);
                if(map.size() > 0) {
                    JsonObject annDataObj = new JsonObject();
                    for(Map.Entry<String, Object> entry : map.entrySet()) {
                        annDataObj.add(entry.getKey(), serializeAnnotationValue(entry.getValue()));
                    }
                    obj.add("annotationData", annDataObj);
                }
            } catch(ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            return obj;
        }
    }

    private static class ClassDataHandler implements JsonDeserializer<ModFileScanData.ClassData>, JsonSerializer<ModFileScanData.ClassData> {
        @Override
        public ModFileScanData.ClassData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            JsonArray array = object.getAsJsonArray("interfaces");
            Set<org.objectweb.asm.Type> interfaces = new HashSet<>();
            if(array != null) {
                for(JsonElement element : array.asList()) {
                    interfaces.add(context.deserialize(element, org.objectweb.asm.Type.class));
                }
            }
            return new ModFileScanData.ClassData(
                    context.deserialize(object.get("clazz"), org.objectweb.asm.Type.class),
                    context.deserialize(object.get("parent"), org.objectweb.asm.Type.class),
                    interfaces
            );
        }

        private static final Field clazz, parent, interfaces;
        static {
            try {
                clazz = ModFileScanData.ClassData.class.getDeclaredField("clazz");
                clazz.setAccessible(true);
                parent = ModFileScanData.ClassData.class.getDeclaredField("parent");
                parent.setAccessible(true);
                interfaces = ModFileScanData.ClassData.class.getDeclaredField("interfaces");
                interfaces.setAccessible(true);
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public JsonElement serialize(ModFileScanData.ClassData src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            try {
                obj.add("clazz", context.serialize(clazz.get(src)));
                obj.add("parent", context.serialize(parent.get(src)));
                JsonArray array = new JsonArray();
                Set<org.objectweb.asm.Type> ifaces = (Set<org.objectweb.asm.Type>)interfaces.get(src);
                if(ifaces.size() > 0) {
                    for(org.objectweb.asm.Type type : ifaces) {
                        array.add(context.serialize(type));
                    }
                    obj.add("interfaces", array);
                }
            } catch(ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            return obj;
        }
    }

    private static class ScanDataHandler implements JsonDeserializer<Holder>, JsonSerializer<Holder> {
        @Override
        public Holder deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Holder data = new Holder();
            JsonObject object = json.getAsJsonObject();
            if(object.has("classes")) {
                JsonArray array = object.getAsJsonArray("classes");
                for(JsonElement clz : array) {
                    data.classes.add(context.deserialize(clz.getAsJsonObject(), ModFileScanData.ClassData.class));
                }
            }
            if(object.has("annotations")) {
                JsonArray array = object.getAsJsonArray("annotations");
                for(JsonElement clz : array) {
                    data.annotations.add(context.deserialize(clz.getAsJsonObject(), ModFileScanData.AnnotationData.class));
                }
            }
            return data;
        }

        @Override
        public JsonElement serialize(Holder src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            JsonArray classes = new JsonArray(), annotations = new JsonArray();
            for(ModFileScanData.ClassData clz : src.classes) {
                classes.add(context.serialize(clz));
            }
            for(ModFileScanData.AnnotationData annot : src.annotations) {
                annotations.add(context.serialize(annot));
            }
            obj.add("classes", classes);
            obj.add("annotations", annotations);
            return obj;
        }
    }
}
