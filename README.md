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
    * Requires two locations to be selected using left and right click.
* `/zone subcreate [regionKey]`: Creates a new sub-region within an existing region using the currently selected
  locations.
    * Usage: `/zone subcreate [regionKey]`
    * `regionKey` is optional, if not provided, the region at the player's location will be used.
    * Requires two locations to be selected using left and right click.
* `/zone delete <regionKey>`: Deletes an existing region.
    * Usage: `/zone delete <regionKey>`
    * Requires the `regionKey` of the region to be deleted.
* `/zone info [regionKey]`: Displays information about a region.
    * Usage: `/zone info [regionKey]`
    * If `regionKey` is not provided, the region at the player's location will be used.
* `/zone list`: Lists all regions the player is a member of.
    * Usage: `/zone list`
* `/zone cancel`: Cancels the current region selection.
    * Usage: `/zone cancel`
* `/zone set <regionKey> <player> <permission> <value>`: Sets a permission for a member of a region.
  * Usage: `/zone set <regionKey> <player> <permission> <value>`
  * `<regionKey>` is the key of the region.
  * `<player>` is the name of the player.
    * `<permission>` is the permission to set (e.g., `role`, `break`, `place`, `interact`).
    * `<value>` is the value to set for the permission (
      e.g., `owner`, `true`, `false`, `*`, `!*`, `GRASS_BLOCK`, `!GRASS_BLOCK`).

## Code Structure

The plugin's code is organized as follows:

* `de.t14d3.zones`: Contains the main plugin class (`Zones.java`), region management (`RegionManager.java`), permission
  management (`PermissionManager.java`), and the `Region.java` class.
* `de.t14d3.zones.listeners`: Contains event listeners for player interactions (`PlayerInteractListener.java`), command
  handling (`CommandListener.java`), and player quit events (`PlayerQuitListener.java`).
* `de.t14d3.zones.utils`: Contains utility classes such as `Utils.java` for general helper functions, `BeaconUtils.java`
  for beacon visualization, `Types.java` for defining possible type values (Blocks, Entities, etc.), and `Actions.java`
* for defining different actions.

## Usage

### Creating a Region

1. Use left-click on a block to set the first corner of the region. A green beacon will appear.
2. Use right-click on a block to set the second corner of the region. A red beacon will appear.
3. Use the `/zone create` command to create the region.

### Creating a Sub-Region

1. Select the two corners of the sub-region as described above.
2. Use the `/zone subcreate` command to create the sub-region within the region you are standing in or
   use `/zone subcreate <regionKey>` to create a sub-region within the specified region.

### Setting Permissions

1. Use the `/zone set <regionKey> <permission> <value>` command to set permissions for a member of a region.
  * Example: `/zone set <key> Player1 break true` allows "Player1" to break blocks in the specified region.
  * Example: `/zone set <key> Player2 break GRASS_BLOCK` allows "Player2" to break only `GRASS_BLOCK` blocks
    in the specified region.
  * Example: `/zone set <key> Player3 interact !OAK_DOOR` denies "Player3" to interact with `OAK_DOOR`in
    the specified region.
  * Example: `/zone set <key> Player4 place *` allows "Player4" to place all blocks in the specified region.
  * Example: `/zone set <key> Player5 break !*` denies "Player5" to break all blocks in the specified region.

### Viewing Region Information

1. Use the `/zone info` command to view information about the region you are standing in.
2. Use the `/zone info <regionKey>` command to view information about a specific region.

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
