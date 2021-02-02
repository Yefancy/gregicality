package gregicadditions.worldgen;

import gregicadditions.network.IPSaveData;
import gregicadditions.network.MessageReservoirListSync;
import gregicadditions.network.NetworkHandler;
import gregicadditions.utils.GALog;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;


public class PumpjackHandler {
    public final static LinkedHashMap<ReservoirType, Integer> reservoirList = new LinkedHashMap<>();
    private final static Map<Integer, HashMap<Integer, Integer>> totalWeightMap = new HashMap<>();

    public static HashMap<DimensionChunkCoords, Long> timeCache = new HashMap<>();
    public static HashMap<DimensionChunkCoords, OilWorldInfo> oilCache = new HashMap<>();
    public static double oilChance = 100;

    private static final int depositSize = 1;

    /**
     * Gets amount of fluid in a specific chunk's reservoir in mB
     *
     * @param world  The world to test
     * @param chunkX X coordinate of desired chunk
     * @param chunkZ Z coordinate of desired chunk
     * @return mB of fluid in the given reservoir
     */
    public static int getFluidAmount(World world, int chunkX, int chunkZ) {
        if (world.isRemote)
            return 0;
        OilWorldInfo info = getOilWorldInfo(world, chunkX, chunkZ);
        if (info == null || (info.capacity == 0) || info.getType() == null || info.getType().fluid == null || (info.current == 0 && info.getType().replenishRate == 0))
            return 0;

        return info.current;
    }

    /**
     * Gets Fluid type in a specific chunk's reservoir
     *
     * @param world  The world to test
     * @param chunkX X coordinate of desired chunk
     * @param chunkZ Z coordinate of desired chunk
     * @return Fluid in given reservoir (or null if none)
     */
    public static Fluid getFluid(World world, int chunkX, int chunkZ) {
        if (world.isRemote)
            return null;

        OilWorldInfo info = getOilWorldInfo(world, chunkX, chunkZ);

        if (info == null || info.getType() == null) {
            return null;
        } else {
            return info.getType().getFluid();
        }
    }

    /**
     * Gets the mB/tick of fluid that is produced "residually" in the chunk (can be extracted while empty)
     *
     * @param world  The world to test
     * @param chunkX X coordinate of desired chunk
     * @param chunkZ Z coordinate of desired chunk
     * @return mB of fluid that can be extracted "residually"
     */
    public static int getResidualFluid(World world, int chunkX, int chunkZ) {
        OilWorldInfo info = getOilWorldInfo(world, chunkX, chunkZ);

        if (info == null || info.getType() == null || info.getType().fluid == null || (info.capacity == 0) || (info.current == 0 && info.getType().replenishRate == 0))
            return 0;

        DimensionChunkCoords coords = new DimensionChunkCoords(world.provider.getDimension(), chunkX / depositSize, chunkZ / depositSize);

        Long l = timeCache.get(coords);
        if (l == null) {
            timeCache.put(coords, world.getTotalWorldTime());
            return info.getType().replenishRate;
        }

        long lastTime = world.getTotalWorldTime();
        timeCache.put(coords, world.getTotalWorldTime());
        return lastTime != l ? info.getType().replenishRate : 0;
    }

    /**
     * Gets the OilWorldInfo object associated with the given chunk
     *
     * @param world  The world to retrieve
     * @param chunkX X coordinate of desired chunk
     * @param chunkZ Z coordinate of desired chunk
     * @return The OilWorldInfo corresponding w/ given chunk
     */
    public static OilWorldInfo getOilWorldInfo(World world, int chunkX, int chunkZ) {
        if (world.isRemote)
            return null;

        int dim = world.provider.getDimension();
        DimensionChunkCoords coords = new DimensionChunkCoords(dim, chunkX / depositSize, chunkZ / depositSize);

        OilWorldInfo worldInfo = oilCache.get(coords);
        if (worldInfo == null) {
            ReservoirType res = null;

            Random r = world.getChunk(chunkX / depositSize, chunkZ / depositSize).getRandomWithSeed(90210); // Antidote
            double dd = r.nextDouble();
            boolean empty = dd > oilChance;
            double size = r.nextDouble();
            int query = r.nextInt();

            if (!empty) {
                Biome biome = world.getBiomeForCoordsBody(new BlockPos(chunkX << 4, 64, chunkZ << 4));
                int totalWeight = getTotalWeight(dim, biome);
                if (totalWeight > 0) {
                    int weight = Math.abs(query % totalWeight);
                    for (Entry<ReservoirType, Integer> e : reservoirList.entrySet()) {
                        if (e.getKey().validDimension(dim) && e.getKey().validBiome(biome)) {
                            weight -= e.getValue();
                            if (weight < 0) {
                                res = e.getKey();
                                break;
                            }
                        }
                    }
                }
            }

            int capacity = 0;

            if (res != null) {
                capacity = (int) (size * (res.maxSize - res.minSize)) + res.minSize;
            }

            worldInfo = new OilWorldInfo();
            worldInfo.capacity = capacity;
            worldInfo.current = capacity;
            worldInfo.type = res;
            oilCache.put(coords, worldInfo);
        }
        return worldInfo;
    }

