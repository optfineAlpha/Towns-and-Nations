package org.leralix.tan.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.chunk.ClaimedChunk2;
import org.leralix.tan.storage.stored.NewClaimedChunkStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chunk loading/unloading to prevent memory leaks from claimed chunks
 * staying loaded indefinitely in memory.
 */
public class ChunkLoadManager implements Listener {

    private static ChunkLoadManager instance;
    private final Map<String, Long> chunkLoadTimes = new ConcurrentHashMap<>();
    private final Map<String, Boolean> forceLoadedChunks = new ConcurrentHashMap<>();
    
    // Configuration values - can be moved to config.yml later
    private static final long CHUNK_UNLOAD_DELAY = 300000; // 5 minutes in milliseconds
    private static final int MAX_LOADED_CLAIMED_CHUNKS = 100; // Maximum claimed chunks to keep loaded
    
    private ChunkLoadManager() {
        startCleanupTask();
    }
    
    public static ChunkLoadManager getInstance() {
        if (instance == null) {
            instance = new ChunkLoadManager();
        }
        return instance;
    }
    
    /**
     * Called when a chunk loads - track if it's a claimed chunk
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        String chunkKey = getChunkKey(chunk);
        
        // Only track claimed chunks
        if (NewClaimedChunkStorage.getInstance().isChunkClaimed(chunk)) {
            chunkLoadTimes.put(chunkKey, System.currentTimeMillis());
            
            // Log for debugging (can be removed in production)
            TownsAndNations.getPlugin().getLogger().info(
                "[ChunkLoadManager] Claimed chunk loaded: " + chunkKey + 
                " | Total tracked: " + chunkLoadTimes.size()
            );
        }
    }
    
    /**
     * Called when a chunk unloads - clean up tracking
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        String chunkKey = getChunkKey(chunk);
        
        // Remove from tracking
        chunkLoadTimes.remove(chunkKey);
        forceLoadedChunks.remove(chunkKey);
        
        // Log for debugging (can be removed in production)
        if (NewClaimedChunkStorage.getInstance().isChunkClaimed(chunk)) {
            TownsAndNations.getPlugin().getLogger().info(
                "[ChunkLoadManager] Claimed chunk unloaded: " + chunkKey + 
                " | Total tracked: " + chunkLoadTimes.size()
            );
        }
    }
    
    /**
     * Force unload a specific chunk if it's been loaded too long
     */
    public void forceUnloadChunk(Chunk chunk) {
        if (chunk == null || !chunk.isLoaded()) {
            return;
        }
        
        String chunkKey = getChunkKey(chunk);
        
        // Only unload if it's not force-loaded by other plugins or has players
        if (!chunk.isForceLoaded() && chunk.getEntities().length == 0) {
            // Unload the chunk
            chunk.unload(true);
            
            // Clean up tracking
            chunkLoadTimes.remove(chunkKey);
            forceLoadedChunks.remove(chunkKey);
            
            TownsAndNations.getPlugin().getLogger().info(
                "[ChunkLoadManager] Force unloaded chunk: " + chunkKey
            );
        }
    }
    
    /**
     * Called when a chunk is unclaimed - ensure it gets unloaded
     */
    public void onChunkUnclaimed(Chunk chunk) {
        if (chunk == null) {
            return;
        }
        
        String chunkKey = getChunkKey(chunk);
        
        // Remove from tracking immediately
        chunkLoadTimes.remove(chunkKey);
        forceLoadedChunks.remove(chunkKey);
        
        // Schedule chunk for unloading after a short delay to ensure any ongoing operations complete
        new BukkitRunnable() {
            @Override
            public void run() {
                if (chunk.isLoaded() && !NewClaimedChunkStorage.getInstance().isChunkClaimed(chunk)) {
                    forceUnloadChunk(chunk);
                }
            }
        }.runTaskLater(TownsAndNations.getPlugin(), 100L); // 5 second delay
        
        TownsAndNations.getPlugin().getLogger().info(
            "[ChunkLoadManager] Scheduled unclaimed chunk for unload: " + chunkKey
        );
    }
    
