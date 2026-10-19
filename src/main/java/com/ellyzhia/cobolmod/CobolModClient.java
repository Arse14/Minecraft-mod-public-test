package com.ellyzhia.cobolmod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Code exécuté uniquement côté client (jamais sur un serveur dédié).
 * Rendus, écrans, touches, etc. iront ici ou dans un package "client".
 */
@Mod(value = CobolMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CobolMod.MODID, value = Dist.CLIENT)
public class CobolModClient {
    public CobolModClient(ModContainer container) {
        // Écran de config accessible depuis Mods > Ellyzhia > Config
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        CobolMod.LOGGER.info("[Ellyzhia] Setup client terminé.");
    }
}
