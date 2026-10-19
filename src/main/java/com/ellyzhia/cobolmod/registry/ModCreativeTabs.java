package com.ellyzhia.cobolmod.registry;

import com.ellyzhia.cobolmod.CobolMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Onglet créatif "Ellyzhia" qui regroupe tout le contenu du mod.
 * Chaque nouvel item/bloc doit être ajouté dans displayItems pour y apparaître.
 */
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CobolMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ELLYZHIA_TAB =
            CREATIVE_MODE_TABS.register("ellyzhia_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cobolmod.ellyzhia_tab"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.ELLYZHIA_SHARD.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ELLYZHIA_SHARD.get());
                        output.accept(ModItems.ELLYZHIA_BLOCK_ITEM.get());
                    })
                    .build());
}
