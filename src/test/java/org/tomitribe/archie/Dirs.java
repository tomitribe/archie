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
import org.tomitribe.util.Files;
import org.tomitribe.util.IO;
import org.tomitribe.util.hash.XxHash64;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class Dirs {

    private Dirs() {
    }

    public static Set extract(final Set result) throws IOException {
        final File tmpdir = Files.tmpdir();
        final File original = Files.mkdirs(tmpdir, "original");
        final File modified = Files.mkdirs(tmpdir, "modified");

        Zips.unzip(result.getOriginal(), original);
        Zips.unzip(result.getModified(), modified);

        return new Set(original, modified);
    }

    public static String list(final File dir) {
        /*
         * Hash every file so we can compare contents
         */
        Function<File, String> hash = file -> {
            if (file.isDirectory()) return "";
            try (final InputStream in = IO.read(file)) {
                return Long.toHexString(XxHash64.hash(in));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        return Files.collect(dir).stream()
                .map(file -> String.format("%s %s", file.getAbsolutePath().substring(dir.getAbsolutePath().length()), hash.apply(file)))
                .reduce((s, s2) -> s + "\n" + s2)
                .orElse("");
    }

    @Data
    public static class Set {
        private final File original;
        private final File modified;

        public Set extract() throws IOException {
            return Dirs.extract(this);
        }

        public void compare() {
            final String expected = Zips.list(original);
            final String actual = Zips.list(modified);

            assertEquals(expected, actual);
        }

        public Set select(final String path) {
            return new Set(
                    new File(original, path),
                    new File(modified, path)
            );
        }
    }
}
