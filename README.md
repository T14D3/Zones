# Zones Plugin

This plugin allows server administrators and players to create and manage protected regions within their Minecraft
world. It provides a flexible system for defining areas, setting permissions, and controlling interactions within those
areas.

## Features

* **Region Creation:** Create regions using in-game selection or manual coordinates.
* **Sub-Regions:** Create sub-regions within existing regions for more granular control.
* **Permission Management:** Set detailed permissions for players within regions, controlling actions like breaking,
  placing, interacting, and more.
* **Member Management:** Add and remove members from regions with specific permissions.
* **Region Overlap Control:** Configure whether regions can overlap.
* **Bypass Permissions:** Allow specific players to bypass region restrictions.
* **Configuration:** Customize messages and region settings through configuration files.
* **Beacon Visualization:** Visualize selected regions using temporary beacons.

## Commands

The plugin provides the following commands:

* `/zone create`: Creates a new region using the currently selected locations.
    * Usage: `/zone create`
  * Requires two locations to be selected using left and right click, starts selection if not present.
  * Prevents creation if the new region would overlap an existing region, unless the `zones.create.overlap`
    permission is set.
* `/zone subcreate`: Creates a new sub-region within an existing region using the currently selected
  locations.
    * Usage: `/zone subcreate [regionKey]`
    * `regionKey` is optional, if not provided, the region at the player's location will be used.
  * Requires two locations to be selected using left and right click, starts selection if not present.
  * Player needs to be considered an admin in the region, or have the `zones.subcreate.other` permission
* `/zone delete`: Deletes an existing region.
    * Usage: `/zone delete <regionKey>`
    * Requires the `regionKey` of the region to be deleted.
  * Player needs to be the owner of the region, or have the `zones.delete.other` permission
* `/zone expand`: Expands an existing region by a specified amount.
  * Usage: `/zone expand <regionKey> <amount> [overlap]`
  * Player needs to be an admin of the region, or have the `zones.expand.other` permission
  * If `overlap` is set to `true` and the player has the `zones.expand.overlap` permission,
  * the region will be expanded even if it overlaps with other regions.
* `/zone info`: Displays information about a region.
    * Usage: `/zone info [regionKey]`
    * If `regionKey` is not provided, the region at the player's location will be used.
  * To show region members, the player needs to be an admin of the region, or have the `zones.info.other` permission
* `/zone list`: Lists all regions the player is a member of.
    * Usage: `/zone list`
  * Only shows regions the player is a member of, unless the player has the `zones.list.other` permission
* `/zone cancel`: Cancels the current region selection.
    * Usage: `/zone cancel`
* `/zone set`: Sets a permission for a member of a region.
  * Usage: `/zone set <regionKey> <who> <permission> <value>`
  * `<regionKey>` is the key of the region.
  * `<who>` who to set the permission for.
    * e.g., `Player1` - to create or modify a group, use `+group-GROUPNAME`, where GROUPNAME can be any name.
  * `<permission>` is the permission to set (e.g., `role`, `break`, `place`, `interact`).
  * `<value>` is the value to set for the permission (
    * e.g., `owner`, `true`, `false`, `*`, `!*`, `GRASS_BLOCK`, `!GRASS_BLOCK`).
* `/zone rename`: Renames a region.
  * Usage: `/zone rename <regionKey> <newName>`
  * `<regionKey>` is the key of the region, `<newName>` is the new name of the region.
  * Player needs to be an admin of the region, or have the `zones.rename.other` permission
* `/zone select`: Selects a region.
  * Usage: `/zone select [regionKey]`
  * If `regionKey` is not provided, the region at the player's location will be used.
  * Visually highlights the region in the world using particles.
  * Player needs to be a member of the region, or have the `zones.select.other` permission
* `/zone mode`: Changes the selection mode.
  * Usage: `/zone mode <mode>`
  * `<mode>` is the new mode to set the player to.
    * e.g., `3d` for 3D selection mode, `2d` for 2D selection mode.
* `/zone find`: Finds regions.
  * Usage: `/zone find`
  * Displays a bossbar with the name(s) of all regions at the player's location.

