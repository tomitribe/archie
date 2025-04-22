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


import com.tomitribe.snitch.annotate.AnnotateType;
import com.tomitribe.snitch.listen.StaticNoArgCallback;
import org.junit.Assert;
import org.junit.Test;
import org.tomitribe.util.Archive;
import org.tomitribe.util.Files;
import org.tomitribe.util.IO;
import org.tomitribe.util.Mvn;
import org.tomitribe.util.Zips;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;

public class JarTransformationTest {

    private final Resources resources = new Resources(JarTransformationTest.class);

    @Test
    public void copyWithNoMods() throws Exception {
        final File originalJar = Mvn.mvn("org.apache.tomcat", "tomcat-coyote", "9.0.76", "jar");

        final JarTransformation transformation = JarTransformation.builder().build();

        final Dirs.Set result = transform(originalJar, transformation);

        assertEquals(Dirs.list(result.getOriginal()), Dirs.list(result.getModified()));
    }

    @Test
    public void enhanceOne() throws Exception {
        final File originalJar = Mvn.mvn("org.apache.tomcat", "tomcat-coyote", "9.0.76", "jar");

        final JarTransformation transformation = JarTransformation.builder()
                .enhance("org/apache/coyote/AbstractProtocol.class", StaticNoArgCallback.builder()
                        .find("void org.apache.coyote.AbstractProtocol.init()")
                        .insert("void com.tomitribe.subscription.Startup.tomcat()")
                        .build())
                .build();

        final Dirs.Set result = transform(originalJar, transformation);

        final File after = new File(result.getModified(), "org/apache/coyote/AbstractProtocol.class");

        /*
         * Check the contents of the modified AbstractProtocol.class against what we expect
         */
        {
            final String expected = resources.contents("enhanceOne-expected.txt");
            final String actual = Asmifier.asmify(IO.readBytes(after));

            assertEquals(expected, actual);
        }

        /*
         * Check the directory listings of the original and modified jars
         */
        {
            /*
             * Our expectation is that the original contents will be identical
             * with the exception that AbstractProtocol no longer has a hash
             * of 14ba047acc33146 as it should have been modified.
             *
             * We expect AbstractProtocol has a new hash of fb35a9c3ebfa2fe5
             * and all other files and hashes are the same.
             */
            final String orignalHash = "14ba047acc33146";
            final String modifiedHash = "fb35a9c3ebfa2fe5";

            final String expected = Dirs.list(result.getOriginal()).replace(orignalHash, modifiedHash); // reflect the changes we expect
            final String actual = Dirs.list(result.getModified());

            assertEquals(expected, actual);
        }
    }

    @Test
    public void multipleEnhancements() throws Exception {
        final File originalJar = Mvn.mvn("org.apache.tomcat", "tomcat-coyote", "9.0.76", "jar");

        final JarTransformation transformation = JarTransformation.builder()
                .enhance("org/apache/coyote/AbstractProtocol.class", StaticNoArgCallback.builder()
                        .find("void org.apache.coyote.AbstractProtocol.init()")
                        .insert("void com.tomitribe.subscription.Startup.tomcat()")
                        .build())
                .enhance(s -> s.endsWith("AbstractProtocol.class"), AnnotateType.builder()
                        .annotation("com.tomitribe.Copyright")
                        .set("owner", "Tomitribe Corporation")
                        .set("licensee", "Unreliable, Inc.")
                        .set("build", "Apache Tomcat 9.0.76-TT.14")
                        .build())
                .build();

        final Dirs.Set result = transform(originalJar, transformation);

        final File after = new File(result.getModified(), "org/apache/coyote/AbstractProtocol.class");

        /*
         * Check the contents of the modified AbstractProtocol.class against what we expect
         */
        {
            final String expected = resources.contents("multipleEnhancements-expected.txt");
            final String actual = Asmifier.asmify(IO.readBytes(after));

            assertEquals(expected, actual);
        }

        /*
         * Check the directory listings of the original and modified jars
         */
        {
            final String orignalHash = "14ba047acc33146";
            final String modifiedHash = "439ad1d9eaad436e";

            final String expected = Dirs.list(result.getOriginal()).replace(orignalHash, modifiedHash); // reflect the changes we expect
            final String actual = Dirs.list(result.getModified());

            assertEquals(expected, actual);
        }
    }

