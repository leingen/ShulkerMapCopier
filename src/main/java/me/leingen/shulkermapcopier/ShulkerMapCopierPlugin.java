package me.leingen.shulkermapcopier;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.ShulkerBox;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Arrays;
import java.util.Set;

public class ShulkerMapCopierPlugin extends JavaPlugin {
    // Number of max copies per command
    private static final int MAX_COPIES = 10;

    // Cache shulker box materials for performance
    private static final Set<Material> SHULKER_BOX_MATERIALS = EnumSet.of(
            Material.SHULKER_BOX,
            Material.WHITE_SHULKER_BOX,
            Material.ORANGE_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX,
            Material.LIGHT_BLUE_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX,
            Material.LIME_SHULKER_BOX,
            Material.PINK_SHULKER_BOX,
            Material.GRAY_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX,
            Material.CYAN_SHULKER_BOX,
            Material.PURPLE_SHULKER_BOX,
            Material.BLUE_SHULKER_BOX,
            Material.BROWN_SHULKER_BOX,
            Material.GREEN_SHULKER_BOX,
            Material.RED_SHULKER_BOX,
            Material.BLACK_SHULKER_BOX
    );

    @Override
    public void onEnable() {
        getLogger().info("ShulkerMapCopier plugin enabled!");
        this.getCommand("copyShulkerMap").setExecutor(this);
        this.getCommand("copyShulkerMap").setTabCompleter(new CopyShulkerTab());
    }

    @Override
    public void onDisable() {
        getLogger().info("ShulkerMapCopier plugin disabled!");
    }

