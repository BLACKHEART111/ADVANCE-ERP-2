#!/usr/bin/env python3
"""Check ELF LOAD/RELRO and uncompressed ZIP offsets in an arm64 APK; never changes files."""
import argparse,json,struct,zipfile
from pathlib import Path

def inspect(apk):
    results=[]
    with zipfile.ZipFile(apk) as z,open(apk,'rb') as raw:
        for entry in z.infolist():
            if not entry.filename.startswith('lib/') or not entry.filename.endswith('.so'):continue
            data=z.read(entry)
            if data[:6]!=b'\x7fELF\x02\x01':
                results.append({'library':entry.filename,'ok':False,'error':'Expected little-endian ELF64'});continue
            offset=struct.unpack_from('<Q',data,32)[0]
            size,count=struct.unpack_from('<HH',data,54)
            headers=[struct.unpack_from('<IIQQQQQQ',data,offset+i*size) for i in range(count)]
            loads=[h for h in headers if h[0]==1]
            elf_ok=bool(loads) and all(h[7]>=16384 and (h[2]-h[3])%16384==0 for h in loads)
            relro_ok=True
            for h in headers:
                if h[0]!=0x6474e552:continue
                end=h[3]+h[6]
                # A partial RELRO page is safe only if it has no following writable data.
                if end%16384:
                    following=any(l[1]&2 and l[3]<=end<l[3]+l[6] for l in loads)
                    if following:relro_ok=False
            raw.seek(entry.header_offset+26)
            name_len,extra_len=struct.unpack('<HH',raw.read(4))
            data_offset=entry.header_offset+30+name_len+extra_len
            zip_ok=entry.compress_type!=zipfile.ZIP_STORED or data_offset%16384==0
            results.append({'library':entry.filename,'loadAlignments':[h[7] for h in loads],
                'elf16Kb':elf_ok,'relro16Kb':relro_ok,'zip16Kb':zip_ok,
                'zipDataOffset':data_offset,'ok':elf_ok and relro_ok and zip_ok})
    return {'apk':str(apk),'libraries':results,'ok':bool(results) and all(x['ok'] for x in results)}

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('apk',type=Path);parser.add_argument('--output',type=Path)
    args=parser.parse_args();result=inspect(args.apk);out=json.dumps(result,indent=2)
    if args.output:args.output.write_text(out+'\n')
    print(out);raise SystemExit(0 if result['ok'] else 1)
