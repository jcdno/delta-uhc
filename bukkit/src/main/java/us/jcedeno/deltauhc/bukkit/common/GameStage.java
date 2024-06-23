package us.jcedeno.deltauhc.bukkit.common;

public interface GameStage {
    void registerTasks();

    void unregisterTasks();

    boolean registered();
}
