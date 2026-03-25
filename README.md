# building-competition
A Minecraft paper plugin for a long-term building competition.

Uses MultiverseCore for world management, FastAsyncWorldEdit for building the plots, and WorldGuard for plot protections.

- Create a plot for a user upon running a command, tiled in a plot world next to other plots, and automatically teleport them there
- Keep the user inside of their plot, and don't let them damage other user's plots
- Define a buildable area, so that players can't damage the walls of their plot
- Send the user to their existing plot if it already exists
- Dev mode for testing

## Usage
Place the `.jar` file into your server's `plugins` folder. Make sure you also have `FastAsyncWorldEdit` and `WorldGuard` installed.

Upon starting the server, the default `config.yml` file will be created.

You'll need to create a plot schematic. Build something in game. Use redstone blocks (or configurable with `build-area-corner`) to define the base of the buildable area. The square one block above these corners, and the height defined by `build-height` describes the full buildable area. Players will not be able to build outside of that area once their plot is created.

Once you've completed your plot, save it with FAWE using `/schematic save plot`. Find the `plot.schem` file and copy it into the `/plugins/buiilding-competition` directory.

Then, use `/bc buildplot` to create a plot for the command executor. Subsequent runs of that command will teleport the player into their existing plot.

You can use something like a Multiverse Portal to run that command for a player upon entering an area.

### Full Configuration
Configuration changes can be made to the `/plugins/building-competition/config.yml` file. Run `/bc reload` to apply these changes.

- `plot-world` - Defines the name of the world to build plots in. 
  - Default is `plot`
  - Use something like Multiverse to create new worlds
- `schem-file` - The name of the schematic file to load plots from
  - Default is `plot.schem`
- `dev` - If the plugin should be in dev mode or not
  - Default is `false`
  - If `true`, it won't check if a plot already exists for a player before making another one. This can be useful to test how many plots behave on a server.
- `build-area-corner` - The block that defines the corners of the base of the buildable area
  - Default is `minecraft:redstone_block`
- `build-height` - The height of the buildable area
  - Default is `31`

### Full Command Usage
- `/bc` - Lists all commands for the plugin
- `/bc reload` - Reloads the configuration
- `/bc buildplot` - Builds a plot for the player that executes the command
- `/bc reset` - Resets the `plots.yml` file. This clears all WorldGuard regions and makes places where plots were before able to be replaced with new plots.
- `/bc info` - Gets the owner of the plot that the executor is standing in. Uses the WorldGuard region owner.


## To-Do
- [x] Make a chat command
- [x] Make the chat command support placing a `.schem` file
- [x] Make a configuration system
- [x] Make a command for reloading the configuration
- [x] Place schematic based on configurable `plot_world` and `schem_file`
- [x] Use the PDC to set and save plot data on a player
    1. Check the PDC list of plots, and find the closest location in the x and y direction where a tiled plot can be placed
    2. Build that plot based on the given information
    3. Set plot details in the JSON file: `uuid`, `plot_width`, `plot_height`, `plot_x`, `plot_y`
- [x] Command permissions
- [x] When creating a plot, create a worldguard allow on their plot
- [ ] Refactor and clean up
- [x] Add command for getting plot info