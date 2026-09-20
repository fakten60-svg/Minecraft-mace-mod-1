package de.aerialmace.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Inventory helpers that stick to the normal client interaction path:
 * everything is expressed as screen-handler clicks or the regular slot-selection
 * packet a scroll wheel click would send — no direct server-side state edits.
 */
public final class InventoryUtils {

	/**
	 * Slot ids inside {@link PlayerScreenHandler}, verified against the 1.21.11 layout:
	 * 0 = crafting result, 1-4 = crafting input, 5-8 = armor (head, chest, legs, feet),
	 * 9-35 = main inventory, 36-44 = hotbar, 45 = offhand.
	 */
	private static final int CHEST_SLOT_ID = 6;
	/** First hotbar slot index inside {@link PlayerScreenHandler} (hotbar = 36..44). */
	private static final int HOTBAR_OFFSET = 36;

	private InventoryUtils() {
	}

	/**
	 * True when the stack holds an equippable that targets the chest slot
	 * (chestplate, elytra, ...).
	 */
	public static boolean isChestplate(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		EquippableComponent equippable = stack.getComponents().get(DataComponentTypes.EQUIPPABLE);
		return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
	}

	/**
	 * Equips the item currently held in the main hand as chestplate using vanilla
	 * inventory clicks (the same packets a player clicking in the inventory screen
	 * produces).
	 *
	 * <p>Flow: pick up the held chestplate, put it into the chest slot; if that slot
	 * was occupied, the previous chestplate is returned to the emptied hand slot —
	 * exactly like manual swapping.
	 *
	 * @return {@code true} when the equip action was triggered, {@code false} when the
	 *         held item is not a chestplate or interaction is impossible.
	 */
	public static boolean equipHeldItemAsChestplate(MinecraftClient client, ClientPlayerEntity player) {
		ClientPlayerInteractionManager interactionManager = client.interactionManager;
		if (interactionManager == null || player == null) {
			return false;
		}

		ItemStack held = player.getMainHandStack();
		if (!isChestplate(held)) {
			return false;
		}

		PlayerScreenHandler handler = player.playerScreenHandler;
		int syncId = handler.syncId;
		int handSlot = HOTBAR_OFFSET + player.getInventory().getSelectedSlot();

		// 1. Pick the held chestplate up onto the cursor.
		interactionManager.clickSlot(syncId, handSlot, 0, SlotActionType.PICKUP, player);
		// 2. Click the chest slot: vanilla swaps it with the cursor stack, returning the
		//    previously equipped chestplate to the cursor.
		interactionManager.clickSlot(syncId, CHEST_SLOT_ID, 0, SlotActionType.PICKUP, player);
		// 3. If the old chestplate now sits on the cursor, park it in the emptied hand slot.
		if (!player.currentScreenHandler.getCursorStack().isEmpty()) {
			interactionManager.clickSlot(syncId, handSlot, 0, SlotActionType.PICKUP, player);
		}
		return true;
	}

	/**
	 * Scans the hotbar for a mace. Only the nine hotbar slots are searched (the sequence
	 * switches the selected slot, which is what the player would do by hand).
	 *
	 * @return the hotbar slot (0-8) holding a mace, or {@code -1} if none exists.
	 */
	public static int findMaceSlot(ClientPlayerEntity player) {
		for (int slot = 0; slot < 9; slot++) {
			ItemStack stack = player.getInventory().getStack(slot);
			if (!stack.isEmpty() && stack.getItem() instanceof MaceItem) {
				return slot;
			}
		}
		return -1;
	}

	/**
	 * Switches to the given hotbar slot exactly like a scroll-wheel selection: the
	 * client's selected slot is updated and the regular selection packet is sent.
	 */
	public static void switchHotbarSlot(MinecraftClient client, ClientPlayerEntity player, int slot) {
		if (slot < 0 || slot > 8 || player == null) {
			return;
		}
		if (player.getInventory().getSelectedSlot() == slot) {
			return;
		}
		player.getInventory().setSelectedSlot(slot);
		player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
	}
}
