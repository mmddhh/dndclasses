# Tectonic terrain prototype

This offline preview does not change the Minecraft world preset. It uses a
245 × 245 torus, with one sample per 256 blocks. Plate ownership is separate
from the initial land/ocean crust field. Relative plate motion supplies uplift,
subsidence and shear along shared boundaries. Over 32 iterations, global
drainage cuts valleys and deposits some sediment downhill.

From the repository root with JDK 17:

```powershell
javac -d build/tectonic-classes src/main/java/mdh/dndclasses/worldgen/terrain/WorldTopology.java src/main/java/mdh/dndclasses/worldgen/terrain/TectonicPrototype.java
java -cp build/tectonic-classes mdh.dndclasses.worldgen.terrain.TectonicPrototype 12345 build/tectonic-preview/12345
```

- `plates.png`: ownership and velocity arrows. Both map edges wrap.
- `initial.png` and `evolved.png`: elevation before and after evolution, on the same color scale.
- `drainage.png`: cyan channels continue to sea; purple channels terminate inland.
  Tan depressions are dry, lavender depressions are seasonal, and blue inland
  basins have enough modeled supply to overflow.
- `summary.txt`: plate velocities, cell counts and longest route to an outlet.
  Route length counts grid steps, including diagonal steps; it is not a block distance.

The preview checks plate connectivity and acyclic drainage. This is a fixed
boundary model, not a plate advection solver. The water budget is a coarse
classification; it does not establish Minecraft water levels. The prototype
currently treats every below-sea sample as ocean. Coastal connectivity, river
profiles at block scale, and caves, vegetation, natural wonders and miracles
still need integration into the new preset.
