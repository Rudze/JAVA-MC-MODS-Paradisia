package com.rudze.api;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Zips the web folder of the squaremap mod (page, scripts and map tiles), so the website's
 * GitHub Actions job can publish the map next to the site.
 */
final class MapArchive {

    private MapArchive() {
    }

    static boolean exists(Path folder) {
        return Files.isDirectory(folder);
    }

    /** Streams the zip. Images are stored as they are: PNG files don't shrink when compressed again. */
    static void write(Path folder, OutputStream out) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(folder)) {
            files = walk.filter(Files::isRegularFile).toList();
        }
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Path file : files) {
                byte[] bytes;
                try {
                    bytes = Files.readAllBytes(file);
                } catch (IOException e) {
                    continue; // a tile squaremap is rewriting right now, it will be in the next archive
                }
                ZipEntry entry = new ZipEntry(folder.relativize(file).toString().replace('\\', '/'));
                if (file.toString().endsWith(".png")) {
                    CRC32 crc = new CRC32();
                    crc.update(bytes);
                    entry.setMethod(ZipEntry.STORED);
                    entry.setSize(bytes.length);
                    entry.setCrc(crc.getValue());
                }
                zip.putNextEntry(entry);
                zip.write(bytes);
                zip.closeEntry();
            }
        }
    }
}
