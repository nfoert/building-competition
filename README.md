# building-competition
A Minecraft paper plugin for a long-term building competition.

Uses MultiverseCore for world management and FastAsyncWorldEdit for building the plots.

- Create a plot for a user on server join or world join, tiled in a plot world next to other plots, and automatically teleport them there
- Keep the user inside of their plot, and don't let them damage other user's plots

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
