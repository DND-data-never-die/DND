"""Greyscale copies of PNG captures, for checking that every state reads without colour.

Usage: python3 grayscale.py OUT_DIR IN.png [IN.png ...]
Luma is ITU-R BT.601: Y = 0.299 R + 0.587 G + 0.114 B. Uses Pillow when it is installed (its "L"
conversion is the same formula), otherwise a pure-Python PNG decoder for 8-bit RGB/RGBA images.
"""
import os
import struct
import sys
import zlib


def luma_pillow(src, dst):
    from PIL import Image
    Image.open(src).convert("RGB").convert("L").save(dst, optimize=True)


def luma_pure(src, dst):
    data = open(src, "rb").read()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(src + ": not a PNG")
    pos, idat = 8, []
    while pos < len(data):
        ln, typ = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + ln]
        if typ == b"IHDR":
            w, h, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", body)
        elif typ == b"IDAT":
            idat.append(body)
        pos += 12 + ln
    if depth != 8 or ctype not in (2, 6) or interlace:
        raise ValueError(src + ": unsupported PNG layout")
    bpp = 4 if ctype == 6 else 3
    raw = zlib.decompress(b"".join(idat))
    stride = w * bpp
    prev = bytearray(stride)
    out = bytearray()
    p = 0
    for _ in range(h):
        f = raw[p]
        line = bytearray(raw[p + 1:p + 1 + stride])
        p += 1 + stride
        if f == 1:
            for i in range(bpp, stride):
                line[i] = (line[i] + line[i - bpp]) & 255
        elif f == 2:
            line = bytearray((a + b) & 255 for a, b in zip(line, prev))
        elif f == 3:
            for i in range(stride):
                left = line[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + ((left + prev[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = line[i - bpp] if i >= bpp else 0
                b = prev[i]
                c = prev[i - bpp] if i >= bpp else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else (b if pb <= pc else c))) & 255
        prev = line
        out.append(0)
        r, g, b = line[0::bpp], line[1::bpp], line[2::bpp]
        out.extend((299 * x + 587 * y + 114 * z + 500) // 1000 for x, y, z in zip(r, g, b))

    def chunk(t, b):
        return struct.pack(">I", len(b)) + t + b + struct.pack(">I", zlib.crc32(t + b) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 0, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(out), 6)) + chunk(b"IEND", b"")
    open(dst, "wb").write(png)


def main():
    out_dir, files = sys.argv[1], sys.argv[2:]
    os.makedirs(out_dir, exist_ok=True)
    try:
        import PIL  # noqa: F401
        convert, how = luma_pillow, "Pillow"
    except ImportError:
        convert, how = luma_pure, "pure Python"
    for f in files:
        convert(f, os.path.join(out_dir, os.path.basename(f)))
    print(f"{len(files)} greyscale copies in {out_dir} ({how}, ITU-R BT.601 luma)")


if __name__ == "__main__":
    main()