    /**
     * Depletes fluid from a given chunk
     *
     * @param world  World whose chunk to drain
     * @param chunkX Chunk x
     * @param chunkZ Chunk z
     * @param amount Amount of fluid in mB to drain
     */
    public static void depleteFluid(World world, int chunkX, int chunkZ, int amount) {
        OilWorldInfo info = getOilWorldInfo(world, chunkX, chunkZ);
        info.current = Math.max(0, info.current - amount);
        IPSaveData.setDirty(world.provider.getDimension());
    }

    /**
     * Gets the total weight of reservoir types for the given dimension ID and biome type
     *
     * @param dim   The dimension id to check
     * @param biome The biome type to check
     * @return The total weight associated with the dimension/biome pair
     */
    public static int getTotalWeight(int dim, Biome biome) {
        if (!totalWeightMap.containsKey(dim)) {
            totalWeightMap.put(dim, new HashMap<>());
        }

        Map<Integer, Integer> dimMap = totalWeightMap.get(dim);
        int biomeID = Biome.getIdForBiome(biome);

        if (dimMap.containsKey(biomeID)) {
            return dimMap.get(biomeID);
        }

        int totalWeight = 0;
        for (Entry<ReservoirType, Integer> e : reservoirList.entrySet()) {
            if (e.getKey().validDimension(dim) && e.getKey().validBiome(biome))
                totalWeight += e.getValue();
        }
        dimMap.put(biomeID, totalWeight);
        return totalWeight;
    }

    /**
     * Adds a reservoir type to the pool of valid reservoirs
     *
     * @param fluid         The String fluidid of the fluid for this reservoir
     * @param minSize       The minimum reservoir size, in mB
     * @param maxSize       The maximum reservoir size, in mB
     * @param replenishRate The rate at which fluid can be drained from this reservoir when empty, in mB/tick
     * @param weight        The weight for this reservoir to spawn
     * @return The created ReservoirType
     */
    public static ReservoirType addReservoir(String fluid, int minSize, int maxSize, int replenishRate, int weight) {
        ReservoirType mix = new ReservoirType(fluid, minSize, maxSize, replenishRate);
        reservoirList.put(mix, weight);
        return mix;
    }

