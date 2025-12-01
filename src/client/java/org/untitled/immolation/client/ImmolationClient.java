package org.untitled.immolation.client;

import net.fabricmc.api.ClientModInitializer;
import org.untitled.immolation.client.keybinds.KeyBindManager;

public class ImmolationClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyBindManager.register(); //register and wait for key binds


        System.out.println("test");
    }
}
