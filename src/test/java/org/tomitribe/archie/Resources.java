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

import org.tomitribe.util.Files;
import org.tomitribe.util.IO;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;

public class Resources {

    private final String prefix;

    public Resources(final Class<?> testClass) {
        this(testClass.getSimpleName());
    }

    public Resources(final String prefix) {
        this.prefix = prefix;
    }

    public Resources() {
        this.prefix = null;
    }

    private static ClassLoader loader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public byte[] bytes(final String name) {
        try {
            return IO.readBytes(read(name));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String contents(final String name) {
        return new String(bytes(name));
    }

    public File file(final String name) {
        try {
            final File tmpdir = Files.tmpdir();
            final File file = new File(tmpdir, new File(name).getName());
            IO.copy(read(name), file);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public InputStream read(final String name) {
        try {
            final String path = prefix == null ? name : prefix + "/" + name;

            final URL resource = loader().getResource(path);
            if (resource == null) {
                throw new ResourceNotFoundException(path);
            }
            Objects.requireNonNull(resource);
            return IO.read(resource);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(final String path) {
            super(String.format("Resource not found: '%s'", path));
        }
    }
}
