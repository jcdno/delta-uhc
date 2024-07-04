package us.jcedeno.deltauhc.bukkit.config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import lombok.Data;

@Data
public class GameConfig {

    @IgnoreSetting
    int startPlayers = 8;
    int radius = 2500;
    int minDistance = 250;
    int shrinkStartTime = 60 * 60;
    int shrinkDuration = 60 * 10;
    int radiusFinalSize = 100;
    int pvpTime = 15 * 60;
    int healTime = 30;
    int teamSize = 1;
    @IgnoreSetting
    boolean teamManagement = false;
    @IgnoreSetting
    boolean nether = false;
    @IgnoreSetting
    boolean end = false;
    @IgnoreSetting
    int borderDistanceWarning = 75;

    @IgnoreSetting
    volatile boolean pvp = false;
    @IgnoreSetting
    volatile int currentGameTime = -1;
    @IgnoreSetting
    volatile List<UUID> playersAlive = new ArrayList<>();
    @IgnoreSetting
    volatile int initialPlayers = 0;

    public int increaseGameTime() {
        this.currentGameTime++;

        return this.currentGameTime;
    }

    public void addPlayer(UUID uuid) {
        this.playersAlive.add(uuid);
    }

    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    /**
     * Annotation to tell the /config command to not render a field.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface IgnoreSetting {
    }


}
