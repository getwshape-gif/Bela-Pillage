package fr.belarion.belapillage.durability;

import org.bukkit.Location;
import org.bukkit.block.Block;

/**
 * Cle immuable identifiant un bloc dans le monde par (monde, x, y, z), independamment
 * de tout objet Bukkit {@link org.bukkit.block.Block} (qui n'est qu'une vue volatile).
 * <p>
 * Utilisee comme cle de {@link DurabilityManager} : seuls les blocs ayant deja subi au
 * moins une explosion possedent une entree, ce qui garde la structure en memoire
 * proportionnelle au nombre de blocs reellement endommages (et non a la totalite des
 * blocs proteges du monde), meme avec plusieurs milliers d'entrees.
 */
public final class BlockKey {

    private final String world;
    private final int x;
    private final int y;
    private final int z;

    public BlockKey(String world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static BlockKey of(Block block) {
        return new BlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockKey of(Location location) {
        return new BlockKey(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockKey)) return false;
        BlockKey other = (BlockKey) o;
        return x == other.x && y == other.y && z == other.z && world.equals(other.world);
    }

    @Override
    public int hashCode() {
        int result = world.hashCode();
        result = 31 * result + x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }

    @Override
    public String toString() {
        return world + ";" + x + ";" + y + ";" + z;
    }
}
