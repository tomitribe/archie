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

import lombok.Builder;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.tomitribe.util.IO;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Builder(builderClassName = "Builder")
public class InsertEntry implements Consumer<ArchiveOutputStream> {

    private final String name;
    private final Supplier<byte[]> bytes;

    private InsertEntry(final String name, final Supplier<byte[]> bytes) {
        this.name = name;
        this.bytes = bytes;
    }

    @Override
    public void accept(final ArchiveOutputStream archiveOutputStream) {
        try {
            final byte[] content = bytes.get();
            final ArchiveEntry archiveEntry = createArchiveEntry(archiveOutputStream, name, content);
            archiveOutputStream.putArchiveEntry(archiveEntry);
            archiveOutputStream.write(content);
            archiveOutputStream.closeArchiveEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ArchiveEntry createArchiveEntry(final ArchiveOutputStream stream, final String entryName, final byte[] content) {
        if (stream instanceof TarArchiveOutputStream) {
            final TarArchiveEntry entry = new TarArchiveEntry(entryName);
            entry.setSize(content.length);
            return entry;
        }
        if (stream instanceof JarArchiveOutputStream) {
            final JarArchiveEntry entry = new JarArchiveEntry(entryName);
            entry.setSize(content.length);
            return entry;
        }
        if (stream instanceof ZipArchiveOutputStream) {
            final ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
            entry.setSize(content.length);
            return entry;
        }
        throw new UnsupportedArchiveException(stream);
    }


    public static class Builder {

        public Builder file(final File file) {
            return bytes(() -> readBytes(file));
        }

        public Builder content(final String content) {
            return bytes(() -> content.getBytes(StandardCharsets.UTF_8));
        }

    }

    private static byte[] readBytes(final File content) {
        try {
            return IO.readBytes(content);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class UnsupportedArchiveException extends RuntimeException {
        public UnsupportedArchiveException(final ArchiveOutputStream stream) {
            super(String.format("Unsupported ArchiveOutputStream '%s'", stream.getClass().getName()));
        }
    }
}
