/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tomitribe.archie.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.tomitribe.util.Files;
import org.tomitribe.util.IO;
import org.tomitribe.util.Longs;
import org.tomitribe.util.PrintString;
import org.tomitribe.util.hash.XxHash64;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Useful for writing test cases to inspect the results of transformations
 */
public class TarGzs {

    private TarGzs() {
    }

    public static void untargz(final File zipFile, final File destination) throws IOException {
        untargz(zipFile, destination, false);
    }

    public static void untargz(final File zipFile, final File destination, final boolean noparent) throws IOException {
        untargz(zipFile, destination, noparent, pathname -> true);
    }

    public static void untargz(final File zipFile, final File destination, final boolean noparent, final FileFilter fileFilter) throws IOException {

        Files.file(zipFile);
        Files.readable(zipFile);

        try (InputStream read = IO.read(zipFile)) {
            untargz(read, destination, noparent, fileFilter);
        }
    }

    public static void untargz(final InputStream read, final File destination, final boolean noparent, final FileFilter fileFilter) throws IOException {
        Objects.requireNonNull(fileFilter, "'fileFilter' is required.");

        Files.dir(destination);
        Files.writable(destination);

        try {
            GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(read);
            TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn);

            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
                String path = entry.getName();
                if (noparent) {
                    path = path.replaceFirst("^[^/]+/", "");
                }

                File file = new File(destination, path);
                if (!fileFilter.accept(file)) continue;

                if (entry.isDirectory()) {
                    Files.mkdirs(file);
                } else {
                    Files.mkdirs(file.getParentFile());
                    IO.copy(tarIn, file);
                    long lastModified = entry.getLastModifiedDate().getTime();
                    if (lastModified > 0L) {
                        file.setLastModified(lastModified);
                    }

//                    if (entry.getMode() != 420) System.out.printf("%s  %s%n", entry.getMode(), entry.getName());
                    // DMB: I have no idea how to respect the mod.
                    // Elasticsearch tar has entries with 33261 that are executable
                    if (33261 == entry.getMode()) {
                        file.setExecutable(true);
                    }
                    // DMB: I have no idea how to respect the mod.
                    // Kibana tar has entries with 493 that are executable
                    if (493 == entry.getMode()) {
                        file.setExecutable(true);
                    }
                }
            }

            tarIn.close();
        } catch (IOException var9) {
            throw new IOException("Unable to unzip " + read, var9);
        }
    }


    public static String list(final File zip) {
        try (final InputStream in = IO.read(zip)) {
            return list(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String list(final InputStream read) {
        final PrintString out = new PrintString();
        list(read, "", out);
        return out.toString();
    }

    public static void list(final InputStream read, final String parent, final PrintStream out) {
        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(read))) {

            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
                String path = entry.getName();

                if (entry.isDirectory()) {
                    out.printf("%s%s%n", parent, path);
                } else {
                    final byte[] bytes = IO.readBytes(tarIn);

                    if (path.endsWith(".jar") || path.endsWith(".war") ||
                            path.endsWith(".rar") || path.endsWith(".ear") ||
                            path.endsWith(".zip")) {

                        out.printf("%s%s%n", parent, path);

                        Zips.list(IO.read(bytes), parent + path + " > ", out);

                    } else if (path.endsWith(".tar.gz")) {

                        out.printf("%s%s%n", parent, path);

                        TarGzs.list(IO.read(bytes), parent + path + " > ", out);

                    } else {
                        final long hash = XxHash64.hash(IO.read(bytes));
                        final String hex = Longs.toHex(hash);
                        out.printf("%s%s  %s%n", parent, path, hex);
                    }
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException("Unable to untar " + read, e);
        }
    }
}
