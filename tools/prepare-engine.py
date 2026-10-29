#!/usr/bin/env python3
"""Fetch the pinned official engine for a source checkout. No pip/native runtime install."""
from pathlib import Path
import hashlib, ssl, urllib.request

ROOT=Path(__file__).resolve().parent.parent
URL='https://github.com/yt-dlp/yt-dlp/releases/download/2026.10.26/yt-dlp'
SHA256='e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3'
TARGET=ROOT/'app/src/main/assets/yt-dlp-2026.10.26.zip'

def digest(path):
    value=hashlib.sha256()
    with path.open('rb') as stream:
        for data in iter(lambda:stream.read(65536),b''): value.update(data)
    return value.hexdigest()

if TARGET.exists() and digest(TARGET)==SHA256:
    print('Pinned engine already present and verified')
else:
    class HTTPSOnly(urllib.request.HTTPRedirectHandler):
        def redirect_request(self,req,fp,code,msg,headers,newurl):
            if not newurl.lower().startswith('https://'): raise RuntimeError('Refusing unencrypted engine redirect')
            return super().redirect_request(req,fp,code,msg,headers,newurl)
    TARGET.parent.mkdir(parents=True,exist_ok=True)
    pending=TARGET.with_suffix('.pending')
    try:
        opener=urllib.request.build_opener(HTTPSOnly(),urllib.request.HTTPSHandler(context=ssl.create_default_context()))
        with opener.open(URL,timeout=30) as source,pending.open('wb') as output:
            total=0
            while data:=source.read(65536):
                total+=len(data)
                if total>8*1024*1024: raise RuntimeError('Engine exceeds expected size budget')
                output.write(data)
        if digest(pending)!=SHA256: raise RuntimeError('Engine SHA256 mismatch')
        pending.replace(TARGET)
        print('Pinned engine downloaded and SHA256 verified')
    finally:
        pending.unlink(missing_ok=True)
