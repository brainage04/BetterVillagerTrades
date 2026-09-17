#!/usr/bin/env python3
"""Offline render: PYTHONPATH=<Pillow site-packages> python3 render.py.
Only integer nearest-neighbor enlargement, alpha compositing and 90-degree transpose.
Gear is historical procedural frame zero, not a newly drawn approximation.
"""
from pathlib import Path
import json
from PIL import Image, ImageDraw
ROOT = Path(__file__).resolve().parent
SRC = ROOT / 'sources'
entries = []

def load(name):
    return Image.open(SRC / (name + '.png')).convert('RGBA')

def canvas(w=32, h=None):
    return Image.new('RGBA', (w, h or w))

def scale(im, factor=2):
    return im.resize((im.width * factor, im.height * factor), Image.Resampling.NEAREST)

def save(project, slug, im, notes, source='Official Java 1.21.4 client textures; see provenance.json'):
    im.save(ROOT / (slug + '.png'))
    entries.append(dict(project=project, label=notes, path='pixel/' + slug + '.png', method='Pillow deterministic native-pixel composition', source=source, notes=notes))

def face(skin):
    result = skin.crop((8, 8, 16, 16))
    result.alpha_composite(skin.crop((40, 8, 48, 16)))
    return result

# Heart behind both swords. Transpose is exactly 90 degrees counterclockwise.
im = canvas()
im.alpha_composite(scale(load('full'), 3), (2, 2))
im.alpha_composite(scale(load('diamond_sword')))
im.alpha_composite(scale(load('iron_sword').transpose(Image.Transpose.ROTATE_90)))
save('BrainageMinigames', 'brainage-minigames', im, 'Heart back; diamond middle; iron front, exactly 90 degrees CCW (blade top-left).')
# Java 1.21.4 Speed colour 0x33EBFF multiplies potion_overlay only (minecraft.wiki/w/Speed).
potion = canvas(16)
liquid = load('potion_overlay')
liquid.putdata([(r * 51 // 255, g * 235 // 255, b, a) for r,g,b,a in liquid.get_flattened_data()])
potion.alpha_composite(liquid)
potion.alpha_composite(load('potion'))
potion.save(SRC / 'speed-potion-composited.png')
im = canvas(32)
im.alpha_composite(scale(load('half')), (0, 8))
im.alpha_composite(potion, (16, 8))
save('AcceleratedDamage', 'accelerated-damage', im, 'Vanilla half-heart left (2x); actual speed-tinted potion right (native 16x16).')

# Vanilla mob effect includes a 1px transparent border: remove padding, never resample.
luck = load('luck').crop((1, 1, 17, 17))
# Villager head is 8x10, unlike player heads. Include all ten rows and the nose front UV.
villager = load('villager')
vface = villager.crop((8, 8, 16, 18))
vface.alpha_composite(villager.crop((26, 2, 28, 6)), (3, 6))
im = scale(luck)
im.alpha_composite(scale(vface), (8, 6))
save('BetterVillagerTrades', 'better-villager-trades', im, 'Luck 16x16 content doubled behind FULL 8x10 villager face plus nose, doubled and centered; final 32x32.')

# Faithful frame zero mapping from the historical procedural gear algorithm.
gear, middle = load('gear'), load('gearmiddle')
g = canvas(16)
for y in range(16):
    for x in range(16):
        p = middle.getpixel((x,y))
        if p[3] <= 128:
            p = gear.getpixel((int((x/15 - .5)*31 + 16), int((y/15 - .5)*31 + 16)))
        g.putpixel((x,y), (*p[:3], 255 if p[3] > 128 else 0))
g.save(SRC / 'gear-frame-zero-16.png')
skinface = face(load('supplied-skin'))
im = scale(g)
im.alpha_composite(scale(skinface), (8,8))
save('BrainageServerUtils', 'brainage-server-utils', im, 'Historical vanilla gear frame 16x16 doubled behind supplied skin face 8x8 doubled; centered 32x32.', 'Mojang Alpha a1.0.4 jar retained misc/gear.png + misc/gearmiddle.png; supplied skin; provenance.json')

info = canvas(16)
d = ImageDraw.Draw(info)
d.ellipse((0,0,15,15), fill='#1863ba')
d.rectangle((7,3,8,4), fill='white')
d.rectangle((7,6,8,11), fill='white')
d.rectangle((5,11,10,12), fill='white')
im = scale(load('enchanted_book'))
im.alpha_composite(info, (8,8))
save('GetEnchantInfo', 'get-enchant-info', im, 'Enchanted book 16x16 doubled; native 16x16 pixel information symbol centered in front.')

im = scale(load('milk_bucket'))
im.alpha_composite(scale(face(load('steve'))), (8,8))
save('MilkablePlayers', 'milkable-players', im, 'Milk bucket doubled to 32x32; Steve face doubled and centered in front.')

bubble = canvas(16)
d = ImageDraw.Draw(bubble)
d.rectangle((1,1,14,11), fill='#9146ff')
d.rectangle((2,12,7,12), fill='#9146ff')
d.polygon([(3,12),(3,15),(6,12)], fill='#9146ff')
for x in (3,7,11):
    d.rectangle((x,5,x+1,6), fill='white')
save('SimpleTwitchChat', 'simple-twitch-chat', bubble, 'Native 16x16 purple speech bubble, three white dots, alpha background.', 'Original deterministic pixel symbol')

for size in range(4,9):
    im = scale(skinface)
    plus = canvas(size)
    d = ImageDraw.Draw(plus)
    thickness = 2 if size % 2 == 0 else 1
    lo = (size - thickness)//2
    d.rectangle((lo,0,lo+thickness-1,size-1), fill='#55ff55')
    d.rectangle((0,lo,size-1,lo+thickness-1), fill='#55ff55')
    im.alpha_composite(plus, (16-size,0))
    save('NPCAddons', 'npc-addons-plus-' + str(size), im, f'Supplied 8x8 skin face doubled; native {size}x{size} plus at top-right; final16x16.', 'Authorized supplied skin + deterministic pixel plus')

# Same bubble as chat, 2x. Small overlays left/right, not a decorative background.
im = scale(bubble)
play = canvas(12)
d = ImageDraw.Draw(play)
d.polygon([(2,1),(2,10),(10,5)], fill='white')
im.alpha_composite(play, (0,11))
grass = load('grass_block_side')
overlay = load('grass_block_side_overlay')
overlay.putdata([(r*145//255,g*189//255,b*89//255,a) for r,g,b,a in overlay.get_flattened_data()])
grass.alpha_composite(overlay)
im.alpha_composite(grass, (16,10))
save('TwitchPlaysMinecraft', 'twitch-plays-minecraft', im, 'SimpleTwitchChat bubble doubled; white pixel play button left; vanilla grass-block side with default grass tint right.')
(ROOT / 'manifest.json').write_text(json.dumps({'entries': entries}, indent=2) + '\n')
print(json.dumps([{'file': e['path'], 'size': Image.open(ROOT / Path(e['path']).name).size, 'mode': Image.open(ROOT / Path(e['path']).name).mode} for e in entries], indent=2))
