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
import org.tomitribe.util.IO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.security.CodeSigner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class Transformations {

    private final List<Transformation> transformations = new ArrayList<>();
    private final List<Transformation.Action> before = new ArrayList<>();
    private final List<Transformation.Action> after = new ArrayList<>();
    private final List<Transformation.Action> beforeEntry = new ArrayList<>();
    private final List<Transformation.Action> afterEntry = new ArrayList<>();
    protected final List<Consumer<Builder>> builderConsumers = new ArrayList<>();

    private final List<Predicate<String>> skipEntry = new ArrayList<>();
    private final List<Predicate<String>> skipTransformation = new ArrayList<>();


    public Transformations(final List<Transformation> transformations,
                           final List<Transformation.Action> before,
                           final List<Transformation.Action> after,
                           final List<Transformation.Action> beforeEntry,
                           final List<Transformation.Action> afterEntry,
                           final List<Predicate<String>> skipEntry,
                           final List<Predicate<String>> skipTransformation) {
        this.transformations.addAll(transformations);
        this.beforeEntry.addAll(beforeEntry);
        this.afterEntry.addAll(afterEntry);
        this.before.addAll(before);
        this.after.addAll(after);
        this.skipEntry.addAll(skipEntry);
        this.skipTransformation.addAll(skipTransformation);
    }

    public Transformer transformer(final File file) {
        final String name = file.getName();

        if (name.endsWith(".zip")) {
            return new ZipTransformation(this);
        }

        if (name.endsWith(".tar.gz")) {
            return new TarGzTransformation(this);
        }

        if (name.endsWith(".jar")
                || name.endsWith(".ear")
                || name.endsWith(".war")
                || name.endsWith(".rar")) {

            return new JarTransformation(this);
        }

        if (name.endsWith(".pdf")) {
            return new PassThroughTransformation(this);
        }

        throw new UnsupportedFileTypeException(file);
    }

    public byte[] apply(final String entryName, byte[] contents) {
        // start by check the excluded list because it's faster
        if (skipTransformation(entryName) || isSigned(entryName, contents)) {
            return contents;
        }

        for (final Transformation transformation : transformations) {
            if (transformation.applies(entryName)) {
                contents = transformation.apply(contents);
            }
        }

        return contents;
    }

    public void afterEntry(final String entryName, final ArchiveOutputStream out) {
        for (final Transformation.Action action : afterEntry) {
            if (action.applies(entryName)) {
                action.accept(out);
            }
        }
    }

    public void beforeEntry(final String entryName, final ArchiveOutputStream out) {
        for (final Transformation.Action action : beforeEntry) {
            if (action.applies(entryName)) {
                action.accept(out);
            }
        }
    }

    public void afterArchive(final ArchiveOutputStream out) {
        for (final Transformation.Action action : after) {
            action.accept(out);
        }
    }

    public void beforeArchive(final ArchiveOutputStream out) {
        for (final Transformation.Action action : before) {
            action.accept(out);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Determines if the entry with the specified name should be skipped (i.e. not appear in the output at all)
     * @param name The name of the entry
     * @return whether the entry should be skipped
     */
    public boolean skip(final String name) {
        for (final Predicate<String> predicate : skipEntry) {
            if (predicate.test(name)) {
                return true;
            }
        }

        return false;
    }

    /**
     * As opposed to previous #skip() where we wanted to know if an entry needs to be excluded entirely from the output,
     * in this method, we want to know if an entry should not be transformed. This is typically the case for signed jars
     * like BC provider.
     *
     * @param name The name of the entry
     * @return if the transformations should not be done
     */
    protected boolean skipTransformation(final String name) {
        for (final Predicate<String> predicate : skipTransformation) {
            if (predicate.test(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The goal is to exclude all signed jars because even copying entry by entry will result in a tempered jar. We don't
     * want to spend ages to verify every single entry. This is useless for what we need. So we only check if there is
     * at least a single xxx-Digest entry in the manifest.
     *
     * @param name the name of the entry file to process
     * @param content the content of the file
     * @return true if the file is a jar and it's signed
     */
    protected boolean isSigned(final String name, final byte[] content) {
        Objects.requireNonNull(name, "name is required.");
        Objects.requireNonNull(content, "content is required.");
        if (name.endsWith(".jar")) { // don't think we care about ear, war, car, etc
            try (final JarInputStream jarInputStream = new JarInputStream(new ByteArrayInputStream(content))) {
                final Manifest manifest = jarInputStream.getManifest();
                /*
                 * avoid the stream approach with then a flatMap etc because it will process all the entries while
                 * we are only interested in finding at least one.
                 *
                 * Unfortunately this is not enough to exclude a jar. For sure if there is no digest, this is not a signed
                 * jar. But some exceptions like ECJ from Eclipse has Digest attributes but is not signed. In this case,
                 * the only way to know if it's signed is to look at the code signers. But it requires to iterate though
                 * all JarEntries, so it's time-consuming for big jars or with a lot of entries.
                 */
                if (hasDigestEntry(manifest) && hasCodeSigners(jarInputStream)) {
                    return true;
                }
            } catch (final IOException e) {
                return false; // let's assume it's ok to weave this entry
            }
        }
        return false;
    }

    private boolean hasCodeSigners(final JarInputStream jarInputStream) throws IOException {
        JarEntry entry;
        while ((entry = jarInputStream.getNextJarEntry()) != null) {
            if (entry.isDirectory()) {
                continue;
            }
            // read only this entry as it might be enough
            jarInputStream.closeEntry();
            final CodeSigner[] codeSigners = entry.getCodeSigners();
            if (codeSigners != null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDigestEntry(final Manifest manifest) {
        if (manifest == null) {
            return false;
        }
        for (Map.Entry<String, Attributes> entry : manifest.getEntries().entrySet()) {
            for (Object attrKey : entry.getValue().keySet()) {
                if (attrKey instanceof Attributes.Name && attrKey.toString().contains("-Digest"))
                    return true;
            }
        }
        return false;
    }

    public static class Builder {

        private final List<Transformation> transformations = new ArrayList<>();
        private final List<Transformation.Action> before = new ArrayList<>();
        private final List<Transformation.Action> after = new ArrayList<>();
        private final List<Transformation.Action> beforeEntry = new ArrayList<>();
        private final List<Transformation.Action> afterEntry = new ArrayList<>();

        private final List<Predicate<String>> skipEntry = new ArrayList<>();
        private final List<Predicate<String>> skipTransformation = new ArrayList<>();

        protected final List<Consumer<Builder>> builderConsumers = new ArrayList<>();

        public Builder add(final String name, final Supplier<byte[]> bytes) {
            after(InsertEntry.builder()
                    .bytes(bytes)
                    .name(name)
                    .build());

            return this;
        }

        public Builder add(final String name, final byte[] bytes) {
            return add(name, () -> bytes);
        }

        public Builder add(final String name, final String content) {

            return add(name, content::getBytes);
        }

        public Builder add(final String name, final File content) {
            return add(name, () -> readBytes(content));
        }

        public Builder enhance(final String entryName, final Function<byte[], byte[]> transformer) {
            transformations.add(new Transformation(new Equals(entryName), transformer));
            return this;
        }

        public Builder enhance(final Predicate<String> entryPredicate, final Function<byte[], byte[]> transformer) {
            transformations.add(new Transformation(entryPredicate, transformer));
            return this;
        }

        public Builder prepend(final String entryName, final String contents) {
            transformations.add(new Transformation(new Equals(entryName), new Prepend(contents)));
            return this;
        }

        public Builder prepend(final Predicate<String> entryPredicate, final String contents) {
            transformations.add(new Transformation(entryPredicate, new Prepend(contents)));
            return this;
        }

        public Builder before(final Consumer<ArchiveOutputStream> consumer) {
            this.before.add(new Transformation.Action(s -> true, consumer));
            return this;
        }

        public Builder after(final Consumer<ArchiveOutputStream> consumer) {
            this.after.add(new Transformation.Action(s -> true, consumer));
            return this;
        }

        public Builder beforeEntry(final String entryName, final Consumer<ArchiveOutputStream> consumer) {
            return beforeEntry(new Equals(entryName), consumer);
        }

        public Builder beforeEntry(final Predicate<String> entryName, final Consumer<ArchiveOutputStream> consumer) {
            beforeEntry.add(new Transformation.Action(entryName, consumer));
            return this;
        }

        public Builder afterEntry(final String entryName, final Consumer<ArchiveOutputStream> consumer) {
            return afterEntry(new Equals(entryName), consumer);
        }

        public Builder afterEntry(final Predicate<String> entryName, final Consumer<ArchiveOutputStream> consumer) {
            afterEntry.add(new Transformation.Action(entryName, consumer));
            return this;
        }

        public Builder skip(final Predicate<String> predicate) {
            skipEntry.add(predicate);
            return this;
        }

        public Builder skipTransformation(final Predicate<String> predicate) {
            skipTransformation.add(predicate);
            return this;
        }


        /**
         * TODO Use this to create a class that adds the subscription jar
         * by watching each predicate tested value and can figure out the
         * archive root so we know where to add the subscription jar
         */
        public Builder and(final Consumer<Builder> consumer) {
            this.builderConsumers.add(consumer);
            return this;
        }


        private void applyBuilderConsumers() {
            /*
             * Small optimization
             */
            if (builderConsumers.size() == 0) return;

            /*
             * More consumers may be added during processing.
             *
             * To both avoid a ConcurrentModificationException and to ensure
             * we process those new consumers, we copy the list then empty it.
             *
             * If new consumers are added they will be the only ones in builderConsumers
             */
            final List<Consumer<Builder>> invoked = new ArrayList<>();

            while (true) {

                final ArrayList<Consumer<Builder>> consumers = new ArrayList<>(builderConsumers);
                builderConsumers.clear();

                for (final Consumer<Builder> consumer : consumers) {
                    consumer.accept(this);
                    invoked.add(consumer);
                }

                /*
                 * If the list is not empty, then a consumer added another
                 * consumer, and we need to loop again and process them
                 */
                if (builderConsumers.size() == 0) break;
            }

            builderConsumers.addAll(invoked);
        }

        public Transformations build() {
            applyBuilderConsumers();
            return new Transformations(transformations, before, after, beforeEntry, afterEntry, skipEntry, skipTransformation);
        }

        public static byte[] readBytes(final File content) {
            try {
                return IO.readBytes(content);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        public static byte[] readBytes(final URL content) {
            try {
                return IO.readBytes(content);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

    }

    @Data
    public static class Prepend implements Function<byte[], byte[]> {
        private final String contents;

        @Override
        public byte[] apply(final byte[] bytes) {
            try {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                out.write(contents.getBytes());
                out.write(bytes);
                return out.toByteArray();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Data
    public static class Append implements Function<byte[], byte[]> {
        private final String contents;

        @Override
        public byte[] apply(final byte[] bytes) {
            try {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                out.write(bytes);
                out.write(contents.getBytes());
                return out.toByteArray();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Data
    public static class Equals implements Predicate<String> {
        private final String expected;

        @Override
        public boolean test(final String s) {
            return expected.equals(s);
        }
    }

    public static class UnsupportedFileTypeException extends RuntimeException {
        public UnsupportedFileTypeException(final File file) {
            super(String.format("Unsupported file type '%s'. Supported types are zip, tar.gz, jar, war, ear and rar", file.getName()));
        }
    }

}
