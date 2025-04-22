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

import com.example.Blue;
import com.example.Green;
import com.example.Red;
import com.tomitribe.snitch.listen.StaticNoArgCallback;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.Test;
import org.tomitribe.util.Archive;
import org.tomitribe.util.Files;
import org.tomitribe.util.IO;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class ZipTransformationTest {

    @Test
    public void test() throws Exception {
        final Archive lib = Archive.archive()
                .add(Red.class)
                .add(Green.class)
                .add(Blue.class)
                .add("META-INF/LICENSE", "Apache License v2.0");

        final File originalFile = Archive.archive()
                .add("lib/one.jar", lib.toJar())
                .add("lib/two.jar", lib.toJar())
                .toJar();


        final File modifiedFile = new File(Files.tmpdir(), "modified.jar");

        final Dirs.Set set = new Dirs.Set(originalFile, new Binary(modifiedFile));

        transform(set);

        set.compare();
    }

    private static void transform(final Dirs.Set set) throws IOException {
        transform(set.getOriginal(), set.getModified());
    }

    private static void transform(final File originalFile, final File modifiedFile) throws IOException {
        final JarTransformation transformation = JarTransformation.builder()
                .enhance("org/apache/coyote/AbstractProtocol.class", StaticNoArgCallback.builder()
                        .find("void org.apache.coyote.AbstractProtocol.init()")
                        .insert("void com.tomitribe.subscription.Startup.tomcat()")
                        .build())
                .build();

        try (final ZipArchiveInputStream in = new ZipArchiveInputStream(IO.read(originalFile))) {
            try (final ZipArchiveOutputStream out = new ZipArchiveOutputStream(IO.write(modifiedFile))) {

                ZipArchiveEntry entry;
                while ((entry = in.getNextZipEntry()) != null) {

                    final String name = entry.getName();

                    if (entry.isDirectory()) {
                        final ZipArchiveEntry dir = new ZipArchiveEntry(name);
                        out.putArchiveEntry(dir);
                        out.closeArchiveEntry();
                    } else {

                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        if (name.endsWith(".jar")) {
                            transformation.transform(in, baos);
                        } else {
                            IO.copy(in, baos);
                        }

                        final byte[] bytes = baos.toByteArray();

                        final JarArchiveEntry file = new JarArchiveEntry(name);
                        file.setMethod(entry.getMethod());
                        file.setSize(bytes.length);
                        file.setTime(entry.getTime());
                        if (entry.getCreationTime() != null) file.setCreationTime(entry.getCreationTime());
                        if (entry.getLastModifiedTime() != null) file.setLastModifiedTime(entry.getLastModifiedTime());
                        if (entry.getLastAccessTime() != null) file.setLastAccessTime(entry.getLastAccessTime());

                        out.putArchiveEntry(file);
                        out.write(bytes);
                        out.closeArchiveEntry();
                    }
                }
            }
        }
    }
}