    public static void recalculateChances(boolean mutePackets) {
        totalWeightMap.clear();
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER && !mutePackets) {
            HashMap<ReservoirType, Integer> packetMap = new HashMap<>();
            for (Entry<ReservoirType, Integer> e : PumpjackHandler.reservoirList.entrySet()) {
                if (e.getKey() != null && e.getValue() != null)
                    packetMap.put(e.getKey(), e.getValue());
            }
            NetworkHandler.INSTANCE.sendToAll(new MessageReservoirListSync(packetMap));
        }
    }

    private static final HashMap<Biome, String> biomeNames = new HashMap<>();

    /**
     * Get the biome name associated with a given biome (it's not a recommended method)
     *
     * @param biome The biome to get the name
     * @return The biome's name
     */
    public static String getBiomeName(Biome biome) {
        if (!biomeNames.containsKey(biome)) {
            String biomeName = ObfuscationReflectionHelper.getPrivateValue(Biome.class, biome, "field_76791_y"); // field_76791_y -> Biome.biomeName
            biomeNames.put(biome, biomeName.replace(" ", "").replace("_", "").toLowerCase());
        }
        return biomeNames.get(biome);
    }

    public static String convertConfigName(String str) {
        return str.replace(" ", "").toUpperCase();
    }

    public static String getTagDisplayName(String tag) {
        return tag.charAt(0) + tag.substring(1).toLowerCase();
    }

    public static String getBiomeDisplayName(String str) {
        String ret = "";
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c) && i != 0 && str.charAt(i - 1) != ' ') {
                ret = ret + " " + c;
            } else {
                ret = ret + c;
            }
        }
        return ret;
    }

    public static class ReservoirType {
        public String fluid;

        public int minSize;
        public int maxSize;
        public int replenishRate;

        public List<Integer> dimensionWhitelist = new ArrayList<>();
        public List<Integer> dimensionBlacklist = new ArrayList<>();

        public List<Integer> biomeWhitelist = new ArrayList<>();
        public List<Integer> biomeBlacklist = new ArrayList<>();

        private Fluid f;

        public ReservoirType(String fluid, int minSize, int maxSize, int replenishRate) {
            this.fluid = fluid;
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.replenishRate = replenishRate;
        }

        public Fluid getFluid() {
            if (fluid == null) return null;

            if (f == null) {
                f = FluidRegistry.getFluid(fluid);
            }

            return f;
        }

        public boolean validDimension(int dim) {
            if (dimensionWhitelist != null && dimensionWhitelist.size() > 0) {
                for (int white : dimensionWhitelist) {
                    if (dim == white)
                        return true;
                }
                return false;
            } else if (dimensionBlacklist != null && dimensionBlacklist.size() > 0) {
                for (int black : dimensionBlacklist) {
                    if (dim == black)
                        return false;
                }
                return true;
            }
            return true;
        }

        public boolean validBiome(Biome biome) {
            if (biome == null) return false;
            if (biomeWhitelist != null && biomeWhitelist.size() > 0) {
                GALog.logger.info("white {}", biomeWhitelist);
                for (Integer white : biomeWhitelist) {
                    if (white == null) continue;
                    GALog.logger.info("id {}", white);
                    GALog.logger.info("biome {}", Biome.getBiomeForId(white));
                    if (Biome.getBiomeForId(white).equals(biome))
                        return true;
                }
                return false;
            } else if (biomeBlacklist != null && biomeBlacklist.size() > 0) {
                GALog.logger.info("black {}", biomeBlacklist);
                for (Integer black : biomeBlacklist) {
                    if (black == null) continue;
                    GALog.logger.info("id {}", black);
                    GALog.logger.info("biome {}", Biome.getBiomeForId(black));
                    if (Biome.getBiomeForId(black).equals(biome))
                        return false;
                }
                return true;
            }
            return true;
        }

        public NBTTagCompound writeToNBT() {
            NBTTagCompound tag = new NBTTagCompound();

            tag.setString("fluid", fluid);

            tag.setInteger("minSize", minSize);
            tag.setInteger("maxSize", maxSize);
            tag.setInteger("replenishRate", replenishRate);

            tag.setIntArray("dimensionWhitelist", ArrayUtils.toPrimitive(dimensionWhitelist.toArray(new Integer[0])));
            tag.setIntArray("dimensionBlacklist", ArrayUtils.toPrimitive(dimensionBlacklist.toArray(new Integer[0])));

            NBTTagList wl = new NBTTagList();
            for (Integer s : biomeWhitelist) {
                wl.appendTag(new NBTTagString(s + ""));
            }
            tag.setTag("biomeWhitelist", wl);

            NBTTagList bl = new NBTTagList();
            for (Integer s : biomeBlacklist) {
                bl.appendTag(new NBTTagString(s + ""));
            }
            tag.setTag("biomeBlacklist", bl);

            return tag;
        }

        public static ReservoirType readFromNBT(NBTTagCompound tag) {
            String name = tag.getString("name");
            String fluid = tag.getString("fluid");

            int minSize = tag.getInteger("minSize");
            int maxSize = tag.getInteger("maxSize");
            int replenishRate = tag.getInteger("replenishRate");

            ReservoirType res = new ReservoirType(fluid, minSize, maxSize, replenishRate);

            res.dimensionWhitelist = Arrays.stream(tag.getIntArray("dimensionWhitelist")).boxed().collect(Collectors.toList());
            res.dimensionBlacklist = Arrays.stream(tag.getIntArray("dimensionBlacklist")).boxed().collect(Collectors.toList());

            NBTTagList wl = (NBTTagList) tag.getTag("biomeWhitelist");
            res.biomeWhitelist = new ArrayList<>(wl.tagCount());
            for (int i = 0; i < wl.tagCount(); i++) {
                res.biomeWhitelist.add(i, Integer.valueOf(wl.getStringTagAt(i)));
            }

            NBTTagList bl = (NBTTagList) tag.getTag("biomeBlacklist");
            res.biomeBlacklist = new ArrayList<>(bl.tagCount());
            for (int i = 0; i < bl.tagCount(); i++) {
                res.biomeBlacklist.add(i, Integer.valueOf(bl.getStringTagAt(i)));
            }


            return res;
        }
    }


    public static class OilWorldInfo {
        public ReservoirType type;
        public ReservoirType overrideType;
        public int capacity;
        public int current;

        public ReservoirType getType() {
            return (overrideType == null) ? type : overrideType;
        }

        public NBTTagCompound writeToNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("capacity", capacity);
            tag.setInteger("oil", current);
            if (type != null) {
                tag.setString("type", type.fluid);
            }
            if (overrideType != null) {
                tag.setString("overrideType", overrideType.fluid);
            }
            return tag;
        }

        public static OilWorldInfo readFromNBT(NBTTagCompound tag) {
            OilWorldInfo info = new OilWorldInfo();
            info.capacity = tag.getInteger("capacity");
            info.current = tag.getInteger("oil");

            if (tag.hasKey("type")) {
                String s = tag.getString("type");
                for (ReservoirType res : reservoirList.keySet()) {
                    if (s.equalsIgnoreCase(res.fluid))
                        info.type = res;
                }
            } else if (info.current > 0) {
                for (ReservoirType res : reservoirList.keySet()) {
                    if (res.fluid.equalsIgnoreCase("oil"))
                        info.type = res;
                }

                if (info.type == null) {
                    return null;
                }
            }

            if (tag.hasKey("overrideType")) {
                String s = tag.getString("overrideType");
                for (ReservoirType res : reservoirList.keySet()) {
                    if (s.equalsIgnoreCase(res.fluid))
                        info.overrideType = res;
                }
            }

            return info;
        }
    }
}