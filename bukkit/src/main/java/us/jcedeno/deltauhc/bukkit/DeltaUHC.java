package us.jcedeno.deltauhc.bukkit;

import java.util.concurrent.Callable;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.World.Environment;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.arguments.parser.StandardParameters;
import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import fr.mrmicky.fastboard.adventure.FastBoard;
import fr.mrmicky.fastinv.FastInvManager;
import lombok.Getter;
import us.jcedeno.deltauhc.bukkit.config.GameConfig;
import us.jcedeno.deltauhc.bukkit.scenarios.ScenarioManager;
import us.jcedeno.deltauhc.bukkit.stages.global.GlobalStage;
import us.jcedeno.deltauhc.bukkit.stages.ingame.InGameStage;
import us.jcedeno.deltauhc.bukkit.stages.lobby.LobbyStage;
import us.jcedeno.deltauhc.bukkit.stages.starting.StartingStage;
import us.jcedeno.deltauhc.bukkit.team.TeamManager;

/**
 * Entry point of Delta UHC, 1.21
 * 
 * @author jcedeno
 */
public class DeltaUHC extends JavaPlugin {
    @Getter
    private final LobbyStage lobbyStage = new LobbyStage();
    @Getter
    private final InGameStage inGameStage = new InGameStage();
    @Getter
    private final GlobalStage globalStage = new GlobalStage();
    @Getter
    private final StartingStage startingStage = new StartingStage();
    @Getter
    private final GameConfig gameConfig = new GameConfig();
    @Getter
    private final TeamManager teamManager = new TeamManager();
    @Getter
    private final ScenarioManager scenarioManager = new ScenarioManager();
    // Command manager
    private BukkitCommandManager<CommandSender> bukkitCommandManager;
    private AnnotationParser<CommandSender> annotationParser;

    @Getter
    private static DeltaUHC game;

    public static GameConfig gameConfig() {
        return DeltaUHC.getGame().getGameConfig();
    }

    public static void runSync(Runnable run){
        Bukkit.getScheduler().runTask(game, run);
    }

    public FastBoard getBoard(Player player){
        return globalStage.getBoards().get(player.getUniqueId());
    }

    @Override
    public void onEnable() {
        game = this;
        FastInvManager.register(this);

        // Create lobby world
        WorldCreator wc = new WorldCreator("lobby");
        wc.environment(Environment.NORMAL);
        wc.type(WorldType.FLAT);
        wc.generator("VoidGen");
        var world = wc.createWorld();
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setHardcore(true);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        world.setPVP(false);
        world.getEntities().forEach(Entity::remove);
        world.setTime(0);

        /*
         * TODO: Do not register the stages right away, check something to see if there
         * was a game already going before (a possible restart).
         */

        globalStage.registerTasks();
        lobbyStage.registerTasks();

        /** Register brigadier. */
        try {
            this.bukkitCommandManager = new BukkitCommandManager<>(this,
                    CommandExecutionCoordinator.simpleCoordinator(),
                    Function.identity(),
                    Function.identity());
            
            // TODO: Cannot register brigadier until supported by incendo
            // this.bukkitCommandManager.registerBrigadier();
        } catch (Exception e) {
            e.printStackTrace();
        }
        /** Reigster annotation parser. */
        final Function<ParserParameters, CommandMeta> commandMetaFunction = p -> CommandMeta.simple()
                // This will allow you to decorate commands with descriptions
                .with(CommandMeta.DESCRIPTION, p.get(StandardParameters.DESCRIPTION, "No description"))
                .build();

        this.annotationParser = new AnnotationParser<>(this.bukkitCommandManager, CommandSender.class,
                commandMetaFunction);

        this.constructCommands();



    }

    @Override
    public void onDisable() {

    }

    /**
     * A method that constructs all the cloud framework commands.
     */
    private void constructCommands() {
        // Parse all @CommandMethod-annotated methods
        this.annotationParser.parse(this);
        // Parse all @CommandContainer-annotated classes
        try {
            this.annotationParser.parseContainers();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}
