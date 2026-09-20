package de.aerialmace.event;

import de.aerialmace.module.Module;

/**
 * Fired after a module's enabled state changed. Listeners may read the module but must
 * not toggle other modules from inside the handler (that would re-enter the bus).
 */
public record ModuleToggleEvent(Module module, boolean enabled) {
}
