package org.leralix.tan.commands.admin;

import org.bukkit.command.CommandSender;
import org.leralix.lib.commands.SubCommand;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.listeners.ChunkLoadManager;
import org.leralix.tan.utils.TanChatUtils;

import java.util.Collections;
import java.util.List;

public class EmergencyCleanup extends SubCommand {

    @Override
    public String getName() {
        return "emergencycleanup";
    }

    @Override
    public String getDescription() {
        return "Force cleanup all loaded claimed chunks to prevent memory leaks";
    }

    @Override
    public int getArguments() {
        return 0;
    }

    @Override
    public String getSyntax() {
        return "/tanadmin emergencycleanup";
    }
    
    public List<String> getTabCompleteSuggestions(CommandSender commandSender, String lowerCase, String[] args){
        return Collections.emptyList();
    }
    
    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender.hasPermission("tan.admin.emergencycleanup"))) {
            sender.sendMessage(TanChatUtils.getTANString() + "You don't have permission to use this command.");
            return;
        }
        
        sender.sendMessage(TanChatUtils.getTANString() + "Starting emergency chunk cleanup...");
        ChunkLoadManager.getInstance().emergencyCleanup();
        sender.sendMessage(TanChatUtils.getTANString() + "Emergency cleanup completed. Check console for details.");
        sender.sendMessage(TanChatUtils.getTANString() + Lang.COMMAND_GENERIC_SUCCESS.get());
    }
}