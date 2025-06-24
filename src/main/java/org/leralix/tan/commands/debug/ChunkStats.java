package org.leralix.tan.commands.debug;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.leralix.lib.commands.SubCommand;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.listeners.ChunkLoadManager;
import org.leralix.tan.utils.TanChatUtils;

import java.util.Collections;
import java.util.List;

public class ChunkStats extends SubCommand {

    @Override
    public String getName() {
        return "chunkstats";
    }

    @Override
    public String getDescription() {
        return "Check chunk loading statistics and memory usage";
    }

    @Override
    public int getArguments() {
        return 0;
    }

    @Override
    public String getSyntax() {
        return "/tandebug chunkstats";
    }
    
    public List<String> getTabCompleteSuggestions(CommandSender commandSender, String lowerCase, String[] args){
        return Collections.emptyList();
    }
    
    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) && !(sender instanceof ConsoleCommandSender)) {
            return;
        }
        
        String stats = ChunkLoadManager.getInstance().getStatistics();
        sender.sendMessage(TanChatUtils.getTANString() + "Chunk Loading Statistics:");
        sender.sendMessage(TanChatUtils.getTANString() + stats);
        
        // Additional memory information
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryPercentage = ((double) usedMemory / maxMemory) * 100;
        
        sender.sendMessage(TanChatUtils.getTANString() + String.format(
            "Memory Usage: %.1f%% (%d MB / %d MB)", 
            memoryPercentage, 
            usedMemory / (1024 * 1024), 
            maxMemory / (1024 * 1024)
        ));
        
        sender.sendMessage(TanChatUtils.getTANString() + Lang.COMMAND_GENERIC_SUCCESS.get());
    }
}