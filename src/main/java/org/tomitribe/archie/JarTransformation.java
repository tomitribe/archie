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
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.tomitribe.util.IO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.CRC32;

public class JarTransformation implements Transformer, Function<byte[], byte[]> {

    private final Transformations transformations;

    public JarTransformation(final Transformations transformations) {
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
        /*
         * We need to ensure the JarArchiveOutputStream is closed or the resulting jar file will not
         * have the expected metadata at the end and you will see errors like this from `jar tvf`
         *
         * $ jar tvf ../lib/annotations-api.jar
         * java.util.zip.ZipException: zip END header not found
         *
         * However, the input and output streams passed in are typically from an enclosing JarArchiveOutputStream
         * and JarArchiveInputStream and need to stay open.
         */
        try (final JarArchiveInputStream in = new JarArchiveInputStream(new UnclosableInputStream(source));
             final JarArchiveOutputStream out = new JarArchiveOutputStream(new UnclosableOutputStream(destination))) {

            transformations.beforeArchive(out);

            JarArchiveEntry entry;
            while ((entry = in.getNextJarEntry()) != null) {

                final String name = entry.getName();
                if (shouldSkip(name)) continue;

                transformations.beforeEntry(name, out);

                if (entry.isDirectory()) {
                    final JarArchiveEntry dir = new JarArchiveEntry(name);
                    out.putArchiveEntry(dir);
                    out.closeArchiveEntry();
                } else {

                    final byte[] bytes = transformations.apply(name, IO.readBytes(in));

                    final JarArchiveEntry file = new JarArchiveEntry(name);
                    file.setMethod(entry.getMethod());
                    file.setSize(bytes.length);
                    file.setTime(entry.getTime());
                    file.setUnixMode(entry.getUnixMode());
                    if (entry.getCreationTime() != null) file.setCreationTime(entry.getCreationTime());
                    if (entry.getLastModifiedTime() != null) file.setLastModifiedTime(entry.getLastModifiedTime());
                    if (entry.getLastAccessTime() != null) file.setLastAccessTime(entry.getLastAccessTime());

                    final CRC32 crc = new CRC32();
                    crc.update(bytes);
                    file.setCrc(crc.getValue());

                    out.putArchiveEntry(file);

                    out.write(bytes);
                    out.closeArchiveEntry();
                }

                transformations.afterEntry(name, out);
            }

            transformations.afterArchive(out);
        }
    }

    private boolean shouldSkip(final String name) {
        return transformations.skip(name);
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

        public JarTransformation build() {
            return new JarTransformation(builder.build());
        }

        public Builder skip(final Predicate<String> predicate) {
            builder.skip(predicate);
            return this;
        }
    }
}