    @Test
    public void prepend() throws Exception {
        final File originalJar = Mvn.mvn("org.apache.tomcat", "tomcat-coyote", "9.0.76", "jar");

        final JarTransformation transformation = JarTransformation.builder()
                .prepend(s -> s.equals("LICENSE") || s.endsWith("/LICENSE"),
                        "Copyright Tomitribe Corporation. 2023\n" +
                                "\n" +
                                "The source code for this program is not published or otherwise divested\n" +
                                "of its trade secrets, irrespective of what has been deposited with the\n" +
                                "U.S. Copyright Office.\n")
                .build();

        final Dirs.Set result = transform(originalJar, transformation);


        /*
         * Check the contents of the modified META-INF/LICENSE against what we expect
         */
        {
            final File modified = new File(result.getModified(), "META-INF/LICENSE");

            final String expected = resources.contents("prepend-expected.txt");
            final String actual = IO.slurp(modified);

            assertEquals(expected, actual);
        }

        /*
         * Check the directory listings of the original and modified jars
         */
        {
            /*
             * Our expectation is that the original contents will be identical
             * with the exception that META-INF/LICENSE no longer has a hash
             * of 965643f9e7a4d5ed as it should have been modified.
             *
             * We expect META-INF/LICENSE has a new hash of ebbd85fd0da36b75
             * and all other files and hashes are the same.
             */
            final String orignalHash = "965643f9e7a4d5ed";
            final String modifiedHash = "ebbd85fd0da36b75";

            final String expected = Dirs.list(result.getOriginal()).replace(orignalHash, modifiedHash); // reflect the changes we expect
            final String actual = Dirs.list(result.getModified());

            assertEquals(expected, actual);
        }
    }

    @Test
    public void testSkip() throws Exception {
        final File tmpdir = Files.tmpdir(true);

        final File sourceFile = new File(tmpdir, "source.jar");
        final Archive archive = new Archive();
        archive.add("red.txt", "this is the red file");
        archive.add("blue.txt", "this is the blue file");

        archive.toJar(sourceFile);

        final File transformedFile = new File(tmpdir, "tranformed.jar");
        JarTransformation.builder()
                .skip(entry -> entry.equals("blue.txt"))
                .build()
                .transform(new FileInputStream(sourceFile), new FileOutputStream(transformedFile));

        // check the result only has a red file
        final File exploded = new File(tmpdir, "exploded");
        Assert.assertTrue(exploded.mkdir());
        Zips.unzip(transformedFile, exploded);

        final String list = Dirs.list(exploded);
        final String[] fileEntries = list.split("\n");

        Assert.assertEquals(1, fileEntries.length);
        final String entry = fileEntries[0].replaceAll("^[\\\\/]+", "");

        Assert.assertEquals("red.txt a24a69dd71b79a2c", entry);
    }

    public static Dirs.Set transform(final File originalJar, final JarTransformation transformation) throws IOException {
        final File tmpdir = Files.tmpdir();
        final File modifiedJar = new File(tmpdir, "modified.jar");
        try (final InputStream in = IO.read(originalJar); final OutputStream out = IO.write(modifiedJar)) {
            transformation.transform(in, out);
        }

        final File originalDir = Files.mkdirs(tmpdir, "original");
        final File modifiedDir = Files.mkdirs(tmpdir, "modified");

        Zips.unzip(originalJar, originalDir);
        Zips.unzip(modifiedJar, modifiedDir);

        return new Dirs.Set(originalDir, modifiedDir);
    }

}