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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.Assert;
import org.junit.Test;
import org.tomitribe.util.IO;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TarGzTransformationTest {

    @Test
    public void testShouldNotDestroySymLinks() throws Exception {
        final TarGzTransformation transformation = TarGzTransformation.builder().build();
        final byte[] bytes = IO.readBytes(Objects.requireNonNull(getClass().getResourceAsStream("/sample.tar.gz"))); // this sample contains a symlink
        final byte[] converted = transformation.apply(bytes);

        final Map<String, String> links = new HashMap<>();

        try (final TarArchiveInputStream in = new TarArchiveInputStream(new GzipCompressorInputStream(new ByteArrayInputStream(converted)))) {
            TarArchiveEntry entry;
            while ((entry = in.getNextTarEntry()) != null) {
                if (entry.isSymbolicLink()) {
                    links.put(entry.getName(), entry.getLinkName());
                }
            }
        }

        Assert.assertEquals(1, links.size());
        Assert.assertNotNull(links.get("sample/hello.txt"));
        Assert.assertEquals("hello.txt.1", links.get("sample/hello.txt"));
    }
}