    /**
     * Periodic cleanup task to unload chunks that have been loaded too long
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupLoadedChunks();
            }
        }.runTaskTimer(TownsAndNations.getPlugin(), 6000L, 6000L); // Run every 5 minutes
    }
    
    /**
     * Clean up chunks that have been loaded for too long or exceed the limit
     */
    private void cleanupLoadedChunks() {
        long currentTime = System.currentTimeMillis();
        int unloadedCount = 0;
        int totalTracked = chunkLoadTimes.size();
        
        // If we're tracking too many chunks, be more aggressive about unloading
        boolean aggressiveCleanup = totalTracked > MAX_LOADED_CLAIMED_CHUNKS;
        long unloadThreshold = aggressiveCleanup ? CHUNK_UNLOAD_DELAY / 2 : CHUNK_UNLOAD_DELAY;
        
        // Create a copy to avoid concurrent modification
        Map<String, Long> chunksCopy = new HashMap<>(chunkLoadTimes);
        
        for (Map.Entry<String, Long> entry : chunksCopy.entrySet()) {
            String chunkKey = entry.getKey();
            long loadTime = entry.getValue();
            
            // Check if chunk has been loaded for too long
            if (currentTime - loadTime > unloadThreshold) {
                Chunk chunk = getChunkFromKey(chunkKey);
                
                if (chunk != null && chunk.isLoaded()) {
                    // Check if chunk has any players nearby (within 2 chunks)
                    boolean hasNearbyPlayers = chunk.getWorld().getPlayers().stream()
                        .anyMatch(player -> {
                            Chunk playerChunk = player.getLocation().getChunk();
                            return Math.abs(playerChunk.getX() - chunk.getX()) <= 2 && 
                                   Math.abs(playerChunk.getZ() - chunk.getZ()) <= 2;
                        });
                    
                    // Only unload if no nearby players
                    if (!hasNearbyPlayers) {
                        forceUnloadChunk(chunk);
                        unloadedCount++;
                    }
                }
            }
        }
        
        if (unloadedCount > 0 || totalTracked > 50) {
            TownsAndNations.getPlugin().getLogger().info(
                String.format("[ChunkLoadManager] Cleanup complete. Unloaded: %d, Still tracking: %d, Aggressive: %s", 
                    unloadedCount, chunkLoadTimes.size(), aggressiveCleanup)
            );
        }
    }
    
    /**
     * Get current statistics for monitoring
     */
    public String getStatistics() {
        return String.format("Tracked claimed chunks: %d, Force-loaded: %d", 
            chunkLoadTimes.size(), forceLoadedChunks.size());
    }
    
    /**
     * Generate a unique key for a chunk
     */
    private String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();
    }
    
    /**
     * Get chunk from key string
     */
    private Chunk getChunkFromKey(String key) {
        try {
            String[] parts = key.split("_");
            if (parts.length != 3) return null;
            
            String worldName = parts[0];
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            
            return Bukkit.getWorld(worldName).getChunkAt(x, z);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Emergency cleanup method - unload all tracked chunks
     */
    public void emergencyCleanup() {
        TownsAndNations.getPlugin().getLogger().warning(
            "[ChunkLoadManager] Emergency cleanup triggered! Unloading all tracked chunks."
        );
        
        Map<String, Long> chunksCopy = new HashMap<>(chunkLoadTimes);
        int unloadedCount = 0;
        
        for (String chunkKey : chunksCopy.keySet()) {
            Chunk chunk = getChunkFromKey(chunkKey);
            if (chunk != null && chunk.isLoaded()) {
                forceUnloadChunk(chunk);
                unloadedCount++;
            }
        }
        
        // Clear all tracking
        chunkLoadTimes.clear();
        forceLoadedChunks.clear();
        
        TownsAndNations.getPlugin().getLogger().warning(
            String.format("[ChunkLoadManager] Emergency cleanup complete. Unloaded %d chunks.", unloadedCount)
        );
    }
}