package org.untitled.immolation.client.features;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.untitled.immolation.client.type.Identifications;
import org.untitled.immolation.client.type.WeightsWrapper;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ItemWeight {
    //Store all the Mythics + their respective weightings in ItemIds
    public static final Map<String, Identifications> MythicWeights = new HashMap<String, Identifications>();
    public static void loadWeights() {
        //Identifier id = Identifier.of("immolation", "immolation/weights.json");
        //I think i need to be killed after writing whatever  this is LOL
        File jsonFile = new File("D:\\IdeaProjects\\immolation\\src\\client\\resources\\immolation\\weights.json");
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8)) {

            JsonElement fileElement = JsonParser.parseReader(new JsonReader(reader));
            if (fileElement.isJsonObject()) {
                JsonObject jsonObject = fileElement.getAsJsonObject();

                JsonObject weights = jsonObject.get("weights").getAsJsonObject();
                System.out.println("size = " + weights.entrySet().size());
                for (Map.Entry<String, JsonElement> entry : weights.entrySet()) {
                    JsonObject itemObject = entry.getValue().getAsJsonObject();
                    System.out.println(entry.getKey() + ": " + entry.getValue() + "\n");
                    if (itemObject.has("Main")) {
                        Identifications stats = new Identifications();
                        HashMap<String, Identifications> itemToAdd = new HashMap<>();
                        JsonObject mainStats = itemObject.get("Main").getAsJsonObject();
                        for (Map.Entry<String, JsonElement> stat : mainStats.entrySet()) {
                            stats.put(stat.getKey(), stat.getValue().getAsFloat());

                            System.out.println(stat.getKey() + ": " + stat.getValue() + "\n");
                        }
                        MythicWeights.put(entry.getKey(), stats);
                    }
                }
            }



        } catch (IOException e) {
            e.printStackTrace();

        }
    }


}

