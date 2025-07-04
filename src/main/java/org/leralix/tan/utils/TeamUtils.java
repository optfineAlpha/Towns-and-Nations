package org.leralix.tan.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.storage.stored.PlayerDataStorage;

/**
 * Utility class for handling teams for scoreboard color coding
 */
public class TeamUtils {

    private TeamUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Update the color of all the scoreboards
     */
    public static void updateAllScoreboardColor(){
        if(TownsAndNations.getPlugin().colorCodeIsNotEnabled())
            return;
        for(Player player : Bukkit.getOnlinePlayers())
            setIndividualScoreBoard(player);
    }

    /**
     * Set the color of the scoreboard of a player
     * @param player    The player to set the scoreboard color of
     */

    public static void setIndividualScoreBoard(Player player) {
        Bukkit.getScheduler().runTask(TownsAndNations.getPlugin(), () -> setIndividualScoreBoardSync(player));
    }

    private static void setIndividualScoreBoardSync(Player player) {
        if(TownsAndNations.getPlugin().colorCodeIsNotEnabled())
            return;

        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if(scoreboardManager == null)
            return;

        Scoreboard board = scoreboardManager.getNewScoreboard();

        for (TownRelation relation : TownRelation.values()) {
            Team team = board.getTeam(relation.getName().toLowerCase());

            if (team == null) {
                team = board.registerNewTeam(relation.getName().toLowerCase());
            }

            team.setColor(relation.getColor());
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }
        player.setScoreboard(board);

        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {

            if(PlayerDataStorage.getInstance().get(otherPlayer).hasTown()){
                addPlayerToCorrectTeam(otherPlayer, player);
                if(!otherPlayer.getUniqueId().equals(player.getUniqueId())) //If player is not himself, no need to do it twice
                    addPlayerToCorrectTeam(player, otherPlayer);
            }
        }
    }

    /**
     * Add a player to the correct team
     * @param player        The player's scoreboard
     * @param playerToAdd         The player to add to the team scoreboard
     */
    public static void addPlayerToCorrectTeam(Player player, Player playerToAdd) {

        Scoreboard scoreboard = player.getScoreboard();
        if(!PlayerDataStorage.getInstance().get(playerToAdd).hasTown() || !PlayerDataStorage.getInstance().get(player).hasTown())
            return;

        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().get(player);
        TownRelation relation = tanPlayer.getRelationWithPlayer(playerToAdd);
        if(relation == null)
            return;

        Team playerTeam = scoreboard.getTeam(relation.getName().toLowerCase());
        if(playerTeam == null){ //Player did not have a town when he logged in. No team was created for him.
            TeamUtils.setIndividualScoreBoard(player);
            playerTeam = scoreboard.getTeam(relation.getName().toLowerCase());
        }
        if(playerTeam == null)
            return;
        playerTeam.addEntry(playerToAdd.getName());

    }

}
