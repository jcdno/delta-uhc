package us.jcedeno.deltauhc.bukkit.stages.global;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.mrmicky.fastboard.FastBoard;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import us.jcedeno.deltauhc.bukkit.DeltaUHC;

public class GlobalStage extends AbstractStage implements Listener{
    @Getter
    private final Map<UUID, FastBoard> boards = new HashMap<>();

    @Override
    public void registerTasks() {
        Bukkit.getPluginManager().registerEvents(this, DeltaUHC.getGame());
    }

    @Override
    public void unregisterTasks() {

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        FastBoard board = new FastBoard(player);

        board.updateTitle(ChatColor.RED + "       Δ UHC       ");

        this.boards.put(player.getUniqueId(), board);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        FastBoard board = this.boards.remove(player.getUniqueId());

        if (board != null) {
            board.delete();
        }
    }

    
}
