package us.jcedeno.deltauhc.bukkit.stages.ingame.tasks;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static us.jcedeno.deltauhc.bukkit.common.utils.StringUtils.formatTime;

import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;

import net.kyori.adventure.text.minimessage.MiniMessage;
import us.jcedeno.deltauhc.bukkit.DeltaUHC;
import us.jcedeno.deltauhc.bukkit.config.GameConfig;
import us.jcedeno.deltauhc.bukkit.locations.Locations;
import us.jcedeno.deltauhc.bukkit.stages.ingame.InGameStage;

public class GameLoop implements Runnable {
    private final GameConfig cfg;
    private final InGameStage stage;
    private final MiniMessage mini = miniMessage();

    public GameLoop(final GameConfig cfg, final InGameStage stage) {
        this.cfg = cfg;
        this.stage = stage;
    }

    @Override
    public void run() {
        var time = cfg.increaseGameTime();

        if (time == cfg.getShrinkStartTime()) {
            // Start border shrink
            Bukkit.getScheduler().runTask(DeltaUHC.getGame(),
                    () -> Locations.getGameWorld().getWorldBorder().setSize(
                            cfg.getRadiusFinalSize() * 2,
                            cfg.getShrinkDuration()));
            Bukkit.broadcast(
                    mini.deserialize("<yellow>[!!] The world border will begin to shrink and it will take <white>"
                            + formatTime(cfg.getShrinkDuration())
                            + "</white> to complete shrinking down to a radius of <white>"
                            + cfg.getRadiusFinalSize() + "</white> blocks!</yellow>"));
            Bukkit.getOnlinePlayers()
                    .forEach(p -> p.playNote(p.getLocation(), Instrument.CHIME, Note.flat(1, Tone.A)));
        }

        if (time == cfg.getHealTime()) {

            DeltaUHC.runSync(() -> {
                Bukkit.getOnlinePlayers().forEach(p -> {
                    p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                    p.setFoodLevel(20);
                });
                Bukkit.broadcast(mini.deserialize("<#89ff9f>[+] Final heal"));
            });
        }

        if (time == cfg.getPvpTime()) {
            cfg.setPvp(true);
            Bukkit.broadcast(mini.deserialize("<#DC14A0>[⚔] PVP has been enabled."));
        }

        int timeUntilHeal = getTimeUntilEvent(time, cfg.getHealTime());
        int timeUntilPvP = getTimeUntilEvent(time, cfg.getPvpTime());
        int timeUntilShrink = getTimeUntilEvent(time, cfg.getShrinkStartTime());

        int timeUntilNextEvent = Math.min(Math.min(timeUntilHeal, timeUntilPvP), timeUntilShrink);

        String nextEventName = null;
        if (timeUntilNextEvent == timeUntilHeal) {
            nextEventName = "Final Heal";
        } else if (timeUntilNextEvent == timeUntilPvP) {
            nextEventName = "PvP";
        } else if (timeUntilNextEvent == timeUntilShrink) {
            nextEventName = "Border Shrink";
        }
        final String actualEvent = nextEventName;

        Bukkit.getOnlinePlayers().stream().forEach((p) -> {
            if (actualEvent != null) {
                p.sendActionBar(
                        mini.deserialize(actualEvent + ": <green>" + formatTime(timeUntilNextEvent)));
            }

            DeltaUHC.getGame().getBoard(p).updateLines(stage.getProcessedLines());
        });

    }

    private int getTimeUntilEvent(int currentTime, int eventTime) {
        return eventTime > currentTime ? eventTime - currentTime : Integer.MAX_VALUE;
    }

}
