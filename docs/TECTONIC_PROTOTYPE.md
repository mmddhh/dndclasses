# Tectonic terrain prototype

This offline preview does not change the Minecraft world preset. It uses a
245 × 245 torus, with one sample per 256 blocks. Plates grow from irregular
seeds at different rates across a random resistance field. Continents grow
independently and begin with little relief. No sinusoidal fields are used.

Over 72 iterations, plate fractions carry crust and relief on a subcell grid.
Continental material spreads into gaps without gaining mass; extension
thins the crust and lowers its surface before sustained extension can admit
new oceanic crust. Overlaps thicken continental crust and raise terrain.
Global drainage periodically cuts valleys and deposits sediment downhill.

From the repository root with JDK 17:

```powershell
javac -encoding UTF-8 -d build/tectonic-classes src/main/java/mdh/dndclasses/worldgen/terrain/WorldTopology.java src/main/java/mdh/dndclasses/worldgen/terrain/PreviewHydrology.java src/main/java/mdh/dndclasses/worldgen/terrain/TectonicPrototype.java
java -cp build/tectonic-classes mdh.dndclasses.worldgen.terrain.TectonicPrototype 12345 build/tectonic-preview/12345
java -cp build/tectonic-classes mdh.dndclasses.worldgen.terrain.TectonicPrototype --gallery build/tectonic-v2 7 42 12345 314159 20261127
java -cp build/tectonic-classes mdh.dndclasses.worldgen.terrain.TectonicPrototype --check-rift build/tectonic-v3/seed-7
java -Xmx768m -cp build/tectonic-classes mdh.dndclasses.worldgen.terrain.TectonicPrototype --detail build/tectonic-detail 7 0.40 42 12345 314159 20261127
java -Xmx768m -cp build/tectonic-classes mdh.dndclasses.worldgen.terrain.TectonicPrototype --detail-64 build/tectonic-detail 7 0.40 42 12345 314159 20261127
```

Open `build/tectonic-v2/index.html` to compare the five seeds and switch
between the same iteration in all of them.

Open `build/tectonic-detail/index.html` to compare 256-, 128-, and (after
`--detail-64`) 64-block sampling. Run `--detail` first on a fresh directory.
Use the same seed order and land fraction for both commands. The second
command adds 64-block results one seed at a time and skips completed runs.
The seed and iteration selectors update all available maps. The drainage
layer shows iteration 72. Finer grids rerun the simulation, so coastlines
and drainage can differ between resolutions.

- `plates.png`: ownership and velocity arrows. Both map edges wrap.
- `initial.png` and `evolved.png`: relief before and after evolution.
- `step-00.png` through `step-72.png`: relief at intervals of 18 steps.
  Matching `-plates` images use one color per dominant plate. Separate
  `-thickness` images show continental thickness (lighter is thicker), and
  `-rift` images show accumulated extension (red) and new ocean floor (blue).
- `--check-rift` runs seed 7 with one and two substeps per iteration, exports
  every iteration from 18 to 36, and logs flooded and isolated low cells in
  a fixed lower-left window. Those counts also reflect coastline movement.
- `drainage.png`: runoff follows downhill routes with a preference for convergent
  valleys. Only established channels are drawn, with thicker main rivers.
  Cyan channels continue to sea; purple channels enter an inland basin.
  Tan depressions are dry, lavender water is seasonal, light blue lakes are
  closed, and darker blue lakes can overflow. Partly filled lakes cover only
  the floor supported by the water budget, and only surplus leaves a spillway.
- `summary.txt`: plate velocities, cell counts, lake outlets, channel junctions
  and the longest visible reach.
  Route length counts grid steps, including diagonal steps; it is not a block distance.

The preview checks initial plate connectivity, continental mass conservation,
and acyclic drainage, and counts small fragments on the dominant-plate map.
The first-order transport can smooth boundaries; plate velocities are
constant, and neighboring materials within one cell share a bulk state.
Only the largest connected below-sea component counts as open
ocean. The water budget is a coarse classification; it does not establish
Minecraft water levels. River profiles at block scale, caves, vegetation,
natural wonders and miracles still need integration into the new preset.
