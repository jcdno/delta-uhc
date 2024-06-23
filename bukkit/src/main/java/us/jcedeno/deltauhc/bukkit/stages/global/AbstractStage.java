package us.jcedeno.deltauhc.bukkit.stages.global;

import us.jcedeno.deltauhc.bukkit.common.GameStage;

public abstract class AbstractStage implements GameStage {
    protected boolean register = false;

    @Override
    public boolean registered() {
        return register;
    }

    @Override
    public void registerTasks() {
        if (register) {
            throw new RuntimeException("Cannot register an already registered stage.");
        }
        this.register = true;
    }

    @Override
    public void unregisterTasks() {
        if (!register) {
            throw new RuntimeException("Cannot unregister a non registered stage.");
        }
        this.register = false;
    }

}
