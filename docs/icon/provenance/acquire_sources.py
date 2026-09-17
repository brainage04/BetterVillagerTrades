#!/usr/bin/env python3
"""Reacquire pinned official textures, checking recorded hashes before writing.
Existing files are preserved. The authorized local skin is never fetched from username.
"""
from pathlib import Path
import hashlib, io, json, urllib.request, zipfile
ROOT = Path(__file__).resolve().parent
archives = {}
for item in json.loads((ROOT / 'provenance.json').read_text()):
    target = ROOT / 'sources' / item['file']
    if target.exists():
        data = target.read_bytes()
    elif 'member' in item:
        url = item['url']
        if url not in archives:
            archives[url] = zipfile.ZipFile(io.BytesIO(urllib.request.urlopen(url, timeout=90).read()))
        data = archives[url].read(item['member'])
    else:
        data = Path(item['source']).read_bytes()
    if hashlib.sha256(data).hexdigest() != item['sha256']:
        raise RuntimeError('Source checksum mismatch: ' + str(target))
    if not target.exists():
        target.write_bytes(data)
    print(item['file'], item['sha256'])
