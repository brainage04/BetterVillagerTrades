# BetterVillagerTrades icon

## What this is

The mod's icon: `icon.png` — 32x32 RGBA PNG, 579 bytes,
sha256 `5a35dfbc7624e0eb635e3f1efe0326c98f9563fc3a3ef6649b9033bd2cf1fecf`.

Two layers: behind, the vanilla **Luck** mob-effect icon enlarged 2x; in front, a **villager
head** (full 8x10 face plus its nose) enlarged 2x and centred.

## How it was made

**Method: generated pixel art from vanilla textures.** This is *not* an in-game screenshot and
*not* a Blender render. It is a deterministic offline composition of official Minecraft
textures with Pillow; the only pixel operations used are nearest-neighbour enlargement,
alpha compositing and a transparent-border trim.

| | |
|---|---|
| Tool | Pillow 12.3.0 on Python 3.13 (`provenance/render.py`, the `BetterVillagerTrades` block) |
| Background source | official Java 1.21.4 `assets/minecraft/textures/mob_effect/luck.png` (18x18, sha256 `6289a3a1…`), pinned by URL + member + sha256 in `provenance.json` |
| Background handling | the texture's fully transparent 1-pixel padding is cropped off (`(1,1)-(17,17)`) to give exactly 16x16 of content — a trim, never a resample — then doubled to 32x32 |
| Foreground source | official Java 1.21.4 `assets/minecraft/textures/entity/villager/villager.png` (sha256 `dca7cc3e…`) |
| Foreground geometry | the **full 8x10 villager face** UV `(8,8)-(16,18)` — all ten rows including the chin, not the 8x8 player crop — with the nose's front UV `(26,2)-(28,6)` overlaid at `(3,6)`; then doubled to 16x20 and centred at `(8,6)` on the 32x32 canvas |
| Composition | 32x32 canvas, background at `(0,0)`, face on top at `(8,6)` |

Everything is integer, deterministic and replayable. Verified while creating this provenance:
`python3 render.py` in a clean directory holding the 17 shipped source textures reproduces
`icon.png` byte for byte.

## Provenance files

| Path | What it is |
|---|---|
| `render.py` | **The script that produced this icon.** Deterministic Pillow composition of all round-3 pixel icons (this icon's block is the `luck` + `villager` one) |
| `provenance.json` | The pinned source manifest: for every texture, the exact Mojang URL + archive member + sha256, plus the supplied skin's path and sha256 |
| `derivations.json` | The derivation record: Pillow version, the exact runtime command, the `luck` 18x18→16x16 crop rule and the villager face/nose UV notes |
| `sources/` | The 17 source textures `render.py` reads (official Java 1.21.4 client textures, the two Alpha gear textures, and the supplied skin), each hash-verified against `provenance.json` |
| `manifest.json` | The round-3 delivery record for all pixel icons, including this one's label and method |
| `blockers.json` | The pixel session's blocker list (empty for this icon) |
| `acquire_sources.py` | Re-fetch/verify tool for `provenance.json` (see Notes for its known defect; **not needed**, every source is shipped) |

Excluded on purpose: other icons' output PNGs from the same script, the NMSR head-render helper
(`render_service.py`, `service-provenance.json`) which belongs to a different mod's icon, and
`__pycache__`.

## How to regenerate

```sh
cd provenance
PYTHONPATH=/nix/store/4v9j9wbzyhrlx9980ygbr812313mazy0-python3.13-pillow-12.3.0/lib/python3.13/site-packages \
  python3 render.py
```

This rewrites `better-villager-trades.png` (and the other pixel icons the script contains) and
`manifest.json`. It needs no network: every texture `render.py` reads is in `sources/`, and each
one was verified against its pinned sha256 when this provenance was assembled. `render.py`
itself never downloads anything; it only reads `sources/`.

## Notes

* The villager head is 8x10, unlike a player head's 8x8, and this icon deliberately uses all ten
  rows plus the nose quad — a plain 8x8 crop of the villager texture would cut the chin.
* The Luck texture ships with a 1-pixel fully transparent border; it is cropped, not scaled, so
  the doubling stays on the native pixel grid.
* The sources are official Mojang Java 1.21.4 client textures, extracted from the pinned
  `client.jar` object recorded in `provenance.json`; they are not re-drawn by hand.
* `acquire_sources.py` has a defect inherited from the round-3 session: `provenance.json` lists
  `potion.png` twice (once as `item/potion.png`, once as `gui/sprites/container/slot/potion.png`),
  so on the second entry the already-written file fails the checksum comparison and the script
  raises after writing 13 of its 17 files. `render.py` needs the file, so it is shipped here
  verified against the `item/potion.png` pin.
* All colours and pixels come from those sources; nothing is interpolated, so the icon is crisp
  at 32x32 and at integer multiples of it.

## Working-tree note

The round-3 working tree that produced this icon was cleaned up after integration. Every file needed to regenerate the icon was copied into `provenance/`; the copies live under `provenance/from-round3/` when they came from the working tree. Any remaining `round3/...` mention records where something came from, not a path that still exists.
