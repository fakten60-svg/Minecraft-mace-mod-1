package de.aerialmace.config;

import de.aerialmace.notification.NotificationManager;
import de.aerialmace.notification.NotificationType;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class ConfigProfilesScreen extends Screen {
    private String input = "";
    private String selected = "default";
    public ConfigProfilesScreen() { super(Text.literal("Profiles")); }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xD0101014);
        context.drawText(textRenderer, "Config Profiles", 20, 18, 0xFFFFFFFF, true);
        context.drawText(textRenderer, "N + name = create/save | ENTER = load | S = save | R + name = rename | D = delete | ESC", 20, 34, 0xFFAAAAAA, false);
        context.drawText(textRenderer, "Input: " + input, 20, 52, 0xFF60A5FA, false);
        int y = 78;
        for (String profile : ConfigManager.listProfiles()) {
            int color = profile.equals(selected) ? 0xFF60A5FA : 0xFFE5E7EB;
            context.drawText(textRenderer, profile, 28, y, color, false); y += 16;
        }
    }
    @Override public boolean charTyped(CharInput event) {
        if (event.isValidChar()) { char c = (char) event.codepoint(); if (Character.isLetterOrDigit(c) || c == '_' || c == '-') { if (input.length() < 32) input += c; return true; } }
        return super.charTyped(event);
    }
    @Override public boolean keyPressed(KeyInput event) {
        int key = event.getKeycode();
        if (key == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !input.isEmpty()) { input = input.substring(0, input.length() - 1); return true; }
        if (key == GLFW.GLFW_KEY_N && !input.isBlank()) {
            selected = input;
            if (ConfigManager.saveProfile(selected)) {
                NotificationManager.notify(NotificationType.SUCCESS, "Profile saved", selected);
            }
            input = ""; return true;
        }
        if (key == GLFW.GLFW_KEY_S) {
            if (ConfigManager.saveProfile(selected)) {
                NotificationManager.notify(NotificationType.SUCCESS, "Profile saved", selected);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER) {
            if (ConfigManager.loadProfile(selected)) {
                NotificationManager.notify(NotificationType.SUCCESS, "Profile loaded", selected);
            } else {
                NotificationManager.notify(NotificationType.ERROR, "Profile not found", selected);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_R && !input.isBlank()) {
            if (ConfigManager.renameProfile(selected, input)) {
                NotificationManager.notify(NotificationType.SUCCESS, "Profile renamed", selected + " -> " + input);
                selected = input;
            } else {
                NotificationManager.notify(NotificationType.ERROR, "Rename failed", selected);
            }
            input = ""; return true;
        }
        if (key == GLFW.GLFW_KEY_D) {
            if (ConfigManager.deleteProfile(selected)) {
                NotificationManager.notify(NotificationType.INFO, "Profile deleted", selected);
            }
            selected = "default"; return true;
        }
        return super.keyPressed(event);
    }
    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && click.y() >= 75) {
            int index = (int)((click.y() - 78) / 16); var profiles = ConfigManager.listProfiles();
            if (index >= 0 && index < profiles.size()) selected = profiles.get(index);
        }
        return true;
    }
    @Override public boolean shouldPause() { return false; }
}
