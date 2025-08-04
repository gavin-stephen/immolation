package org.untitled.immolation.client.keybinds;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.untitled.immolation.client.features.ItemWeight;
import org.untitled.immolation.client.gui.Drawing;

public class KeyBindManager {
    public static KeyBinding keyBinding1;
    public static KeyBinding keyBinding2;
    public static void register() {
        keyBinding1 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.immolation.openmenu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.immolation.test"
        ));
        keyBinding2 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.immolation.testing",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                "category.immolation.test"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding1.wasPressed()) {
                System.out.println("key 1 was pressed");
                client.player.sendMessage(Text.literal("Key 1 was pressed! "), false);
                if (client.getInstance().currentScreen instanceof Drawing) {
                    client.setScreen(null);
                }
                client.setScreen(new Drawing(Text.empty()));

            }
            while (keyBinding2.wasPressed()) {
                System.out.println("key 2 was pressed");
                System.out.println(ItemWeight.MythicWeights.size());
                client.player.sendMessage(Text.literal("Itemweight size: " + ItemWeight.MythicWeights.size()), false);
                client.player.sendMessage(Text.literal("Crabs : " + ItemWeight.MythicWeights.get("Crusade Sabatons")), false);

                //PEAK WE HAVFE THE DATA LOADED
                client.player.sendMessage(Text.literal("Immolation stats  : " + ItemWeight.MythicWeights.get("Immolation")), false);

            }
        });
    }
}
