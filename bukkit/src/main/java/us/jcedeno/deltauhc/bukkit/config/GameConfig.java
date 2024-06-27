package us.jcedeno.deltauhc.bukkit.config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Data;

@Data
public class GameConfig {

    int startPlayers = 8;
    int radius = 2500;
    int minDistance = 250;
    int shrinkStartTime = 60 * 60;
    int shrinkDuration = 60 * 10;
    int radiusFinalSize = 100;
    int pvpTime = 15 * 60;
    int healTime = 30;

    int teamSize = 1;
    boolean teamManagement = true;

    volatile boolean pvp = false;
    volatile int currentGameTime = -1;
    volatile List<UUID> playersAlive = new ArrayList<>();
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

}
