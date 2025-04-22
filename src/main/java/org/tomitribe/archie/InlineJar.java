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

import lombok.Data;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.tomitribe.util.IO;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Data
public class InlineJar implements Consumer<ArchiveOutputStream> {

    private final Supplier<byte[]> bytes;

    public InlineJar(final Supplier<byte[]> bytes) {
        this.bytes = bytes;
    }

    public InlineJar(final byte[] bytes) {
        this(() -> bytes);
    }

    public InlineJar(final File file) {
        this(() -> readBytes(file));
    }

    private static byte[] readBytes(final File content) {
        try {
            return IO.readBytes(content);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void accept(final ArchiveOutputStream out) {
        try (final JarArchiveInputStream in = new JarArchiveInputStream(IO.read(bytes.get()))) {

            JarArchiveEntry entry;
            while ((entry = in.getNextJarEntry()) != null) {

                final String name = entry.getName();

                if (entry.isDirectory()) {
                    final JarArchiveEntry dir = new JarArchiveEntry(name);
                    out.putArchiveEntry(dir);
                    out.closeArchiveEntry();
                } else {

                    final byte[] bytes = IO.readBytes(in);

                    final JarArchiveEntry file = new JarArchiveEntry(name);
                    file.setMethod(entry.getMethod());
                    file.setSize(bytes.length);
                    file.setTime(entry.getTime());
                    file.setUnixMode(entry.getUnixMode());
                    if (entry.getCreationTime() != null) file.setCreationTime(entry.getCreationTime());
                    if (entry.getLastModifiedTime() != null) file.setLastModifiedTime(entry.getLastModifiedTime());
                    if (entry.getLastAccessTime() != null) file.setLastAccessTime(entry.getLastAccessTime());

                    out.putArchiveEntry(file);
                    out.write(bytes);
                    out.closeArchiveEntry();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