    public static class CopyShulkerTab implements TabCompleter {
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) {
                return Arrays.asList("help", "NumberOfCopies");
            }
            return Collections.emptyList();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("copyShulkerMap")) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        return executeCommand(player, args);
    }

    private boolean executeCommand(Player player, String[] args) {
        // Check for help argument first
        if (args.length > 0 && (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?"))) {
            sendHelpMessage(player);
            return true;
        }

        // CRITICAL: Store the shulker to clone at the very beginning to prevent item switching
        ItemStack shulkToCopy = player.getInventory().getItemInMainHand();

        // Parse the number of copies from command arguments
        int copies = 1; // Default value
        if (args.length > 0) {
            try {
                copies = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid number format! Please enter a valid number between 1 and " + MAX_COPIES + ".", NamedTextColor.RED));
                player.sendMessage(Component.text("Use '/copyShulkerMap help' for usage information.", NamedTextColor.YELLOW));
                return true;
            }
        }

        // Ensure the number of copies is between 1 and MAX_COPIES included
        if (copies < 1 || copies > MAX_COPIES) {
            player.sendMessage(Component.text("Number of copies must be between 1 and " + MAX_COPIES + "!", NamedTextColor.RED));
            player.sendMessage(Component.text("Use '/copyShulkerMap help' for usage information.", NamedTextColor.YELLOW));
            return true; // Invalid number of copies
        }
        
        // Validate and process the original shulker immediately
        int mapsToCopy = validateAndProcessShulker(shulkToCopy, player);
        if (mapsToCopy <= 0) {
            return true; // validateAndProcessShulker failed, end.
        }

        // Calculate total maps needed (maps per shulker * number of copies)
        int totalMapsNeeded = mapsToCopy * copies;

        // Check if player has required items
        PlayerInventory inventory = player.getInventory();
        if (!checkRequiredResources(inventory, totalMapsNeeded, copies, player)) {
            return true; // checkRequiredResources failed, end.
        } 

        // All conditions met, consume items and give clone if success
        if (consumeRequiredItems(inventory, totalMapsNeeded, copies)) {
            // Give the ORIGINAL shulker clones (not current main hand item)
            for (int i = 0; i < copies; i++) {
                inventory.addItem(shulkToCopy.clone());
            }
            
            if (copies == 1) {
                player.sendMessage(Component.text("Successfully copied shulker box with " + mapsToCopy + " maps!", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Successfully copied " + copies + " shulker boxes, each with " + mapsToCopy + " maps!", NamedTextColor.GREEN));
            }
            return true;
        } else {
            player.sendMessage(Component.text("Failed to consume required items!", NamedTextColor.RED));
            return true;
        }
    }

    /**
     * Validates and processes a shulker box, returning its data or null if invalid
     * Return number of filled maps in shulker.
     * Returns 0 in case of invalid shulker.
     */
    private int validateAndProcessShulker(ItemStack item, Player player) {
        // Check if item is a shulker box
        if (!isShulkerBox(item)) {
            player.sendMessage(Component.text("You must hold a shulker box in your main hand!", NamedTextColor.RED));
            return 0;
        }

        // Get the shulker box contents
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta == null) {
            player.sendMessage(Component.text("The shulker box appears to be corrupted!", NamedTextColor.RED));
            return 0;
        }

        ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
        ItemStack[] contents = shulkerBox.getInventory().getContents();

        if (contents == null) {
            player.sendMessage(Component.text("The shulker box appears to be empty!", NamedTextColor.RED));
            return 0;
        }

        // Count filled maps and validate contents in single pass
        int mapCount = 0;
        for (ItemStack stack : contents) {
            if (stack != null && !stack.getType().isAir()) {
                if (stack.getType() == Material.FILLED_MAP) {
                    mapCount += stack.getAmount();
                } else {
                    player.sendMessage(Component.text("The shulker box contains non-map items! Only filled maps are allowed.", NamedTextColor.RED));
                    return 0;
                }
            }
        }

        // Ensure at least one filled map was found
        if (mapCount == 0) {
            player.sendMessage(Component.text("The shulker box contains no filled maps!", NamedTextColor.RED));
            return 0;
        }

        // Everything ok
        return mapCount;
    }

    /**
     * Checks if player has all required resources
     * Returns true if the check was successful
     */
    private boolean checkRequiredResources(PlayerInventory inventory, int mapsNeeded, int shulkersNeeded, Player player) {
        ItemStack[] contents = inventory.getContents();
        if (contents == null) {
            player.sendMessage(Component.text("Inventory is corrupted!", NamedTextColor.RED));
            return false;
        }

        int emptyShulkerCount = 0;
        int emptyMapCount = 0;

        // Single pass through inventory to count required items
        for (ItemStack stack : contents) {
            if (stack != null && !stack.getType().isAir()) {
                if (stack.getType() == Material.MAP) {
                    emptyMapCount += stack.getAmount();
                } else if (isEmptyShulkerBox(stack)) {
                    emptyShulkerCount += stack.getAmount();
                }
            }
        }

        // Validate requirements
        if (emptyShulkerCount < shulkersNeeded) {
            player.sendMessage(Component.text("You need at least " + shulkersNeeded + " empty shulker" + (shulkersNeeded > 1 ? "s" : "") + " in your inventory! You have " + emptyShulkerCount + ".", NamedTextColor.RED));
            return false;
        }

        if (emptyMapCount < mapsNeeded) {
            player.sendMessage(Component.text("You need at least " + mapsNeeded + " empty maps in your inventory! You have " + emptyMapCount + ".", NamedTextColor.RED));
            return false;
        }

        // All conditions met
        return true;
    }

    /**
     * Fast shulker box type check using EnumSet
     */
    private boolean isShulkerBox(ItemStack stack) {
        return stack != null && !stack.getType().isAir() && SHULKER_BOX_MATERIALS.contains(stack.getType());
    }

    /**
     * Checks if a shulker box is empty
     */
    private boolean isEmptyShulkerBox(ItemStack stack) {
        if (!isShulkerBox(stack)) return false;

        BlockStateMeta meta = (BlockStateMeta) stack.getItemMeta();
        if (meta == null) return true;

        ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
        ItemStack[] contents = shulkerBox.getInventory().getContents();

        if (contents == null) return true;

        // Check if all slots are empty
        for (ItemStack item : contents) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Consumes required items from inventory
     */
    private boolean consumeRequiredItems(PlayerInventory inventory, int mapsNeeded, int shulksNeeded) {
        int shulksToConsume = shulksNeeded;
        int mapsToConsume = mapsNeeded;

        for (int i = 0; i < 36 && (shulksToConsume > 0 || mapsToConsume > 0); i++) { // Only main inventory
            ItemStack stack = inventory.getItem(i);
            if (stack != null) {
                if (isEmptyShulkerBox(stack) && shulksToConsume > 0) {
                    int consumeAmount = Math.min(shulksToConsume, stack.getAmount());
                    if (stack.getAmount() > consumeAmount) {
                        stack.setAmount(stack.getAmount() - consumeAmount);
                    } else {
                        inventory.setItem(i, null);
                    }
                    shulksToConsume -= consumeAmount;
                }
                else if (stack.getType() == Material.MAP && mapsToConsume > 0) {
                    int consumeAmount = Math.min(mapsToConsume, stack.getAmount());
                    if (stack.getAmount() > consumeAmount) {
                        stack.setAmount(stack.getAmount() - consumeAmount);
                    } else {
                        inventory.setItem(i, null);
                    }
                    mapsToConsume -= consumeAmount;
                }
            }
        }

        return (shulksToConsume == 0 && mapsToConsume == 0);
    }

    /**
     * Sends help information to the player
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(Component.text("=== ShulkerMapCopier Help ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Usage:", NamedTextColor.YELLOW)
            .append(Component.text(" /copyShulkerMap [amount]", NamedTextColor.WHITE)));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Description:", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("• Duplicates a shulker box that contains only filled maps.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Hold the shulker box you want to copy in your main hand.", NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Requirements per copy:", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("• 1 empty shulker box (any color/name)", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Empty maps equal to the number of filled maps in the original", NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Examples:", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("• ", NamedTextColor.GRAY)
            .append(Component.text("/copyShulkerMap", NamedTextColor.WHITE))
            .append(Component.text(" → Makes 1 copy", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("• ", NamedTextColor.GRAY)
            .append(Component.text("/copyShulkerMap 5", NamedTextColor.WHITE))
            .append(Component.text(" → Makes 5 copies", NamedTextColor.GRAY)));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Notes:", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("• Maximum " + MAX_COPIES + " copies per command", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Copies keep the same color, name, and contents as the original", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Empty shulkers used as materials can have any color or name", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Stacks of filled maps count toward the required number of empty maps", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• If inventory is full, copies will be dropped on the ground!", NamedTextColor.YELLOW));
    }
}