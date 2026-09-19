package de.aerialmace.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.aerialmace.config.ConfigManager;
import de.aerialmace.module.ModuleCategory;
import de.aerialmace.module.ModuleManager;
import de.aerialmace.module.setting.KeybindSetting;
import de.aerialmace.module.setting.Setting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import org.lwjgl.glfw.GLFW;

/**
 * The ClickGUI. Builds one {@link CategoryPanel} per {@link ModuleCategory} from the
 * {@link ModuleManager} — nothing hardcoded per module — plus the Client Settings panel
 * with theme color pickers and the reset actions.
 *
 * <p>Right Shift (configurable) opens/closes this screen; ESC closes as well. Panel
 * positions and every change are persisted via {@link ConfigManager}.
 */
public class ClickGuiScreen extends Screen implements PanelCallbacks {

	private final List<CategoryPanel> panels = new ArrayList<>();
	private final Animation openAnimation = new Animation(1.0f);
	private long lastFrameNanos = System.nanoTime();
	private boolean needsSave;

	public ClickGuiScreen() {
		super(Text.literal("AerialMace ClickGUI"));
	}

	@Override
	protected void init() {
		clearChildren();
		panels.clear();

		for (ModuleCategory category : ModuleCategory.values()) {
			panels.add(new CategoryPanel(category, this));
		}
		panels.add(CategoryPanel.clientSettingsPanel(this));

		// Default layout: staggered column; saved positions are applied when present.
		int x = 20;
		int y = 20;
		for (CategoryPanel panel : panels) {
			if (panel.hasSavedPosition()) {
				panel.applySavedPosition();
			} else {
				panel.setPosition(x, y);
				y += 34;
			}
		}

		openAnimation.open();
		lastFrameNanos = System.nanoTime();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// FPS-independent timing.
		long now = System.nanoTime();
		float deltaSeconds = Math.min(0.1f, (now - lastFrameNanos) / 1_000_000_000f);
		lastFrameNanos = now;

		openAnimation.update(deltaSeconds);
		float eased = openAnimation.eased();

		// Dark translucent background overlay.
		int overlayAlpha = (int) (0xB0 * eased);
		if (overlayAlpha > 4) {
			context.fill(0, 0, width, height, ThemeManager.withAlpha(0x000000, overlayAlpha));
		}

		// GUI scale (client setting): scale the panel layer around the screen center.
		float scale = uiScale();
		double offX = (width - width * scale) / 2.0;
		double offY = (height - height * scale) / 2.0;
		var matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.scale(scale, scale);
		matrices.translate((float) (offX / scale), (float) (offY / scale));

		double uiMouseX = (mouseX - offX) / scale;
		double uiMouseY = (mouseY - offY) / scale;

		for (CategoryPanel panel : panels) {
			panel.update(deltaSeconds);
			panel.render(context, (int) uiMouseX, (int) uiMouseY, deltaSeconds, eased);
		}
		matrices.popMatrix();

		if (needsSave) {
			needsSave = false;
			ConfigManager.save(collectPanelPositions());
		}
	}

	/** Converts screen coords into scaled UI coords. */
	private double[] toUi(double screenX, double screenY) {
		float scale = uiScale();
		double offX = (width - width * scale) / 2.0;
		double offY = (height - height * scale) / 2.0;
		return new double[] { (screenX - offX) / scale, (screenY - offY) / scale };
	}

	private float uiScale() {
		var settings = de.aerialmace.module.modules.ClientSettingsModule.get();
		return settings != null ? (float) settings.getState().guiScale : 1.0f;
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		double[] ui = toUi(click.x(), click.y());
		double mouseX = ui[0];
		double mouseY = ui[1];

		// Stop keybind recording when clicking outside its row.
		for (CategoryPanel panel : panels) {
			panel.stopRecordingIfOutside(mouseX, mouseY);
		}

		for (CategoryPanel panel : panels) {
			if (panel.mouseClicked(click, mouseX, mouseY)) {
				return true;
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseReleased(Click click) {
		for (CategoryPanel panel : panels) {
			panel.mouseReleased(click);
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseDragged(Click click, double deltaX, double deltaY) {
		double[] ui = toUi(click.x(), click.y());
		for (CategoryPanel panel : panels) {
			if (panel.mouseDragged(click, ui[0], ui[1], deltaX, deltaY)) {
				return true;
			}
		}
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		double[] ui = toUi(mouseX, mouseY);
		for (CategoryPanel panel : panels) {
			if (panel.isMouseOverPanel(ui[0], ui[1])) {
				panel.scroll(vertical);
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		int keyCode = input.getKeycode();

		// Keybind recording first.
		for (CategoryPanel panel : panels) {
			if (panel.keyPressed(keyCode)) {
				return true;
			}
		}

		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			close();
			return true;
		}
		if (keyCode == guiKey()) {
			close();
			return true;
		}
		return super.keyPressed(input);
	}

	private int guiKey() {
		var module = de.aerialmace.module.modules.ClientSettingsModule.get();
		return module != null ? module.getState().guiKey : 344;
	}

	@Override
	public void close() {
		for (CategoryPanel panel : panels) {
			panel.savePosition();
		}
		ConfigManager.save(collectPanelPositions());
		super.close();
	}

	@Override
	public void removed() {
		ConfigManager.save(collectPanelPositions());
		super.removed();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private Map<String, double[]> collectPanelPositions() {
		Map<String, double[]> positions = ConfigManager.newPanelPositionMap();
		for (CategoryPanel panel : panels) {
			positions.put(panel.getStorageKey(), new double[] { panel.getX(), panel.getY() });
		}
		return positions;
	}

	// ------------------------------------------------------------------
	// PanelCallbacks
	// ------------------------------------------------------------------

	@Override
	public void markDirty() {
		needsSave = true;
	}

	@Override
	public void playClick() {
		var settings = de.aerialmace.module.modules.ClientSettingsModule.get();
		if (settings != null && !settings.getState().clickSounds) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null) {
			client.getSoundManager().play(
					net.minecraft.client.sound.PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0f));
		}
	}

	@Override
	public void resetModuleSettings() {
		for (var module : ModuleManager.getModules()) {
			for (Setting setting : module.getSettings()) {
				if (setting instanceof KeybindSetting) {
					continue;
				}
				setting.reset();
			}
		}
	}

	@Override
	public void resetKeybinds() {
		// GUI keybind -> RIGHT_SHIFT, all module keybinds -> NONE.
		var clientSettings = de.aerialmace.module.modules.ClientSettingsModule.get();
		if (clientSettings != null) {
			clientSettings.getState().guiKey = 344;
		}
		for (var module : ModuleManager.getModules()) {
			module.getKeybind().reset();
		}
	}

	@Override
	public void resetTheme() {
		ThemeManager.get().accent().reset();
		ThemeManager.get().background().reset();
		ThemeManager.get().panel().reset();
		ThemeManager.get().moduleActive().reset();
		ThemeManager.get().text().reset();
		ThemeManager.get().secondaryText().reset();
	}

	@Override
	public void resetLayout() {
		int x = 20;
		int y = 20;
		for (CategoryPanel panel : panels) {
			panel.setPosition(x, y);
			y += 34;
		}
	}

	/** Default accent tint per category. */
	static int keyDefaultCategoryColor(ModuleCategory category) {
		return switch (category) {
			case COMBAT -> 0xFFF87171;
			case VISUALS -> 0xFF60A5FA;
			case MOVEMENT -> 0xFF34D399;
			case MISC -> 0xFFFBBF24;
		};
	}
}
