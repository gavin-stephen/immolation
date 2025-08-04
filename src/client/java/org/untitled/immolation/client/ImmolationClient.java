package org.untitled.immolation.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import org.untitled.immolation.client.features.ItemWeight;
import org.untitled.immolation.client.keybinds.KeyBindManager;

public class ImmolationClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyBindManager.register(); //register and wait for key binds

        ItemWeight.loadWeights();
        System.out.println("test");
    }
}
