package org.leralix.tan.utils;

import org.bukkit.entity.Player;
import org.leralix.lib.utils.config.ConfigTag;
import org.leralix.lib.utils.config.ConfigUtil;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.storage.stored.PlayerDataStorage;

/**
 * Utility class for handling prefix
 */
public class prefixUtil {
    /**
     * Add the town prefix to a player's name
     * @param player The player to add the prefix to
     */
    public static void addPrefix(Player player){
        if(!ConfigUtil.getCustomConfig(ConfigTag.MAIN).getBoolean("EnableTownPrefix",true)){
            return;
        }
        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().get(player);

        if (tanPlayer.getTown() != null){
            String prefix = tanPlayer.getTown().getColoredTag() + " ";

            player.setPlayerListName(prefix + player.getName());
            player.setDisplayName(prefix + player.getName());
        }
    }

}
