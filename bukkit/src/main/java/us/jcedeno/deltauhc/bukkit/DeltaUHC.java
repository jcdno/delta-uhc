package us.jcedeno.deltauhc.bukkit;

import java.util.function.Function;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.arguments.parser.StandardParameters;
import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import lombok.Getter;
import us.jcedeno.deltauhc.bukkit.config.GameConfig;
import us.jcedeno.deltauhc.bukkit.stages.lobby.LobbyStage;

/**
 * Entry point of Delta UHC, 1.21
 * 
 * @author jcedeno
 */
public class DeltaUHC extends JavaPlugin {
    private final LobbyStage lobbyStage = new LobbyStage();
    @Getter
    private final GameConfig gameConfig = new GameConfig();
    // Command manager
    private BukkitCommandManager<CommandSender> bukkitCommandManager;
    private AnnotationParser<CommandSender> annotationParser;

    @Getter
    private static DeltaUHC game;

    public static GameConfig gameConfig() {
        return DeltaUHC.getGame().getGameConfig();
    }

    @Override
    public void onEnable() {
        game = this;
        /*
         * TODO: Do not register the stages right away, check something to see if there
         * was a game already going before (a possible restart).
         */

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
