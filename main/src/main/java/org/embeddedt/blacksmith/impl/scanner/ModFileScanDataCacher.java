package org.embeddedt.blacksmith.impl.scanner;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraftforge.forgespi.language.ModFileScanData;

import java.io.IOException;
import java.lang.reflect.Type;

public class ModFileScanDataCacher {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ModFileScanData.class, new ScanDataHandler())
            .registerTypeAdapter(org.objectweb.asm.Type.class, new TypeTypeAdapter())
            .create();

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

    private static class ScanDataHandler implements JsonDeserializer<ModFileScanData>, JsonSerializer<ModFileScanData> {
        @Override
        public ModFileScanData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ModFileScanData data = new ModFileScanData();
            JsonObject object = json.getAsJsonObject();
            if(object.has("classes")) {
                JsonArray array = object.getAsJsonArray("classes");
                for(JsonElement clz : array) {
                    data.getClasses().add(context.deserialize(clz.getAsJsonObject(), ModFileScanData.ClassData.class));
                }
            }
            if(object.has("annotations")) {
                JsonArray array = object.getAsJsonArray("annotations");
                for(JsonElement clz : array) {
                    data.getClasses().add(context.deserialize(clz.getAsJsonObject(), ModFileScanData.AnnotationData.class));
                }
            }
            return data;
        }

        @Override
        public JsonElement serialize(ModFileScanData src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            JsonArray classes = new JsonArray(), annotations = new JsonArray();
            for(ModFileScanData.ClassData clz : src.getClasses()) {
                classes.add(context.serialize(clz));
            }
            for(ModFileScanData.AnnotationData annot : src.getAnnotations()) {
                annotations.add(context.serialize(annot));
            }
            obj.add("classes", classes);
            obj.add("annotations", annotations);
            return obj;
        }
    }
}
