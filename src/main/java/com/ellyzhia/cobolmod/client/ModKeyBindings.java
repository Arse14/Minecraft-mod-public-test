package com.ellyzhia.cobolmod.client;

import com.ellyzhia.cobolmod.CobolMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Touches du mod. La touche d'ouverture des raids est modifiable dans
 * Options > Commandes, catégorie "Ellyzhia".
 */
public final class ModKeyBindings {
    public static final String CATEGORY = "key.categories." + CobolMod.MODID;

    /** Touche U : ouvre l'écran des raids. */
    public static final KeyMapping OPEN_RAIDS = new KeyMapping(
            "key." + CobolMod.MODID + ".open_raids",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            CATEGORY);

    private ModKeyBindings() {}

    /** Enregistrement des touches : événement du bus "mod". */
    @EventBusSubscriber(modid = CobolMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_RAIDS);
        }
    }

    /** Détection de l'appui : événement du bus "jeu", à chaque tick client. */
    @EventBusSubscriber(modid = CobolMod.MODID, value = Dist.CLIENT)
    public static final class Handler {
        @SubscribeEvent
        static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            // consumeClick() renvoie true une seule fois par appui, même si la touche reste enfoncée
            while (OPEN_RAIDS.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(new RaidScreen());
                }
            }
        }
    }
}
