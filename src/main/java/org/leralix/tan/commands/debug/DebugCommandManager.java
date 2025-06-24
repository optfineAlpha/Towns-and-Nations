package org.leralix.tan.commands.debug;

import org.leralix.lib.commands.CommandManager;
import org.leralix.tan.commands.debug.ChunkStats;
import org.leralix.lib.commands.MainHelpCommand;

public class DebugCommandManager extends CommandManager {

    public DebugCommandManager(){
    super("tan.debug.commands");

    addSubCommand(new SaveData());
    addSubCommand(new CreateBackup());
    addSubCommand(new ColorCode());
    addSubCommand(new SkipDay());
    addSubCommand(new PlaySound());
    addSubCommand(new SendReport());
    addSubCommand(new ChunkStats());
    addSubCommand(new MainHelpCommand(this));
}
    
    
    @Override
    public String getName() {
        return "tandebug";
    }
}
