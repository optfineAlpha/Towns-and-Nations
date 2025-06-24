package org.leralix.tan.storage.stored;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.bukkit.Chunk;
import org.leralix.tan.dataclass.chunk.*;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.TownsAndNations;
import org.tan.api.getters.TanTerritoryManager;
import org.leralix.tan.listeners.ChunkLoadManager;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class NewClaimedChunkStorage {

    private final Map<String, ClaimedChunk2> claimedChunksMap = new HashMap<>();

    private static NewClaimedChunkStorage instance;

    private NewClaimedChunkStorage() {
        loadStats();
    }

    public static synchronized NewClaimedChunkStorage getInstance() {
        if (instance == null) {
            instance = new NewClaimedChunkStorage();
        }
        return instance;
    }

    private static String getChunkKey(Chunk chunk) {
        return getChunkKey(chunk.getX(),chunk.getZ(),chunk.getWorld().getUID().toString());
    }
    private static String getChunkKey(ClaimedChunk2 chunk) {
        return getChunkKey(chunk.getX(),chunk.getZ(),chunk.getWorldUUID());
    }
    private static String getChunkKey(int x, int z, String chunkWorldUID){
        return x + "," + z + "," + chunkWorldUID;
    }


    public Map<String, ClaimedChunk2> getClaimedChunksMap() {
        return claimedChunksMap;
    }

    public boolean isChunkClaimed(Chunk chunk) {
        return claimedChunksMap.containsKey(getChunkKey(chunk));
    }

    public String getChunkOwnerID(Chunk chunk) {
        ClaimedChunk2 claimedChunk = claimedChunksMap.get(getChunkKey(chunk));
        return claimedChunk != null ? claimedChunk.getOwnerID() : null;
    }

    public TownData getChunkOwnerTown(Chunk chunk) {
        if (!isChunkClaimed(chunk))
            return null;
        return TownDataStorage.getInstance().get(getChunkOwnerID(chunk));
    }

    public Collection<ClaimedChunk2> getAllChunkFrom(TerritoryData territoryData){
        List<ClaimedChunk2> chunks = new ArrayList<>();
        for(ClaimedChunk2 chunk : claimedChunksMap.values()){
            if(chunk.getOwnerID().equals(territoryData.getID())){
                chunks.add(chunk);
            }
        }
        return Collections.unmodifiableCollection(chunks);
    }

    public String getChunkOwnerName(Chunk chunk) {

        ClaimedChunk2 claimedChunk = claimedChunksMap.get(getChunkKey(chunk));

        if(claimedChunk instanceof TownClaimedChunk){
            return TownDataStorage.getInstance().get(claimedChunk.getOwnerID()).getName();
        }
        else if(claimedChunk instanceof RegionClaimedChunk){
            return RegionDataStorage.getInstance().get(claimedChunk.getOwnerID()).getName();
        }
        return null;
    }

    public boolean isOwner(Chunk chunk, String townID) {

        ClaimedChunk2 cChunk = claimedChunksMap.get(getChunkKey(chunk));
        if(cChunk instanceof TownClaimedChunk){
            return cChunk.getOwnerID().equals(townID);
        }
        else if(cChunk instanceof RegionClaimedChunk regionClaimedChunk){
            return regionClaimedChunk.getRegion().isTownInRegion(TownDataStorage.getInstance().get(townID));
        }
        return false;
    }

    public void claimTownChunk(Chunk chunk, String ownerID) {
        claimedChunksMap.put(getChunkKey(chunk), new TownClaimedChunk(chunk, ownerID));
        save();
    }
    public void claimRegionChunk(Chunk chunk, String ownerID){
        claimedChunksMap.put(getChunkKey(chunk), new RegionClaimedChunk(chunk, ownerID));
        save();
    }
    public void claimLandmarkChunk(Chunk chunk, String ownerID){
        claimedChunksMap.put(getChunkKey(chunk), new LandmarkClaimedChunk(chunk, ownerID));
        save();
    }

    public boolean isAdjacentChunkClaimedBySameTown(Chunk chunk, String townID) {

        List<String> adjacentChunkKeys = Arrays.asList(
                getChunkKey(chunk.getX() + 1, chunk.getZ(), chunk.getWorld().getUID().toString()),
                getChunkKey(chunk.getX() - 1, chunk.getZ(), chunk.getWorld().getUID().toString()),
                getChunkKey(chunk.getX(), chunk.getZ() + 1, chunk.getWorld().getUID().toString()),
                getChunkKey(chunk.getX(), chunk.getZ() - 1, chunk.getWorld().getUID().toString())
        );

        for (String adjacentChunkKey : adjacentChunkKeys) {
            ClaimedChunk2 adjacentClaimedChunk = claimedChunksMap.get(adjacentChunkKey);
            if (adjacentClaimedChunk != null && adjacentClaimedChunk.getOwnerID().equals(townID)) {
                return true;
            }
        }
        return false;
    }

    public void unclaimChunk(ClaimedChunk2 claimedChunk) {
        if (claimedChunk == null) {
            return;
        }
        
        // Get the chunk before removing from map
        Chunk chunk = claimedChunk.getChunk();
        
        // Remove from the claimed chunks map
        claimedChunksMap.remove(getChunkKey(claimedChunk));
        
        // CRITICAL: Notify ChunkLoadManager that this chunk was unclaimed
        // This will schedule the chunk for unloading to prevent memory leaks
        if (chunk != null) {
            ChunkLoadManager.getInstance().onChunkUnclaimed(chunk);
        }
        
        // Save the updated data
        save();
        
        // Log for debugging (can be removed in production)
        TownsAndNations.getPlugin().getLogger().info(
            "[TaN] Unclaimed chunk at " + (chunk != null ? chunk.getX() + "," + chunk.getZ() : "unknown") + 
            " - scheduled for unload to prevent memory leak"
        );
    }

    /**
     * Modified unclaimChunk method that properly handles memory cleanup
     */
    public void unclaimChunk(Chunk chunk) {
        if (chunk == null) {
            return;
        }
        
        // Remove from the claimed chunks map
        claimedChunksMap.remove(getChunkKey(chunk));
        
        // CRITICAL: Notify ChunkLoadManager that this chunk was unclaimed
        // This will schedule the chunk for unloading to prevent memory leaks
        ChunkLoadManager.getInstance().onChunkUnclaimed(chunk);
        
        // Save the updated data
        save();
        
        // Log for debugging (can be removed in production)
        TownsAndNations.getPlugin().getLogger().info(
            "[TaN] Unclaimed chunk at " + chunk.getX() + "," + chunk.getZ() + 
            " - scheduled for unload to prevent memory leak"
        );
    }

    /**
     * Modified unclaimAllChunksFromTerritory method that properly handles memory cleanup
     */
    public void unclaimAllChunksFromTerritory(TerritoryData territoryData) {
        if (territoryData == null) {
            return;
        }
        
        // Get all chunks before unclaiming them
        Collection<ClaimedChunk2> chunksToUnclaim = getAllChunkFrom(territoryData);
        List<Chunk> chunkObjects = new ArrayList<>();
        
        // Collect all chunk objects
        for (ClaimedChunk2 claimedChunk : chunksToUnclaim) {
            Chunk chunk = claimedChunk.getChunk();
            if (chunk != null) {
                chunkObjects.add(chunk);
            }
        }
        
        // Remove from map
        unclaimAllChunkFromID(territoryData.getID());
        
        // CRITICAL: Notify ChunkLoadManager for each unclaimed chunk
        // This will schedule all chunks for unloading to prevent memory leaks
        for (Chunk chunk : chunkObjects) {
            ChunkLoadManager.getInstance().onChunkUnclaimed(chunk);
        }
        
        // Log for debugging (can be removed in production)
        TownsAndNations.getPlugin().getLogger().info(
            "[TaN] Unclaimed " + chunkObjects.size() + " chunks from territory " + 
            territoryData.getName() + " - all scheduled for unload to prevent memory leak"
        );
    }

    public void unclaimAllChunkFromID(String id) {
        Iterator<Map.Entry<String, ClaimedChunk2>> iterator = claimedChunksMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ClaimedChunk2> entry = iterator.next();
            ClaimedChunk2 chunk = entry.getValue();
            if (chunk.getOwnerID().equals(id)) {
                iterator.remove();
            }
        }
    }


    public ClaimedChunk2 get(Chunk chunk) {
        ClaimedChunk2 claimedChunk = claimedChunksMap.get(getChunkKey(chunk));
        if (claimedChunk == null) {
            return new WildernessChunk(chunk);
        }
        return claimedChunk;
    }

    private void loadStats() {
        Gson gson = new Gson();
        File file = new File(TownsAndNations.getPlugin().getDataFolder().getAbsolutePath() + "/TAN - Claimed Chunks.json");
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, JsonObject>>() {}.getType();
                Map<String, JsonObject> jsonData = gson.fromJson(reader, type);

                for (Map.Entry<String, JsonObject> entry : jsonData.entrySet()) {
                    JsonObject chunkData = entry.getValue();
                    int x = chunkData.get("x").getAsInt();
                    int z = chunkData.get("z").getAsInt();
                    String worldUUID = chunkData.get("worldUUID").getAsString();
                    String ownerID = chunkData.get("ownerID").getAsString();

                    // Déterminer si c'est une ville ou une région en fonction de la première lettre de ownerID
                    if (ownerID.startsWith("T")) {
                        TownClaimedChunk townChunk = new TownClaimedChunk(x, z, worldUUID, ownerID);
                        claimedChunksMap.put(entry.getKey(), townChunk);
                    }
                    else if (ownerID.startsWith("R")) {
                        RegionClaimedChunk regionChunk = new RegionClaimedChunk(x, z, worldUUID, ownerID);
                        claimedChunksMap.put(entry.getKey(), regionChunk);
                    }
                    else if (ownerID.startsWith("L")) {
                        LandmarkClaimedChunk landmarkClaimedChunk = new LandmarkClaimedChunk(x, z, worldUUID, ownerID);
                        claimedChunksMap.put(entry.getKey(), landmarkClaimedChunk);
                    }
                }
            } catch (IOException e) {
                TownsAndNations.getPlugin().getLogger().severe("Error while loading claimed chunks stats");
            }
        }
    }


    public void save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File(TownsAndNations.getPlugin().getDataFolder().getAbsolutePath() + "/TAN - Claimed Chunks.json");
        file.getParentFile().mkdirs();
        try {
            if (!file.exists()){
                file.createNewFile();
            }
        } catch (IOException e) {
            TownsAndNations.getPlugin().getLogger().severe("Error while creating claimed chunks file");
        }
        try (FileWriter writer = new FileWriter(file, false);){
            gson.toJson(claimedChunksMap, writer);
        } catch (IOException e) {
            TownsAndNations.getPlugin().getLogger().severe("Error while storing claimed chunks in file");
        }
    }

    
    
    

}
