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
- [ ] Use a JSON file to set and save plot data on a player
    - `uuid`, `plot_width`, `plot_height`, `plot_x`, `plot_y`