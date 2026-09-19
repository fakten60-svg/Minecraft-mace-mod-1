package de.aerialmace.friend;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class FriendsScreen extends Screen {
    private String input = "";
    private String search = "";
    public FriendsScreen() { super(Text.literal("Friends")); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xD0101014);
        context.drawText(textRenderer, "Friends", 20, 18, 0xFFFFFFFF, true);
        context.drawText(textRenderer, "Type a name, ENTER = add, BACKSPACE edits, F = search", 20, 34, 0xFFAAAAAA, false);
        context.fill(20, 50, 300, 68, 0xFF25252D);
        context.drawText(textRenderer, input.isEmpty() ? "Add friend..." : input, 26, 55, 0xFFFFFFFF, false);
        int y = 84;
        for (Friend friend : FriendManager.getFriends()) {
            if (!search.isBlank() && !friend.name().toLowerCase().contains(search.toLowerCase())) continue;
            context.drawText(textRenderer, friend.name(), 26, y, 0xFFE5E7EB, false);
            context.drawText(textRenderer, "[remove]", 220, y, 0xFFF87171, false);
            y += 16;
        }
        context.drawText(textRenderer, "ESC close", 20, height - 20, 0xFF999999, false);
    }

    @Override public boolean charTyped(CharInput event) {
        if (event.isValidChar()) {
            char chr = (char) event.codepoint();
            if (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-') { if (input.length() < 32) input += chr; return true; }
        }
        return super.charTyped(event);
    }
    @Override public boolean keyPressed(KeyInput key) {
        int code = key.getKeycode();
        if (code == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (code == GLFW.GLFW_KEY_BACKSPACE && !input.isEmpty()) { input = input.substring(0, input.length() - 1); return true; }
        if (code == GLFW.GLFW_KEY_ENTER && !input.isBlank()) { if (FriendManager.add(input)) FriendManager.save(); input = ""; return true; }
        if (code == GLFW.GLFW_KEY_F) { search = input; input = ""; return true; }
        return super.keyPressed(key);
    }
    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && click.y() >= 80) {
            int index = (int)((click.y() - 84) / 16);
            var visible = FriendManager.getFriends().stream().filter(f -> search.isBlank() || f.name().toLowerCase().contains(search.toLowerCase())).toList();
            if (index >= 0 && index < visible.size() && click.x() >= 190) { FriendManager.remove(visible.get(index).name()); FriendManager.save(); return true; }
        }
        return true;
    }
    @Override public boolean shouldPause() { return false; }
}
