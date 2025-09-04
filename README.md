# ShulkerMapCopier


A simple plugin for **Folia (and compatible with Paper/Spigot)** Minecraft servers that allows cloning shulkers containing only filled maps, provided the player has the required materials.

This plugin was originally created for the mc.wubby.tv Minecraft server to help the growing mapart community share and sell their mapart.

---

## Usage

### Commands
- `/copyshulkermap [amount|help]`
- `/csm [amount|help]` *(alias)*

### Arguments
- `help` → Shows detailed help message.
- `amount` → Number of copies (defaults to `1` if omitted, maximum `10`).

### Function
Clones the shulker box currently held in the player’s **main hand** if all required materials are present.

---

## Requirements & Conditions
1. The shulker to be cloned must contain **only filled maps**. Maps may be stacked.
2. The player must have at least as many **empty shulker boxes** as copies requested.  
   - Any color and custom name are allowed as long as the shulker is empty.
3. The player must have at least as many **empty maps** as there are filled maps inside the original shulker, multiplied by the number of copies requested.

If all conditions are met, materials are consumed, and the player receives the specified number of identical copies. Otherwise, no copy is given, and the player receives a message explaining what went wrong.

---

## Important Notes
- The cloned shulker will be **identical** to the original (color, name, contents).
- Empty shulkers used as materials can be of any color or name as long as they are empty.
- Stacks of filled maps are fully counted toward the required number of empty maps.
- If the player’s inventory is full, cloned shulkers will drop on the ground.
- Maximum copies per command: **10**.

---

## Permissions
- `shulkermapcopier.use` → Allows use of `/copyshulkermap` and `/csm` commands.  
  Default: **true** (all players).
