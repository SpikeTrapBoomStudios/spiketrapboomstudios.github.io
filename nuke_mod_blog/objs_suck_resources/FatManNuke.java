package com.stbstudios.spikesnukes.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEventListener;
import org.jetbrains.annotations.Nullable;

public class FatManNuke extends Block implements EntityBlock {
    public static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties
            .copy(Blocks.OBSIDIAN)
            .strength(1.5f)
            .sound(SoundType.STONE);

    public FatManNuke() {
        super(PROPERTIES);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> tBlockEntityType) {
        return EntityBlock.super.getTicker(level, blockState, tBlockEntityType);
    }

    @Override
    public @Nullable <T extends BlockEntity> GameEventListener getListener(ServerLevel level, T t) {
        return EntityBlock.super.getListener(level, t);
    }
}
