This is my RTS game! Currently it is based somewhat on StarCraft.

## Fog of war

- Unexplored ground is dark; explored ground becomes dim when it leaves sight.
- Units reveal a 270-pixel radius and completed structures reveal 360 pixels.
  Allied players share vision. Resources and unfinished construction sites do
  not reveal terrain.
- Enemy units, structures, and construction sites are hidden outside current
  sight. Known mineral fields remain visible on explored ground, but cannot be
  inspected or targeted until scouted again.
- Hidden enemies cannot be selected, hovered, or automatically targeted. Units
  stop tracking an enemy when sight is lost; attack-move, patrol, and hold orders
  retain their original behavior. Movement and rally orders into the fog are allowed.
- A new building's entire footprint must be currently visible. Placement previews
  do not report hidden obstructions.

Sight is circular, without terrain or building occlusion. It updates ten times
per simulation second on a 7.5-pixel grid. The overlay uses a single map-resolution
texture with hard pixel borders and uploads only changed rows. The HUD,
placement grid, and rally markers remain above it. Sight ranges and
the grid size are configured in `FogOfWar.java`.

Run the regression suite with JDK 22 and `./mvnw.cmd test`.

Hold the middle mouse button over the map and drag to pan the camera. Panning
stays within map bounds and pauses edge scrolling until the button is released.

The bottom-left minimap shows explored terrain, fog, player-colored units and
buildings, known resource fields, and a white outline of the camera view. Click
or drag with the left mouse button to move the camera, including into unexplored
terrain. Enemy markers disappear when vision is lost. The map refreshes with fog
at 10 Hz; the camera outline follows movement each frame.
