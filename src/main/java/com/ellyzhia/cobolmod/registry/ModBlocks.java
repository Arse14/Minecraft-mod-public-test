package com.ellyzhia.cobolmod.registry;

import com.ellyzhia.cobolmod.CobolMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Tous les blocs du mod sont déclarés ici.
 * <p>
 * Pour ajouter un bloc : une constante ici + son BlockItem dans {@link ModItems}, puis
 * - un blockstate dans assets/cobolmod/blockstates/&lt;nom&gt;.json
 * - un modèle de bloc dans assets/cobolmod/models/block/&lt;nom&gt;.json
 * - un modèle d'item dans assets/cobolmod/models/item/&lt;nom&gt;.json
 * - une texture dans assets/cobolmod/textures/block/&lt;nom&gt;.png
 * - une loot table dans data/cobolmod/loot_table/blocks/&lt;nom&gt;.json (pour le drop)
 */
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CobolMod.MODID);

    /** Bloc de test : "cobolmod:ellyzhia_block". Dur comme de la pierre, se mine à la pioche. */
    public static final DeferredBlock<Block> ELLYZHIA_BLOCK = BLOCKS.registerSimpleBlock("ellyzhia_block",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 7));
}
