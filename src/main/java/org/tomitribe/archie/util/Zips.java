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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Useful for writing test cases to inspect the results of transformations
 */
public class Zips {
    private Zips() {
        // checkstyle - noop
    }

    public static void unzip(final File zipFile, final File destination) throws IOException {
        unzip(zipFile, destination, false);
    }

    public static void unzip(final File zipFile, final File destination, final boolean noparent) throws IOException {
        unzip(zipFile, destination, noparent, pathname -> true);
    }

    public static void unzip(final File zipFile, final File destination, final boolean noparent, final FileFilter fileFilter) throws IOException {

        Files.file(zipFile);
        Files.readable(zipFile);

        try (InputStream read = IO.read(zipFile)) {
            unzip(read, destination, noparent, fileFilter);
        }
    }

    public static void unzip(final InputStream read, final File destination, final boolean noparent, final FileFilter fileFilter) throws IOException {
        Objects.requireNonNull(fileFilter, "'fileFilter' is required.");

        Files.dir(destination);
        Files.writable(destination);

        final List<ZipEntry> entries = new ArrayList<>();
        try {
            ZipInputStream e = new ZipInputStream(read);

            ZipEntry entry;
            while ((entry = e.getNextEntry()) != null) {
                entries.add(entry);

                try {
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
                        IO.copy(e, file);
                        long lastModified = entry.getTime();
                        if (lastModified > 0L) {
                            file.setLastModified(lastModified);
                        }
                    }
                } catch (IOException ex) {
                    throw new RuntimeException("Entry: " + entry.getName(), ex);
                }
            }

            e.close();
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
        try {
            ZipInputStream e = new ZipInputStream(read);

            ZipEntry entry;
            while ((entry = e.getNextEntry()) != null) {
                try {
                    String path = entry.getName();

                    if (entry.isDirectory()) {
                        out.printf("%s%s%n", parent, path);
                    } else {
                        final byte[] bytes = IO.readBytes(e);
                        if (path.endsWith(".jar") || path.endsWith(".war") ||
                                path.endsWith(".rar") || path.endsWith(".ear") ||
                                path.endsWith(".zip")) {
                            out.printf("%s%s%n", parent, path);
                            list(IO.read(bytes), parent + path + " > ", out);

                        } else if (path.endsWith(".tar.gz")) {

                            out.printf("%s%s%n", parent, path);

                            TarGzs.list(IO.read(bytes), parent + path + " > ", out);

                        } else {
                            final long hash = XxHash64.hash(IO.read(bytes));
                            final String hex = Longs.toHex(hash);
                            out.printf("%s%s  %s%n", parent, path, hex);
                        }
                    }
                } catch (IOException ex) {
                    throw new RuntimeException("Entry: " + entry.getName(), ex);
                }
            }

        } catch (final IOException e) {
            throw new UncheckedIOException("Unable to unzip " + read, e);
        }
    }


}
