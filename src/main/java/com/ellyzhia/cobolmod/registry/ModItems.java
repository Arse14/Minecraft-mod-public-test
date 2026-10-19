package com.ellyzhia.cobolmod.registry;

import com.ellyzhia.cobolmod.CobolMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Tous les items du mod sont déclarés ici.
 * <p>
 * Pour ajouter un item : une constante ici, puis
 * - un modèle dans assets/cobolmod/models/item/&lt;nom&gt;.json
 * - une texture dans assets/cobolmod/textures/item/&lt;nom&gt;.png
 * - une traduction dans assets/cobolmod/lang/fr_fr.json et en_us.json
 */
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CobolMod.MODID);

    /** Item de test : "cobolmod:ellyzhia_shard" (un éclat d'Ellyzhia). */
    public static final DeferredItem<Item> ELLYZHIA_SHARD = ITEMS.registerSimpleItem("ellyzhia_shard");

    /** Item correspondant au bloc de test, pour pouvoir le poser. */
    public static final DeferredItem<BlockItem> ELLYZHIA_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("ellyzhia_block", ModBlocks.ELLYZHIA_BLOCK);
}
