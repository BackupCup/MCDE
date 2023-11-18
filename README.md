# Introduction
Now with existance of both MC Dungeons Weapons and MC Dungeons Armor mods for Fabric, the only part of the gear expirience of Miencraft Dungeons that's missing is it's unique enchanting system. Well, MC Dungeons Enchanting brings this system into Minecraft Java!

The Enchanting table has been removed from the survival gameplay and replaced with it's new version - Runic Table.
MC Dungeons Enchanting has integrated support for both MC Dungeons Weapons and MC Dungeons Armors

# Content
As of right now, the mod adds only three blocks: 
 - Runic Table - the main block, acts as a new enchanting table. Randomly generates a list of enchantments that one Item can take.
 - Roll Bench - an additional block which rerolls a single enchantment per use, costs lapis lazuli. The cost is respective of the level you'd spend to activate this enchantment.
 - Gilding Foundry - at cost of gold ingots applies a random permanent enchantment to the item.

# FAQ
### Can I use your mod in my modpack?
Absolutely! I only ask that you provide a link back to this page.

### I've seen your mod on other sites. Did you post it and are they safe?
I have not, nor do I plan to, post my mod on any other sites than Curseforge, Modrinth or my GitHub page. If you have downloaded my mod from somewhere else, 1. It isn't from me and 2. It may have some kind of malicious files or have been modified in some way. I urge you to delete the file, run a full virus and malware scan and, if you want to use MC Dungeons Enchanting, I recommend to download it from [CurseForge](https://curseforge.com/minecraft/mc-mods/mc-dungeons-enchanting), [Modrinth](https://modrinth.com/mod/mc-dungeons-enchanting) or [GitHub](https://github.com/EndLone/MCDE/releases).

### 1.12.2?
No, just no.

### Forge?
Same as 1.12.2

### Compatibility with other mods
Oficially, we support only mcdw, mcda and mcdar. But our mod already can work with virtually any other mod which adds its own enchantments.
The one thing you should be aware of is that icons for enchantments should be added by a third party (included with that other mod or added via a resourcepack).

#### Adding your own icons
We have [`icon_template.png`](icon_template.png) at root of this project if you are willing to create your own set of icons. (You can also check out actual [textures](/src/main/resources/assets/mcde/textures/gui/icons) provided by MCDE).

There are two rules you should be aware of:
 - Texture for icons should have this identifier `mcde:textures/gui/icons/<namespace>/<path>.png`. For example, icon for enchantment with id `minecraft:sharpness` should have this identifier `mcde:textures/gui/icons/minecraft/sharpness.png`.
 - Everything in red is ignored (i.e. only the most top-left 23x23 area will actually be rendered).
Once the textures are loaded, everything should work just fine.

#### Tagging enchantments as powerful
MCDE uses `#c:powerful` enchantment tag to determine if enchantment is powerful.
This tag is located in `data/c/tags/enchantment/powerful.json` and can be overriden by a datapack.

# Attributions/Special Thanks
- @Iamnotagenius - for developing most of the logic and code behind this enchanting system. Without him, this mod wouldn't exist.
- @unroman - for providing Ukranian translation
- @LeafForge - for providing Polish translation
- @fzzyhmstrs and @chronosacaria - for helping us to get this done
