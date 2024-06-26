package us.jcedeno.deltauhc.bukkit.scenarios.commands;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

import java.util.List;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.processing.CommandContainer;
import fr.mrmicky.fastinv.FastInv;
import us.jcedeno.deltauhc.bukkit.DeltaUHC;
import us.jcedeno.deltauhc.bukkit.scenarios.models.BaseScenario;

/**
 * A class holding all the user-facing Scenario commands.
 * 
 * @author thejcedeno.
 */
@CommandContainer
@CommandDescription("scenario view commands")
public final class ScenarioCommands {

    @CommandMethod("scenario")
    public void enabledScenarios(final @NonNull CommandSender sender) {
        // Sender error message, in red, saying that there are no scenarios enabled.
        List<BaseScenario> enabledScenarios = DeltaUHC.getGame().getScenarioManager().enabledScenarios();

        if (enabledScenarios.isEmpty()) {
            sender.sendMessage(miniMessage().deserialize("<red>There are no scenarios enabled."));
            return;
        }

        if (sender instanceof Player player) {
            var inv = new FastInv(InventoryType.HOPPER);
            new FastInv(owner -> Bukkit.createInventory(owner, 27, miniMessage()
                    .deserialize(String.format("<bold><green>Enabled Scenarios (%s): ", enabledScenarios.size()))));

            enabledScenarios.forEach(scenario -> inv.addItem(getScenarioItem(scenario), e -> {
                e.getWhoClicked().sendMessage("You clicked on the scenario " + scenario.name());
            }));

            inv.open(player);
            return;
        }

        sender.sendMessage(miniMessage().deserialize("<green>Enabled Scenarios:"));
        DeltaUHC.getGame().getScenarioManager().enabledScenarios()
                .forEach(scenario -> sender.sendMessage("- " + scenario.name()));
    }

    public static ItemStack getScenarioItem(BaseScenario scenario) {
        var item = new ItemStack(scenario.material());
        var meta = item.getItemMeta();

        meta.displayName(miniMessage().deserialize(scenario.name()));
        meta.lore(Stream.of(scenario.description().split("\n")).map(m -> miniMessage().deserialize(m)).toList());
        item.setItemMeta(meta);

        return item;
    }

    // REQUIRED BY CLOUD FRAMEWORK
    public ScenarioCommands(final AnnotationParser<CommandSender> annotationParser) {
    }

}
