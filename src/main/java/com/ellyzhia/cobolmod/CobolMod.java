package com.ellyzhia.cobolmod;

import com.ellyzhia.cobolmod.registry.ModBlocks;
import com.ellyzhia.cobolmod.registry.ModCreativeTabs;
import com.ellyzhia.cobolmod.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Classe principale du mod Ellyzhia.
 * <p>
 * C'est le point d'entrée chargé par NeoForge. Elle se contente d'enregistrer
 * les registres (items, blocs, onglets créatifs) et la config ; la logique
 * de jeu vit dans les autres packages.
 */
@Mod(CobolMod.MODID)
public class CobolMod {
    /** Identifiant du mod. Doit correspondre à mod_id dans gradle.properties. */
    public static final String MODID = "cobolmod";

    public static final Logger LOGGER = LogUtils.getLogger();

    public CobolMod(IEventBus modEventBus, ModContainer modContainer) {
        // Registres : les objets sont réellement créés par NeoForge au bon moment du chargement.
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // Setup commun (client + serveur), appelé une fois les registres remplis.
        modEventBus.addListener(this::commonSetup);

        // Fichier de config généré dans config/cobolmod-common.toml
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[Ellyzhia] Chargement du mod {} terminé.", MODID);
    }
}
