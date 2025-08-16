# SM64 Corruptor GUI (Java, Maven)

A small Swing GUI that corrupts Super Mario 64 (N64) ROMs and can launch your emulator right after.
It locks sensitive regions by default so the ROM is likely to boot, and it can target only the
*extended* area (>= 0x800000) used by many hacks.

## Features
- Modes: FLIP, RANDOM, ADD, SUB, XOR
- Rate-based (probability per byte) or step-based (mutate 1 per N bytes)
- Lock header/boot/checksum fields by default
- Only-extended toggle (>= 0x800000)
- Skip specific hex ranges (inclusive): `00100000-0010FFFF,00200000-00200FFF`
- Emulator launcher with `{rom}` placeholder in args

## Build
```bash
mvn -q package
```
Produces a runnable JAR:
```
target/sm64-corruptor-gui-1.0.0-shaded.jar
```

## Run
```bash
java -jar target/sm64-corruptor-gui-1.0.0-shaded.jar
```

## Usage notes
- This tool **does not** recalc the N64 header CRC. Many emulators ignore it if
  the boot area is intact (which we keep locked by default). If you need CRC repair,
  add it later as a post-pass.
- Supported ROM extensions in the file chooser: `.z64`, `.n64`, `.v64`.
- Use your own legally obtained ROMs.
