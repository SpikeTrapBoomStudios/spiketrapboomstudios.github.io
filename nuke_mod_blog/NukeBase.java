package com.stbstudios.spikesnukes.explosives.nukes;

import com.stbstudios.spikesnukes.SpikesNukesMod;
import com.stbstudios.spikesnukes.networking.BasicSmokeExplosionPacket;
import com.stbstudios.spikesnukes.networking.NetworkHandler;
import com.stbstudios.spikesnukes.networking.SmokeParticlePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

@Mod.EventBusSubscriber(modid = SpikesNukesMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class NukeBase {
    public static float stretch = 2f;
    public static float poleCluster = .9f;

    protected Level level;
    protected Vec3 explosionPos;
    protected int radius;
    protected int power;
    protected double yieldKT;

    private static final int MAX_RAYS_PER_TICK = 30000;
    private final HashMap<ChunkPos, HashSet<BlockPos>> blockKillQueue = new HashMap<>();

    private final Queue<ExplosionRay> rayQueue = new ArrayDeque<>();

    private boolean doBlockKill = false;

    public NukeBase(Level level, Vec3 explosionPos, double yieldKT) {
        this.level = level;
        this.yieldKT = yieldKT;
        this.explosionPos = explosionPos;
        this.power = (int) NukeBase.getExplosionPowerFromYield(this.yieldKT);
    }

    public static double getExplosionRadiusFromYield(double yieldKT) {
        return 20.0 * Math.cbrt(yieldKT);
    }
    public static double getExplosionPowerFromYield(double yieldKT) {
        return 120.0 * Math.sqrt(yieldKT);
    }

    public void detonate() {
        this.radius = (int) getExplosionRadiusFromYield(this.yieldKT);
        castRaySphere((int) (17.372 * this.radius * this.radius));
        spawnPfx();
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void spawnPfx() {
        if (level instanceof ServerLevel) {
            if (yieldKT >= 100.0) {
                NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                        new SmokeParticlePacket(explosionPos.x, explosionPos.y, explosionPos.z));
            } else {
                NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                        new BasicSmokeExplosionPacket(explosionPos.x, explosionPos.y, explosionPos.z));
            }
        }
    }

    public static class ExplosionRay {
        Vec3 direction;
        float power;

        public ExplosionRay(Vec3 direction, float power) {
            this.direction = direction;
            this.power = power;
        }
    }

    public void castRaySphere(int rayCount) {
        final double offset = 2.0 / rayCount;
        final double increment = Math.PI * (3.0 - Math.sqrt(5)); //golden angle (rad)

        //Fibonacci Lattice Algorithm
        for (int i = 0; i < rayCount; i++) {
            double y = ((i * offset) - 1) + (offset / 2);
            y  = Math.signum(y) * Math.pow(Math.abs(y), poleCluster); //Artificial pole clustering because for some god-damn reason the distribution is inconsistent in the top/bottom
            double r = Math.sqrt(1 - y * y);

            double phi = i * increment;

            double x = Math.cos(phi) * r;
            double z = Math.sin(phi) * r;

            Vec3 direction = new Vec3(x, y, z).normalize();
            rayQueue.add(new ExplosionRay(direction, power));
        }
    }

    public void shootRay(ExplosionRay ray) {
        Vec3 dir = ray.direction.normalize();
        double maxDist = radius;
        Vec3 origin = explosionPos;

        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        int stepX = dir.x > 0 ? 1 : -1;
        int stepY = dir.y > 0 ? 1 : -1;
        int stepZ = dir.z > 0 ? 1 : -1;

        double tDeltaX = Math.abs(1.0 / dir.x);
        double tDeltaY = Math.abs(1.0 / dir.y)*stretch;
        double tDeltaZ = Math.abs(1.0 / dir.z);

        double tMaxX = ((stepX > 0 ? (x + 1) - origin.x : origin.x - x)) * tDeltaX;
        double tMaxY = ((stepY > 0 ? (y + 1) - origin.y : origin.y - y)) * tDeltaY;
        double tMaxZ = ((stepZ > 0 ? (z + 1) - origin.z : origin.z - z)) * tDeltaZ;

        double t = 0;

        while (t <= maxDist && ray.power > 0) {
            BlockPos thisPos = new BlockPos(x, y, z);
            ChunkPos thisChunk = new ChunkPos(thisPos);
            BlockState state = level.getBlockState(thisPos);

            if (state.getExplosionResistance(null, BlockPos.ZERO, null) <= ray.power && state.getBlock() != Blocks.AIR) {
                HashSet<BlockPos> currentChunkSet;
                if (blockKillQueue.containsKey(thisChunk)) {
                    currentChunkSet = blockKillQueue.get(thisChunk);
                } else {
                    currentChunkSet = new HashSet<>();
                }
                currentChunkSet.add(thisPos);
                blockKillQueue.put(thisChunk, currentChunkSet);
            } else if (state.getBlock() == Blocks.AIR) {
                ray.power -= .1f;
            }

            if (state.getFluidState().is(FluidTags.WATER)) {
                level.setBlock(thisPos, Blocks.AIR.defaultBlockState(), 3);
            }

            if (state.getFluidState().isEmpty()) {
                ray.power -= state.getExplosionResistance(null, BlockPos.ZERO, null) * 4;
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    t = tMaxX;
                    tMaxX += tDeltaX;
                } else {
                    z += stepZ;
                    t = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    t = tMaxY;
                    tMaxY += tDeltaY;
                } else {
                    z += stepZ;
                    t = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            }
        }
    }

    public void processHitBlocks() {
        if (blockKillQueue.isEmpty()) {
            System.out.println("Nuke Logic Complete");
            doBlockKill = false;
            MinecraftForge.EVENT_BUS.unregister(this);
            return;
        }

        ChunkPos nearestChunk = getNearestChunk();

        if (nearestChunk != null) {
            HashSet<BlockPos> blockPositions = blockKillQueue.remove(nearestChunk);
            if (blockPositions != null) {
                for (BlockPos pos : blockPositions) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private ChunkPos getNearestChunk() {
        ChunkPos nearestChunk = null;
        double minDistSq = Double.MAX_VALUE;
        for (ChunkPos chunk : blockKillQueue.keySet()) {
            double centerX = (chunk.x << 4) + 8;
            double centerZ = (chunk.z << 4) + 8;
            double dx = centerX - explosionPos.x;
            double dz = centerZ - explosionPos.z;
            double distSq = dx * dx + dz * dz;
            if (distSq < minDistSq) {
                minDistSq = distSq;
                nearestChunk = chunk;
            }
        }
        return nearestChunk;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent tick) {
        if (tick.phase != TickEvent.Phase.START) {
            return;
        }
        if (doBlockKill) {
            processHitBlocks();
            return;
        }

        int processedRays = 0;
        while (processedRays < MAX_RAYS_PER_TICK && !rayQueue.isEmpty()) {
            ExplosionRay ray = rayQueue.poll();
            if (ray != null) shootRay(ray);
            processedRays++;
        }

        if (rayQueue.isEmpty()) {
            System.out.println("All rays completed.");
            doBlockKill = true;
        }
    }
}
