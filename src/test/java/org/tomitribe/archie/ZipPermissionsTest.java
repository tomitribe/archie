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

import org.junit.Ignore;
import org.junit.Test;
import org.tomitribe.util.Files;
import org.tomitribe.util.Pipe;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class ZipPermissionsTest {
    private final Resources resources = new Resources();

    @Ignore
    @Test
    public void test() throws Exception {

        final File tmpdir = Files.tmpdir();

        final File unzip = new File("/usr/bin/unzip");

        final File before = resources.file("archive.zip");
        final File after = new File(tmpdir, before.getName());

        Transformations.builder()
                .before(InsertEntry.builder().name("README.txt").content("Hello, World!").build())
                .build()
                .transformer(before)
                .transform(before, after);

        final ProcessBuilder builder = new ProcessBuilder();
        builder.command().add(unzip.getAbsolutePath());
        builder.command().add(after.getAbsolutePath());
        builder.directory(tmpdir);
        final Process process = builder.start();
        final Future<List<Pipe>> future = Pipe.pipe(process);
        future.get();

        final List<File> files = Files.collect(tmpdir);
        assertEquals("/archive.jar\n" +
                "/LICENSE\n" +
                "/README.txt\n" +
                "/archive.zip\n" +
                "/com\n" +
                "/com/example\n" +
                "/com/example/Blue.class\n" +
                "/com/example/Red.class\n" +
                "/com/example/Green.class", join("\n", file -> file.getAbsolutePath().substring(tmpdir.getAbsolutePath().length()), files));

        assertPermissions("rw-", files.get(0));
    }

    private void assertPermissions(final String expected, final File file) {
        final StringBuilder sb = new StringBuilder();
        sb.append(file.canRead() ? "r" : "-");
        sb.append(file.canWrite() ? "w" : "-");
        sb.append(file.canExecute() ? "x" : "-");
        assertEquals(file.getName(), expected, sb.toString());
    }

    public static <T> String join(final String delimiter, final Function<T, String> nameCallback, final Collection<T> collection) {

        final StringBuilder sb = new StringBuilder();

        for (final T obj : collection) {
            sb.append(nameCallback.apply(obj)).append(delimiter);
        }

        return sb.substring(0, sb.length() - delimiter.length());
    }

}