* `/zone save`: Manually saves all regions to file.
  * Usage: `/zone save`
  * Admin only, requires the `zones.save` permission (not given by default)
* `/zone load`: Manually loads all regions from file.
  * Usage: `/zone load`
  * Admin only, requires the `zones.load` permission (not given by default)
* `/zone import`: Imports regions from another plugin.
  * Usage: `/zone import <pluginName>`
  * Currently only supports WorldGuard.
  * Admin only, requires the `zones.import` permission (not given by default)

## Usage

### Creating a Region

1. Use the `/zone create` command to start the creation process.
2. Use left-click on a block to set the first corner of the region. A green beacon will appear.
3. Use right-click on a block to set the second corner of the region. A red beacon will appear.
4. Use the `/zone create` command to create the region.

### Creating a Sub-Region

1. Use the `/zone subcreate` command to start the creation process.
2. Select the two corners of the sub-region as described above.
3. Use the `/zone subcreate` command to create the sub-region within the region you are standing in or
   use `/zone subcreate <regionKey>` to create a sub-region within the specified region.

### Setting Permissions

1. Use the `/zone set <regionKey> <player> <permission> <value>` command to set permissions for a member of a region.
  * Example: `/zone set <key> Player1 break true` allows "Player1" to break blocks in the specified region.
  * Example: `/zone set <key> Player2 break GRASS_BLOCK` allows "Player2" to break only `GRASS_BLOCK` blocks
    in the specified region.
  * Example: `/zone set <key> Player3 interact !OAK_DOOR` denies "Player3" to interact with `OAK_DOOR`in
    the specified region.
  * Example: `/zone set <key> Player4 place *` allows "Player4" to place all blocks in the specified region.
  * Example: `/zone set <key> Player5 break !*` denies "Player5" to break all blocks in the specified region.
* Groups:
  * To assign a group, set the value of the permission `group` to the group name.
  * e.g.: Create a group and assign permission: `/zone set <regionKey> +group-some-group-name break true`
  * e.g. `/zone set <regionKey> ExamplePlayer group some-group-name`
  * ExamplePlayer now inherits the permission `break` from the group `some-group-name`

### Viewing Region Information

1. Use the `/zone info` command to view information about the region you are standing in.
2. Use the `/zone info <regionKey>` command to view information about a specific region.

## Integrations

### WorldGuard

Zones supports importing WorldGuard regions via the `/zone import WorldGuard` command. To use this feature,
you must have WorldGuard installed and enabled. Zones will automatically import all Cuboid regions, including
their members.

### WorldEdit

WorldEdit can only be used to modify blocks in a region if the executing player would be allowed to manually
modify the region.

### PlaceholderAPI

Zones provides multiple placeholders for use with the PlaceholderAPI plugin, displaying region information for the
region the player is standing in. It currently supports the following placeholders:

* `%zones_get_name%`
* `%zones_get_key%`
* `%zones_get_members%`
* `%zones_get_owner%`
* `%zones_get_min%`
* `%zones_get_min_x%`
* `%zones_get_min_y%`
* `%zones_get_min_z%`
* `%zones_get_max%`
* `%zones_get_max_x%`
* `%zones_get_max_y%`
* `%zones_get_max_z%`
* `%zones_is_member%`
* `%zones_can_place_hand%`
* `%zones_can_break_target%`
* `%zones_can_<action>_<type>%`

## Design Choices

* **YAML Configuration:** The plugin uses YAML files for configuration and storage, making it easy to read and modify.
  Additional storage options are planned for future releases.
* **Permission System:** The permission system is designed to be flexible and granular, allowing for fine-grained
  control over player interactions.
* **Event-Driven:** The plugin uses Bukkit's event system to handle player interactions, making it efficient and
  responsive.
* **Caching:** The plugin uses a caching system to improve performance when checking permissions.

## Contributing

Contributions to the plugin are welcome! If you have any ideas for new features, bug fixes, or improvements, please feel
free to submit a pull request or open an issue.

## License

This plugin is distributed under the MIT License.
