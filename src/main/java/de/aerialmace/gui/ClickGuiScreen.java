package de.aerialmace.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.aerialmace.config.ConfigManager;
import de.aerialmace.config.ModConfig;
import de.aerialmace.module.ModuleCategory;
import de.aerialmace.module.ModuleManager;
import de.aerialmace.module.setting.KeybindSetting;
import de.aerialmace.module.setting.Setting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
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

	/** How long the "Reset Everything" confirmation stays armed. */
	private static final long CONFIRM_WINDOW_MS = 5_000L;

	private final List<CategoryPanel> panels = new ArrayList<>();
	private final Animation openAnimation = new Animation(1.0f);
	/** Columns of the default layout for the current GUI scale. */
	private int gridColumns = 4;
	private long lastFrameNanos = System.nanoTime();
	private boolean needsSave;
	private String search = "";
	private long confirmResetUntilMs;

	public ClickGuiScreen() {
		super(Text.literal(ModConfig.CLIENT_NAME + " ClickGUI"));
	}

	@Override
	protected void init() {
		clearChildren();
		panels.clear();

		for (ModuleCategory category : ModuleCategory.values()) {
			panels.add(new CategoryPanel(category, this));
		}
		panels.add(CategoryPanel.clientSettingsPanel(this));

		// Default layout: a grid that always fits the current GUI scale (a fixed four-column
		// grid would push panels off-screen for scale > 1). Saved positions win when present,
		// but every panel is clamped into the reachable area so it can never end up
		// off-screen and undraggable.
		float scale = uiScale();
		int uiWidth = Math.round(width / scale);
		int uiHeight = Math.round(height / scale);
		gridColumns = Math.max(1, Math.min(panels.size(), (uiWidth - 20) / 176));
		for (int index = 0; index < panels.size(); index++) {
			CategoryPanel panel = panels.get(index);
			panel.setMaxVisibleHeight(uiHeight - 8);
			if (panel.hasSavedPosition()) {
				panel.applySavedPosition();
			} else {
				int column = index % gridColumns;
				int row = index / gridColumns;
				panel.setPosition(20 + column * 176, 34 + row * 292);
			}
			panel.clampPosition(uiWidth - 60, uiHeight - 30);
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

		var clientSettings = de.aerialmace.module.modules.ClientSettingsModule.get();
		boolean showSearch = clientSettings == null || clientSettings.getState().showSearch;
		for (CategoryPanel panel : panels) panel.setFilter(search);

		// Dark translucent background overlay.
		int overlayAlpha = (int) (0xB0 * eased);
		if (overlayAlpha > 4) {
			context.fill(0, 0, width, height, ThemeManager.withAlpha(0x000000, overlayAlpha));
		}

		// GUI scale (client setting): anchored at the top-left corner. Centering the scaled
		// layer would push panels off-screen for scale > 1 and make them unreachable.
		float scale = uiScale();
		// The window can be resized while the GUI is open, so the height budget is refreshed
		// every frame (it decides how much of a panel is shown before it scrolls).
		int uiHeightBudget = Math.round(height / scale) - 8;
		for (CategoryPanel panel : panels) {
			panel.setMaxVisibleHeight(uiHeightBudget);
		}
		var matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.scale(scale, scale);

		double uiMouseX = mouseX / scale;
		double uiMouseY = mouseY / scale;

		// Search bar lives inside the scaled layer so it follows the GUI scale too.
		if (showSearch) {
			context.fill(12, 8, 220, 24, ThemeManager.get().surface());
			context.drawText(MinecraftClient.getInstance().textRenderer,
					search.isEmpty() ? "Search modules..." : search, 18, 13,
					search.isEmpty() ? ThemeManager.get().secondaryText().getColor() : ThemeManager.get().text().getColor(), false);
		}

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

	/** Converts screen coords into scaled UI coords (top-left anchored). */
	private double[] toUi(double screenX, double screenY) {
		float scale = uiScale();
		return new double[] { screenX / scale, screenY / scale };
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

		// Topmost panel first, and the panel that handles the click is raised. Without this a
		// panel's expanded settings could be drawn behind a later panel while still receiving
		// clicks (invisible but interactive).
		for (int index = panels.size() - 1; index >= 0; index--) {
			CategoryPanel panel = panels.get(index);
			if (panel.mouseClicked(click, mouseX, mouseY)) {
				bringToFront(panel);
				return true;
			}
		}
		return super.mouseClicked(click, doubled);
	}

	/** Moves a panel to the front of both the draw and the input order. */
	private void bringToFront(CategoryPanel panel) {
		if (panels.isEmpty() || panels.get(panels.size() - 1) == panel) {
			return;
		}
		panels.remove(panel);
		panels.add(panel);
	}

	@Override
	public boolean mouseReleased(Click click) {
		for (CategoryPanel panel : panels) {
			panel.mouseReleased(click);
		}
		// A drag must not be able to park a panel outside the visible area.
		float scale = uiScale();
		int maxX = Math.round(width / scale) - 60;
		int maxY = Math.round(height / scale) - 30;
		for (CategoryPanel panel : panels) {
			panel.clampPosition(maxX, maxY);
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseDragged(Click click, double deltaX, double deltaY) {
		double[] ui = toUi(click.x(), click.y());
		// Sliders/ranges/pickers read click.x()/click.y(), so they must receive UI-space
		// coordinates as well - otherwise a GUI scale other than 1 would write wrong values.
		Click uiClick = new Click(ui[0], ui[1], click.buttonInfo());
		for (int index = panels.size() - 1; index >= 0; index--) {
			if (panels.get(index).mouseDragged(uiClick, ui[0], ui[1], deltaX, deltaY)) {
				return true;
			}
		}
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		double[] ui = toUi(mouseX, mouseY);
		for (int index = panels.size() - 1; index >= 0; index--) {
			CategoryPanel panel = panels.get(index);
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

		// Keybind recording owns the keyboard first: while "Press a key..." is active no
		// other handler (search, module toggles) may react to the same press.
		for (CategoryPanel panel : panels) {
			if (panel.keyPressed(keyCode)) {
				return true;
			}
		}

		if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
			search = search.substring(0, search.length() - 1);
			return true;
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

	/** True while any panel captures a keybind, so typed text must not reach the search. */
	private boolean isRecording() {
		for (CategoryPanel panel : panels) {
			if (panel.isRecording()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (isRecording()) {
			return true;
		}
		if (input.isValidChar()) {
			char chr = (char) input.codepoint();
			if (Character.isLetterOrDigit(chr) || chr == '_' || chr == ' ') {
				if (search.length() < 32) search += chr;
				return true;
			}
		}
		return super.charTyped(input);
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
		// Reset the real settings, not just the cached state: the GUI Keybind setting falls
		// back to RIGHT_SHIFT and every module keybind back to NONE.
		for (var module : ModuleManager.getModules()) {
			for (Setting setting : module.getSettings()) {
				if (setting instanceof KeybindSetting) {
					setting.reset();
				}
			}
			module.getKeybind().reset();
		}
		var clientSettings = de.aerialmace.module.modules.ClientSettingsModule.get();
		if (clientSettings != null) {
			clientSettings.getState().guiKey = de.aerialmace.module.modules.ClientSettingsModule.DEFAULT_GUI_KEY;
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
		ThemeManager.get().border().reset();
		ThemeManager.get().hover().reset();
		ThemeManager.get().setShowBorders(true);
	}

	@Override
	public void resetLayout() {
		for (int index = 0; index < panels.size(); index++) {
			int column = index % gridColumns;
			int row = index / gridColumns;
			panels.get(index).setPosition(20 + column * 176, 34 + row * 292);
		}
		// "Reset GUI Layout" covers the HUD positions as well.
		de.aerialmace.hud.HudManager.resetLayout();
		markDirty();
	}

	/**
	 * Destructive reset. The first click only arms the action (the button label switches to a
	 * confirmation prompt); the second click within {@link #CONFIRM_WINDOW_MS} performs it.
	 * The friend list is user data and is deliberately left untouched.
	 */
	@Override
	public void resetEverything() {
		long now = System.currentTimeMillis();
		if (now > confirmResetUntilMs) {
			confirmResetUntilMs = now + CONFIRM_WINDOW_MS;
			playClick();
			markDirty();
			return;
		}
		confirmResetUntilMs = 0L;
		resetModuleSettings();
		resetKeybinds();
		resetTheme();
		resetLayout();
		de.aerialmace.hud.HudManager.resetAll();
		markDirty();
	}

	@Override
	public boolean isConfirmingReset() {
		if (confirmResetUntilMs == 0L) {
			return false;
		}
		if (System.currentTimeMillis() > confirmResetUntilMs) {
			confirmResetUntilMs = 0L;
			return false;
		}
		return true;
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
