# Cinema Scope (1.20.1)

Port of [CinemaScope by heconis](https://github.com/heconis/CinemaScope) to **Minecraft 1.20.1 (Fabric)**, extended with a full cinematic mode.

Press **K** (rebindable under Controls → "Cinema Scope"):

- HUD and hand are hidden
- black letterbox bars slide in smoothly from the top and bottom (ease-in-out)
- the camera zooms in slightly at the same time
- Minecraft's smooth camera is enabled

Press **K** again and everything animates back out.

Works with any screen ratio (16:9, 16:10, 4:3, 21:9, …): the bars are computed from the actual window size. On screens that are already as wide as the target ratio (21:9 ultrawide), a minimum bar height keeps the effect visible.

## Requirements

- Minecraft 1.20.1
- Fabric Loader ≥ 0.14.21
- Fabric API

## Config

`config/cinema_scope.json` (created on first launch):

| Key | Default | Meaning |
|---|---|---|
| `targetAspectRatio` | `2.39` | Visible picture ratio once the bars are in |
| `minBarFraction` | `0.06` | Minimum height of each bar as a fraction of screen height |
| `zoomAmount` | `0.15` | Zoom strength (0.15 = FOV −15 %) |
| `animationSeconds` | `1.2` | Duration of the slide animation |
| `hideHud` | `true` | Hide the HUD while active |
| `hideHand` | `true` | Hide the first-person hand while active |
| `smoothCamera` | `true` | Enable smooth camera while active |

## Building

```
./gradlew build
```

The jar ends up in `build/libs/`.

## License

MIT — see [LICENSE](LICENSE). Original work © heconis.
