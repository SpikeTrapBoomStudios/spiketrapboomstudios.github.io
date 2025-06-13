package com.stbstudios.spikesnukes.blocks;

import com.stbstudios.spikesnukes.SpikesNukesMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SpikesNukesMod.MOD_ID);

    public static final RegistryObject<Block> FAT_MAN_NUKE = BLOCKS.register("fat_man_block", FatManNuke::new);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
