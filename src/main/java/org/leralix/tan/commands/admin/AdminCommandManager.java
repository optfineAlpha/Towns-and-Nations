package org.leralix.tan.commands.admin;

import org.leralix.lib.commands.CommandManager;
import org.leralix.lib.commands.MainHelpCommand;
import org.leralix.tan.commands.admin.EmergencyCleanup;

public class AdminCommandManager extends CommandManager {

	public AdminCommandManager(){
	    super("tan.admin.commands");
	    
	    addSubCommand(new OpenAdminGUI());
	    addSubCommand(new AddMoney());
	    addSubCommand(new SetMoney());
	    addSubCommand(new UnclaimAdminCommand());
	    addSubCommand(new ReloadCommand());
	    addSubCommand(new SudoPlayer());
	    addSubCommand(new EmergencyCleanup()); // <-- ADD THIS LINE
	    addSubCommand(new MainHelpCommand(this));
	}
	

    @Override
    public String getName() {
        return "tanadmin";
    }


}
