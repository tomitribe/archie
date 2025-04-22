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

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.tomitribe.util.Files;
import org.tomitribe.util.IO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.CRC32;

public class ZipTransformation implements Transformer, Function<byte[], byte[]> {

    private final Transformations transformations;

    public ZipTransformation(final Transformations transformations) {
        this.transformations = transformations;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public byte[] apply(final byte[] bytes) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            transform(in, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    @Override
    public void transform(final InputStream source, final OutputStream destination) throws IOException {
        try (final ZipArchiveInputStream in = new ZipArchiveInputStream(new UnclosableInputStream(source));
             final ZipArchiveOutputStream out = new ZipArchiveOutputStream(new UnclosableOutputStream(destination))) {

            transformations.beforeArchive(out);

            final File tmpdir = Files.tmpdir();

            ZipArchiveEntry entry;
            while ((entry = in.getNextZipEntry()) != null) {

                final String name = entry.getName();

                transformations.beforeEntry(name, out);

                if (entry.isDirectory()) {
                    final ZipArchiveEntry dir = new ZipArchiveEntry(name);
                    out.putArchiveEntry(dir);
                    out.closeArchiveEntry();
                } else {

                    final byte[] bytes = transformations.apply(name, IO.readBytes(in));

                    try {
                        final ZipArchiveEntry file = new ZipArchiveEntry(name);
                        file.setMethod(entry.getMethod());
                        file.setSize(bytes.length);
                        file.setTime(entry.getTime());

                        if (entry.getCreationTime() != null) file.setCreationTime(entry.getCreationTime());
                        if (entry.getLastModifiedTime() != null) file.setLastModifiedTime(entry.getLastModifiedTime());
                        if (entry.getLastAccessTime() != null) file.setLastAccessTime(entry.getLastAccessTime());

                        final CRC32 crc32 = new CRC32();
                        crc32.update(bytes);
                        file.setCrc(crc32.getValue());
                        file.setExtra(new byte[0]);
                        out.putArchiveEntry(file);
                        out.write(bytes);
                        out.closeArchiveEntry();
                    } catch (IOException e) {
                        throw new IOException("Failed to create entry: " + name, e);
                    }
                }

                transformations.afterEntry(name, out);
            }

            transformations.afterArchive(out);
        }
    }

    public static class Builder {

        private Transformations.Builder builder = new Transformations.Builder();

        public Builder enhance(final String entryName, final Function<byte[], byte[]> transformer) {
            builder.enhance(entryName, transformer);
            return this;
        }

        public Builder enhance(final Predicate<String> entryPredicate, final Function<byte[], byte[]> transformer) {
            builder.enhance(entryPredicate, transformer);
            return this;
        }

        public Builder prepend(final String entryName, final String contents) {
            builder.prepend(entryName, contents);
            return this;
        }

        public Builder prepend(final Predicate<String> entryPredicate, final String contents) {
            builder.prepend(entryPredicate, contents);
            return this;
        }

        public Builder before(final Consumer<ArchiveOutputStream> consumer) {
            builder.before(consumer);
            return this;
        }

        public Builder after(final Consumer<ArchiveOutputStream> consumer) {
            builder.after(consumer);
            return this;
        }

        public Builder beforeEntry(final String entryName, final Consumer<ArchiveOutputStream> consumer) {
            builder.beforeEntry(entryName, consumer);
            return this;
        }

        public Builder beforeEntry(final Predicate<String> entryName, final Consumer<ArchiveOutputStream> consumer) {
            builder.beforeEntry(entryName, consumer);
            return this;
        }

        public Builder afterEntry(final String entryName, final Consumer<ArchiveOutputStream> consumer) {
            builder.afterEntry(entryName, consumer);
            return this;
        }

        public Builder afterEntry(final Predicate<String> entryName, final Consumer<ArchiveOutputStream> consumer) {
            builder.afterEntry(entryName, consumer);
            return this;
        }

        public Builder and(final Consumer<Transformations.Builder> consumer) {
            builder.and(consumer);
            return this;
        }

        public ZipTransformation build() {
            return new ZipTransformation(builder.build());
        }
    }
}
