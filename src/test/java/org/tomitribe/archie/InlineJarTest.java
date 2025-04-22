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
import org.tomitribe.util.Files;
import org.tomitribe.util.IO;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;

public class InlineJarTest {

    private final Resources resources = new Resources(InlineJarTest.class);

    @Test
    public void test() throws Exception {

        final File primary = resources.file("primary.jar");

        final File secondary = resources.file("secondary.jar");

        final File file = new File(Files.tmpdir(), "result.jar");
        try (final InputStream in = IO.read(primary); final OutputStream out = IO.write(file)) {
            JarTransformation.builder()
                    .after(new InlineJar(secondary))
                    .build()
                    .transform(in, out);
        }


        assertEquals("com/example/Red.class  796ca5773171bbd4\n" +
                "com/example/Green.class  9044dda245a8680b\n" +
                "com/example/Blue.class  d78e86b7d63d9051\n" +
                "META-INF/LICENSE  9134608477cbc308\n" +
                "com/example/Magenta.class  ddfe4b20b0b1f275\n" +
                "com/example/Cyan.class  97a3ec28180d4904\n" +
                "com/example/Yellow.class  d9cea39e8bf2f47b\n" +
                "META-INF/LICENSE  9134608477cbc308\n", Zips.list(file));
    }

}