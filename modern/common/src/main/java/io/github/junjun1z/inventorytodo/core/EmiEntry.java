package io.github.junjun1z.inventorytodo.core;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

/**
 * EMI plugin: drag & drop onto the panel and keeping EMI's panels away from it. Only EMI loads this class (Fabric:
 * "emi" entrypoint, Forge/NeoForge: {@code @EmiEntrypoint} scan), so nothing happens without EMI.
 * <p>
 * Compiled against tiny stubs (common/src/stubs, never in the jar): the method descriptor
 * {@code register(Ldev/emi/emi/api/EmiRegistry;)V} is the same in every EMI version, and the registry is only used
 * through reflection ({@link ReflectViewers#registerEmi}).
 */
@EmiEntrypoint
public class EmiEntry implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        ReflectViewers.registerEmi(registry);
    }
}
