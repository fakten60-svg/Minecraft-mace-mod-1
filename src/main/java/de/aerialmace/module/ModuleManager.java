package de.aerialmace.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Holds every module. The ClickGUI renders whatever is registered here — panels are
 * generated per {@link ModuleCategory}, never hardcoded per module.
 */
public final class ModuleManager {

	private static final List<Module> MODULES = new ArrayList<>();
	private static final Map<ModuleCategory, List<Module>> BY_CATEGORY = new EnumMap<>(ModuleCategory.class);

	private ModuleManager() {
	}

	public static void register(Module module) {
		module.finishRegistration();
		MODULES.add(module);
		BY_CATEGORY.computeIfAbsent(module.getCategory(), c -> new ArrayList<>()).add(module);
	}

	public static List<Module> getModules() {
		return Collections.unmodifiableList(MODULES);
	}

	public static List<Module> getByCategory(ModuleCategory category) {
		return Collections.unmodifiableList(BY_CATEGORY.getOrDefault(category, List.of()));
	}

	public static boolean hasKeybindConflict(Module source) {
		int key = source.getKeybind().getKey();
		if (source.getKeybind().isNone()) return false;
		for (Module module : MODULES) {
			if (module != source && module.getKeybind().getKey() == key && !module.getKeybind().isNone()) return true;
		}
		return false;
	}

	public static Module getByName(String name) {
		for (Module module : MODULES) {
			if (module.getName().equalsIgnoreCase(name)) {
				return module;
			}
		}
		return null;
	}
}
