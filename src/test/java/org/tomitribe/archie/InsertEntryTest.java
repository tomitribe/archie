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

import org.junit.Test;
import org.tomitribe.archie.util.TarGzs;
import org.tomitribe.util.Files;
import org.tomitribe.util.IO;
import org.tomitribe.util.Zips;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class InsertEntryTest {

    private final Resources resources = new Resources();

    @Test
    public void tarGz() throws Exception {

        final File tmpdir = Files.tmpdir();

        final File before = resources.file("archive.tar.gz");
        final File after = new File(tmpdir, before.getName());

        Transformations.builder()
                .before(InsertEntry.builder().name("README.txt").content("Hello, World!").build())
                .build()
                .transformer(before)
                .transform(before, after);

        TarGzs.untargz(after, tmpdir);

        final String actual = IO.slurp(new File(tmpdir, "README.txt"));
        assertEquals("Hello, World!", actual);
    }

    @Test
    public void zip() throws Exception {

        final File tmpdir = Files.tmpdir();

        final File before = resources.file("archive.zip");
        final File after = new File(tmpdir, before.getName());

        Transformations.builder()
                .before(InsertEntry.builder().name("README.txt").content("Hello, World!").build())
                .build()
                .transformer(before)
                .transform(before, after);

        Zips.unzip(after, tmpdir);

        final String actual = IO.slurp(new File(tmpdir, "README.txt"));
        assertEquals("Hello, World!", actual);
    }

    @Test
    public void jar() throws Exception {

        final File tmpdir = Files.tmpdir();

        final File before = resources.file("archive.jar");
        final File after = new File(tmpdir, before.getName());

        Transformations.builder()
                .before(InsertEntry.builder().name("README.txt").content("Hello, World!").build())
                .build()
                .transformer(before)
                .transform(before, after);

        Zips.unzip(after, tmpdir);

        final String actual = IO.slurp(new File(tmpdir, "README.txt"));
        assertEquals("Hello, World!", actual);
    }
}