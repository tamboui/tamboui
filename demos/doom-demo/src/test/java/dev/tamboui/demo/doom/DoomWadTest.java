/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.doom;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DoomWadTest {

    @Test
    void loadsMapFromWad() throws Exception {
        byte[] wad = buildSimpleWad();
        Path temp = Files.createTempFile("doom-demo", ".wad");
        temp.toFile().deleteOnExit();
        Files.write(temp, wad);

        DoomDemo.WadFile wadFile = DoomDemo.WadFile.open(temp);
        DoomDemo.WadMap map = wadFile.loadMap("MAP01");
        DoomDemo.MapData grid = DoomDemo.WadRasterizer.rasterize(map, 64, "test.wad");

        assertNotNull(grid);
        assertEquals(5, grid.map().length);
        assertEquals(5, grid.map()[0].length);
        assertEquals('#', grid.map()[0][0]);
        assertEquals('.', grid.map()[2][2]);
        assertEquals(2.5, grid.startX(), 0.01);
        assertEquals(2.5, grid.startY(), 0.01);
    }

    private static byte[] buildSimpleWad() throws IOException {
        List<LumpData> lumps = new ArrayList<>();
        lumps.add(new LumpData("MAP01", new byte[0]));

        byte[] things = new byte[10];
        writeShortLE(things, 0, (short) 64);  // x
        writeShortLE(things, 2, (short) 64);  // y
        writeShortLE(things, 4, (short) 0);   // angle
        writeShortLE(things, 6, (short) 1);   // type (player 1)
        writeShortLE(things, 8, (short) 0);   // options
        lumps.add(new LumpData("THINGS", things));

        byte[] vertexes = new byte[16];
        writeShortLE(vertexes, 0, (short) 0);
        writeShortLE(vertexes, 2, (short) 0);
        writeShortLE(vertexes, 4, (short) 128);
        writeShortLE(vertexes, 6, (short) 0);
        writeShortLE(vertexes, 8, (short) 128);
        writeShortLE(vertexes, 10, (short) 128);
        writeShortLE(vertexes, 12, (short) 0);
        writeShortLE(vertexes, 14, (short) 128);
        lumps.add(new LumpData("VERTEXES", vertexes));

        byte[] linedefs = new byte[56];
        int offset = 0;
        offset = writeLine(linedefs, offset, 0, 1, 0);
        offset = writeLine(linedefs, offset, 1, 2, 0);
        offset = writeLine(linedefs, offset, 2, 3, 0);
        writeLine(linedefs, offset, 3, 0, 0);
        lumps.add(new LumpData("LINEDEFS", linedefs));

        return buildWad(lumps);
    }

    private static int writeLine(byte[] buffer, int offset, int v1, int v2, int flags) {
        writeShortLE(buffer, offset, (short) v1);
        writeShortLE(buffer, offset + 2, (short) v2);
        writeShortLE(buffer, offset + 4, (short) flags);
        writeShortLE(buffer, offset + 6, (short) 0);
        writeShortLE(buffer, offset + 8, (short) 0);
        writeShortLE(buffer, offset + 10, (short) 0);
        writeShortLE(buffer, offset + 12, (short) -1);
        return offset + 14;
    }

    private static byte[] buildWad(List<LumpData> lumps) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] header = new byte[12];
        out.write(header);

        List<DirectoryEntry> entries = new ArrayList<>();
        int offset = 12;
        for (LumpData lump : lumps) {
            entries.add(new DirectoryEntry(lump.name, offset, lump.data.length));
            out.write(lump.data);
            offset += lump.data.length;
        }

        int directoryOffset = offset;
        for (DirectoryEntry entry : entries) {
            writeIntLE(out, entry.offset);
            writeIntLE(out, entry.size);
            writeName(out, entry.name);
        }

        byte[] data = out.toByteArray();
        System.arraycopy("PWAD".getBytes(StandardCharsets.US_ASCII), 0, data, 0, 4);
        writeIntLE(data, 4, entries.size());
        writeIntLE(data, 8, directoryOffset);
        return data;
    }

    private static void writeName(ByteArrayOutputStream out, String name) throws IOException {
        byte[] bytes = new byte[8];
        byte[] nameBytes = name.getBytes(StandardCharsets.US_ASCII);
        int count = Math.min(nameBytes.length, 8);
        System.arraycopy(nameBytes, 0, bytes, 0, count);
        out.write(bytes);
    }

    private static void writeIntLE(ByteArrayOutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    private static void writeIntLE(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
        buffer[offset + 2] = (byte) ((value >> 16) & 0xFF);
        buffer[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    private static void writeShortLE(byte[] buffer, int offset, short value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    private record LumpData(String name, byte[] data) {
    }

    private record DirectoryEntry(String name, int offset, int size) {
    }
}
