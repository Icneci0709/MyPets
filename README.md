# MyPets

MyPets is a comprehensive pet management plugin for Minecraft Paper servers (1.20.4+). Tame, store, and share your favorite animals with ease, while keeping them safe in your own farm.

## Features
- **Tameable Pets** – Automatically registers tamed horses, donkeys, mules, cats, wolves, parrots, llamas, camels, and striders.
- **Pet Egg System** – Release a pet from an egg to restore all its attributes (color, style, health, name, etc.).
- **Farm System** – Create private farms with a barrel chest and information sign, perfect for storing pet eggs.
- **Leash Teleportation** – Bring one leashed animal with you when you teleport.
- **Multi-language Support** – Built-in translations for Chinese (Simplified & Traditional), English, Japanese, and Korean.
- **World Whitelist** – Limit plugin functionality to specific worlds.

## Commands

### Pet Commands (`/pet`)
| Command | Description |
|---------|-------------|
| `/pet` | Open the pet management menu |
| `/pet list` | List all your pets |
| `/pet info [index]` | Show detailed info about a pet (also works when looking at a pet) |
| `/pet rename <name>` | Rename a pet |
| `/pet summon <index>` | Teleport a pet to you |
| `/pet recall <index>` | Send a pet back to its bound farm |
| `/pet kill [index]` | Kill a pet |
| `/pet release [index]` | Release a pet into the wild |

### Farm Commands (`/farm`)
| Command | Description |
|---------|-------------|
| `/farm` | Open the farm management menu |
| `/farm create <name>` | Create a new farm (use a wooden shovel to select two points first) |
| `/farm confirm` | Confirm farm creation |
| `/farm list` | List your farms |
| `/farm info [index]` | Show farm details |
| `/farm rename <name>` | Rename your current farm |
| `/farm remove [index]` | Delete a farm |
| `/farm tp <index>` | Teleport to a farm |
| `/farm clear` | Clear your wooden shovel selection |

### Admin Commands (`/mp`)
| Command | Description |
|---------|-------------|
| `/mp reload` | Reload configuration and language files |
| `/mp help` | Show admin help |
| `/mp world add <world>` | Add a world to the whitelist |
| `/mp world remove <world>` | Remove a world from the whitelist |
| `/mp world list` | Show whitelisted worlds |
| `/mp world enable` | Enable world whitelist |
| `/mp world disable` | Disable world whitelist |

## Permissions
- `mypets.command.pet` – Allows use of `/pet` (default: true)
- `mypets.command.pet.kill` – Allows killing a pet (default: true)
- `mypets.command.pet.release` – Allows releasing a pet (default: true)
- `mypets.command.farm` – Allows use of `/farm` (default: true)
- `mypets.command.farm.remove` – Allows deleting a farm (default: true)
- `mypets.admin.reload` – Allows reloading the plugin (default: op)
- `mypets.admin.world` – Allows managing world whitelist (default: op)
- `mypets.egg.release` – Allows releasing pet eggs (default: true)

See `plugin.yml` for the full permission tree.

## Installation
1. Download the latest `MyPets-*.jar` from [GitHub Releases](https://github.com/your-repo/releases).
2. Place the jar into your server's `plugins` folder.
3. Restart your server (or use `/reload confirm`).
4. The plugin will generate `plugins/MyPets/config.yml` and language files.
5. Edit the configuration to your liking and use `/mp reload` to apply changes.

## Configuration (`config.yml`)
```yaml
language: "zh_CN"           # default language
max_farms: 1                # maximum farms per player
selection_tool: "WOODEN_SHOVEL"
max_farm_size_x: 20
max_farm_size_y: 20
max_farm_size_z: 20
# ... (see config.yml for all options)
```

## Language Support
Language files are located in `plugins/MyPets/lang/`.  
Available translations:
- `zh_CN` – Simplified Chinese
- `zh_TW` – Traditional Chinese
- `en_US` – English
- `ja_JP` – Japanese
- `ko_KR` – Korean

To add a new language, copy an existing file, translate it, and set `language:` in the config.

## Compatibility
- **Server software:** Paper 1.20.4+ (Purpur and other forks should work)
- **Java:** 21 or newer
- **No additional plugins required**

## Building from Source
```bash
git clone https://github.com/your-repo/MyPets.git
cd MyPets
./mvnw clean package
```
The compiled jar will be in `target/MyPets-*.jar`.

## License
This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.

---

Enjoy your pets! 🐾
