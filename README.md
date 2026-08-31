# ExecutableItems

A lightweight [Paper](https://papermc.io/) plugin that lets you link one or multiple commands to any item — players right-click to execute them. No GUI setup, no per-use-case plugins needed. Just link, save, give.

---


## Commands

All commands are available as `/executableitems` or the short alias `/ei`.

### Item setup

| Command | Description |
|---|---|
| `/ei link add <command> [player\|op]` | Add a command to the held item |
| `/ei link remove <index>` | Remove a specific command by its index |
| `/ei unlink` | Remove **all** EI data from the held item |
| `/ei onaction <type> [args]` | Set what happens to the item after use |
| `/ei cooldown <seconds>` | Per-item cooldown override (`-1` = use global) |
| `/ei permission <node\|remove>` | Restrict item use to a permission node |
| `/ei info` | Show all linked data of the held item |
| `/ei copy` | Copy all EI data from the held item |
| `/ei paste` | Paste copied EI data onto another item |

### Registry

| Command | Description |
|---|---|
| `/ei save <name>` | Save the held item to the registry |
| `/ei give <player> <name> [amount]` | Give a saved item to a player |
| `/ei rename <old> <new>` | Rename a saved item in the registry |

### General

| Command | Description |
|---|---|
| `/ei help` | Show the help page |
| `/ei reload` | Reload `config.yml` and `items.yml` |

---

## On-Action Types

Set what happens to the item **after** the command(s) execute.

| Type | Usage | Description |
|---|---|---|
| `clear single` | `/ei onaction clear single` | Remove 1 from the stack |
| `clear slot` | `/ei onaction clear slot` | Remove the entire stack |
| `damage <n>` | `/ei onaction damage 10` | Reduce durability by N (breaks at 0) |
| `count <n>` | `/ei onaction count 3` | Reduce stack size by N |
| `drop` | `/ei onaction drop` | Drop the item on use |
| `remove` | `/ei onaction remove` | Remove the on-action setting |

---

## Permissions

| Permission | Description | Default |
|---|---|---|
| `executableitems.use` | Use (right-click) linked items | `true` |
| `executableitems.link` | Link, unlink, copy/paste, onaction, cooldown, permission setup | `op` |
| `executableitems.info` | View linked data of an item | `op` |
| `executableitems.bypass.cooldown` | Bypass the cooldown | `op` |
| `executableitems.save` | Save and rename items in the registry | `op` |
| `executableitems.give` | Give saved items to players | `op` |
| `executableitems.reload` | Reload the config | `op` |

---

## Configuration

```yaml
# config.yml

cooldown: 5       # Global cooldown in seconds — 0 to disable

defaults:
  run-as: player  # Default execution mode: player or op

messages:
  prefix: "&8[&bEI&8] "
  linked: "&aCommand added to item!"
  unlinked: "&cAll EI data removed from item."
  not-linked: "&cThis item has no linked command."
  no-permission: "&cYou don't have permission to do that."
  no-item-in-hand: "&cYou must hold an item in your hand."
  cooldown: "&cPlease wait &e%seconds% &cmore second(s)."
  # ... all messages are fully configurable
```

---

## Placeholders

| Placeholder | Value |
|---|---|
| `%player%` | The name of the player who used the item |

---

## Usage Examples

### Single-use voucher
```
/ei link add give %player% diamond 5
/ei onaction clear single
```
Gives the player 5 diamonds and removes one item from the stack on use.

### Kit item with cooldown
```
/ei link add lootcrates give %player% starter 1 op
/ei cooldown 300
/ei save starter-kit
```
Executes the command as console, applies a 5-minute per-item cooldown, and saves it to the registry for distribution.

### Multiple commands in order
```
/ei link add give %player% diamond 1
/ei link add broadcast %player% just got a diamond!
/ei link add title %player% title {"text":"You got it!"}
```
All three commands execute in order on right-click.

### VIP-only item
```
/ei link add give %player% emerald 3
/ei permission vip.items.emerald
```
Only players with `vip.items.emerald` can use this item.

### Distributing a saved item
```
/ei give Sky starter-kit 1
```

---

## Requirements

| | |
|---|---|
| Server | [Paper](https://papermc.io/) 1.21.x |
| Java | 21+ |

---

## Installation

1. Download the latest release JAR from the [Releases](../../releases) page
2. Drop it into your server's `plugins/` folder
3. Start or reload the server
4. Edit `plugins/ExecutableItems/config.yml` as needed
5. Use `/ei reload` to apply config changes in-game

---

## Building from source

```bash
git clone https://github.com/DirectSky943/ExecutableItems.git
cd ExecutableItems
./gradlew build
```

Output JAR: `build/libs/ExecutableItems-1.0.0.jar`

---

*Made by **Sky** — [skyymc.net](https://skyymc.net)*
