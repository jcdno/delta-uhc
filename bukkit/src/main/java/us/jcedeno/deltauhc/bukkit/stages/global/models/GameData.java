package us.jcedeno.deltauhc.bukkit.stages.global.models;

import lombok.Builder;
import lombok.Data;
import lombok.Builder.Default;

/**
 * Object that represents data to be read when the plugin starts up to attempt to recover from a lost or restart.
 */
@Data
@Builder
public class GameData {
    @Default
    Boolean recreateWorld = false;    
}
