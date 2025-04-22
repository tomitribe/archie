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
package org.tomitribe.archie;


import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.junit.Test;
import org.tomitribe.archie.util.Zips;
import org.tomitribe.util.Files;
import org.tomitribe.util.IO;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

/**
 * The Goal of this test is to verify the before, beforeEntry, afterEntry, and after
 * callbacks of Transformations are executed by ZipTransformation.
 */
public class ZipTransformationCallbacksTest {

    private File zip = new Resources().file("archive.zip");

    /**
     * Test transformations that need to happen at the start of a jar
     */
    @Test
    public void before() throws Exception {

        final Transformations transformations = Transformations.builder()
                .before(archiveOutputStream -> {
                    try {
                        final byte[] bytes = "one".getBytes();

                        final ZipArchiveEntry entry = new ZipArchiveEntry("uno.txt");
                        entry.setTime(System.currentTimeMillis());
                        entry.setSize(bytes.length);

                        archiveOutputStream.putArchiveEntry(entry);
                        archiveOutputStream.write(bytes);
                        archiveOutputStream.closeArchiveEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .before(archiveOutputStream -> {
                    try {
                        final byte[] bytes = "two".getBytes();

                        final ZipArchiveEntry entry = new ZipArchiveEntry("dos.txt");
                        entry.setTime(System.currentTimeMillis());
                        entry.setSize(bytes.length);

                        archiveOutputStream.putArchiveEntry(entry);
                        archiveOutputStream.write(bytes);
                        archiveOutputStream.closeArchiveEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .build();

        final ZipTransformation zipTransformation = new ZipTransformation(transformations);
        final Dirs.Set result = transform(zip, zipTransformation);

        assertEquals("uno.txt  363b02a42408a1f6\n" +
                "dos.txt  c3d9ab4fecf4448b\n" +
                "LICENSE  9134608477cbc308\n" +
                "archive.jar\n" +
                "archive.jar > META-INF/\n" +
                "archive.jar > META-INF/MANIFEST.MF  b44f84c21b8a59ce\n" +
                "archive.jar > META-INF/LICENSE  9134608477cbc308\n" +
                "archive.jar > com/\n" +
                "archive.jar > com/example/\n" +
                "archive.jar > com/example/Blue.class  eede7ddf0e8da147\n" +
                "archive.jar > com/example/Red.class  5274fcc41680dae1\n" +
                "archive.jar > com/example/Green.class  5d17a647bf5bc8c0\n" +
                "com/\n" +
                "com/example/\n" +
                "com/example/Blue.class  eede7ddf0e8da147\n" +
                "com/example/Red.class  5274fcc41680dae1\n" +
                "com/example/Green.class  5d17a647bf5bc8c0\n", Zips.list(result.getModified()));
    }

    /**
     * Test transformations that need to happen at the end of a jar
     */
    @Test
    public void after() throws Exception {

        final Transformations transformations = Transformations.builder()
                .after(archiveOutputStream -> {
                    try {
                        final byte[] bytes = "one".getBytes();

                        final ZipArchiveEntry entry = new ZipArchiveEntry("uno.txt");
                        entry.setTime(System.currentTimeMillis());
                        entry.setSize(bytes.length);

                        archiveOutputStream.putArchiveEntry(entry);
                        archiveOutputStream.write(bytes);
                        archiveOutputStream.closeArchiveEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .after(archiveOutputStream -> {
                    try {
                        final byte[] bytes = "two".getBytes();

                        final ZipArchiveEntry entry = new ZipArchiveEntry("dos.txt");
                        entry.setTime(System.currentTimeMillis());
                        entry.setSize(bytes.length);

                        archiveOutputStream.putArchiveEntry(entry);
                        archiveOutputStream.write(bytes);
                        archiveOutputStream.closeArchiveEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .build();

        final ZipTransformation zipTransformation = new ZipTransformation(transformations);
        final Dirs.Set result = transform(zip, zipTransformation);

        assertEquals("LICENSE  9134608477cbc308\n" +
                "archive.jar\n" +
                "archive.jar > META-INF/\n" +
                "archive.jar > META-INF/MANIFEST.MF  b44f84c21b8a59ce\n" +
                "archive.jar > META-INF/LICENSE  9134608477cbc308\n" +
                "archive.jar > com/\n" +
                "archive.jar > com/example/\n" +
                "archive.jar > com/example/Blue.class  eede7ddf0e8da147\n" +
                "archive.jar > com/example/Red.class  5274fcc41680dae1\n" +
                "archive.jar > com/example/Green.class  5d17a647bf5bc8c0\n" +
                "com/\n" +
                "com/example/\n" +
                "com/example/Blue.class  eede7ddf0e8da147\n" +
                "com/example/Red.class  5274fcc41680dae1\n" +
                "com/example/Green.class  5d17a647bf5bc8c0\n" +
                "uno.txt  363b02a42408a1f6\n" +
                "dos.txt  c3d9ab4fecf4448b\n", Zips.list(result.getModified()));
    }

    /**
     * Test transformations that need to happen at the start of a jar
     */
    @Test
    public void beforeEntry() throws Exception {

        final Transformations transformations = Transformations.builder()
                .beforeEntry(s -> s.endsWith("Red.class"), archiveOutputStream -> {
                    try {
                        final byte[] bytes = "red".getBytes();

                        final ZipArchiveEntry entry = new ZipArchiveEntry("BeforeRed.txt");
                        entry.setTime(System.currentTimeMillis());
                        entry.setSize(bytes.length);

                        archiveOutputStream.putArchiveEntry(entry);
                        archiveOutputStream.write(bytes);
                        archiveOutputStream.closeArchiveEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .build();

        final ZipTransformation zipTransformation = new ZipTransformation(transformations);
        final Dirs.Set result = transform(zip, zipTransformation);

        assertEquals("LICENSE  9134608477cbc308\n" +
                "archive.jar\n" +
                "archive.jar > META-INF/\n" +
                "archive.jar > META-INF/MANIFEST.MF  b44f84c21b8a59ce\n" +
                "archive.jar > META-INF/LICENSE  9134608477cbc308\n" +
                "archive.jar > com/\n" +
                "archive.jar > com/example/\n" +
                "archive.jar > com/example/Blue.class  eede7ddf0e8da147\n" +
                "archive.jar > com/example/Red.class  5274fcc41680dae1\n" +
                "archive.jar > com/example/Green.class  5d17a647bf5bc8c0\n" +
                "com/\n" +
                "com/example/\n" +
                "com/example/Blue.class  eede7ddf0e8da147\n" +
                "BeforeRed.txt  d1d784bb12e4656a\n" +
                "com/example/Red.class  5274fcc41680dae1\n" +
                "com/example/Green.class  5d17a647bf5bc8c0\n", Zips.list(result.getModified()));
    }

    /**
     * Test transformations that need to happen at the start of a jar
     */
    @Test
    public void afterEntry() throws Exception {

        final Transformations transformations = Transformations.builder()
                .afterEntry(s -> s.endsWith("Red.class"), archiveOutputStream -> {
                    try {
                        final byte[] bytes = "red".getBytes();

                        final ZipArchiveEntry entry = new ZipArchiveEntry("AfterRed.txt");
                        entry.setTime(System.currentTimeMillis());
                        entry.setSize(bytes.length);

                        archiveOutputStream.putArchiveEntry(entry);
                        archiveOutputStream.write(bytes);
                        archiveOutputStream.closeArchiveEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .build();

        final ZipTransformation zipTransformation = new ZipTransformation(transformations);
        final Dirs.Set result = transform(zip, zipTransformation);

        assertEquals("LICENSE  9134608477cbc308\n" +
                "archive.jar\n" +
                "archive.jar > META-INF/\n" +
                "archive.jar > META-INF/MANIFEST.MF  b44f84c21b8a59ce\n" +
                "archive.jar > META-INF/LICENSE  9134608477cbc308\n" +
                "archive.jar > com/\n" +
                "archive.jar > com/example/\n" +
                "archive.jar > com/example/Blue.class  eede7ddf0e8da147\n" +
                "archive.jar > com/example/Red.class  5274fcc41680dae1\n" +
                "archive.jar > com/example/Green.class  5d17a647bf5bc8c0\n" +
                "com/\n" +
                "com/example/\n" +
                "com/example/Blue.class  eede7ddf0e8da147\n" +
                "com/example/Red.class  5274fcc41680dae1\n" +
                "AfterRed.txt  d1d784bb12e4656a\n" +
                "com/example/Green.class  5d17a647bf5bc8c0\n", Zips.list(result.getModified()));
    }

    /**
     * Test transformations that need to happen at the start of a jar
     */
    @Test
    public void and() throws Exception {

        final Consumer<Transformations.Builder> consumer = builder -> {
            builder.afterEntry(s -> s.endsWith("Red.class"), archiveOutputStream -> {
                try {
                    final byte[] bytes = "red".getBytes();

                    final ZipArchiveEntry entry = new ZipArchiveEntry("AfterRed.txt");
                    entry.setTime(System.currentTimeMillis());
                    entry.setSize(bytes.length);

                    archiveOutputStream.putArchiveEntry(entry);
                    archiveOutputStream.write(bytes);
                    archiveOutputStream.closeArchiveEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        };

        final Transformations transformations = Transformations.builder()
                .and(consumer)
                .build();

        final ZipTransformation zipTransformation = new ZipTransformation(transformations);
        final Dirs.Set result = transform(zip, zipTransformation);

        assertEquals("LICENSE  9134608477cbc308\n" +
                "archive.jar\n" +
                "archive.jar > META-INF/\n" +
                "archive.jar > META-INF/MANIFEST.MF  b44f84c21b8a59ce\n" +
                "archive.jar > META-INF/LICENSE  9134608477cbc308\n" +
                "archive.jar > com/\n" +
                "archive.jar > com/example/\n" +
                "archive.jar > com/example/Blue.class  eede7ddf0e8da147\n" +
                "archive.jar > com/example/Red.class  5274fcc41680dae1\n" +
                "archive.jar > com/example/Green.class  5d17a647bf5bc8c0\n" +
                "com/\n" +
                "com/example/\n" +
                "com/example/Blue.class  eede7ddf0e8da147\n" +
                "com/example/Red.class  5274fcc41680dae1\n" +
                "AfterRed.txt  d1d784bb12e4656a\n" +
                "com/example/Green.class  5d17a647bf5bc8c0\n", Zips.list(result.getModified()));
    }

    public static Dirs.Set transform(final File originalJar, final ZipTransformation transformation) throws IOException {
        final File tmpdir = Files.tmpdir();
        final File modifiedJar = new File(tmpdir, "modified.jar");
        try (final InputStream in = IO.read(originalJar); final OutputStream out = IO.write(modifiedJar)) {
            transformation.transform(in, out);
        }

        return new Dirs.Set(originalJar, modifiedJar);
    }

}