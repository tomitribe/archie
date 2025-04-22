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

import org.tomitribe.util.IO;
import org.tomitribe.util.Join;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import static org.tomitribe.archie.Digest.MD5;
import static org.tomitribe.archie.Digest.SHA1;
import static org.tomitribe.archie.Digest.SHA256;


public class Binary extends File {

    private final File md5;
    private final File sha1;
    private final File sha256;

    public Binary(final File parent, final String child) {
        super(parent, child);
        md5 = new File(this.getAbsolutePath() + ".md5");
        sha1 = new File(this.getAbsolutePath() + ".sha1");
        sha256 = new File(this.getAbsolutePath() + ".sha256");
    }

    public Binary(final File file) {
        super(file.getPath());
        md5 = new File(this.getAbsolutePath() + ".md5");
        sha1 = new File(this.getAbsolutePath() + ".sha1");
        sha256 = new File(this.getAbsolutePath() + ".sha256");
    }

    public Binary(final String file) {
        super(file);
        md5 = new File(this.getAbsolutePath() + ".md5");
        sha1 = new File(this.getAbsolutePath() + ".sha1");
        sha256 = new File(this.getAbsolutePath() + ".sha256");
    }

    public BinaryOutputStream write() {
        return new BinaryOutputStream();
    }

    public BinaryInputStream read() {
        return new BinaryInputStream();
    }

    public String getMd5() {
        return content(md5);
    }

    public String getSha1() {
        return content(sha1);
    }

    public String getSha256() {
        return content(sha256);
    }

    public void verify() {
        if (!md5.exists() && !sha1.exists() && !sha256.exists()) {
            throw new VerificationNotPossibleException(this, md5, sha1, sha256);
        }

        final BinaryInputStream in = read();
        try {
            IO.copy(in, new Ignore());
        } catch (final IOException e) {
            throw new ReadException(this, e);
        }

        if (md5.exists()) verify("MD5", in.getMd5(), getMd5());
        if (sha1.exists()) verify("SHA-1", in.getSha1(), getSha1());
        if (sha256.exists()) verify("SHA-256", in.getSha256(), getSha256());
    }

    public boolean verify(final PrintStream ps) {
        if (!md5.exists() && !sha1.exists() && !sha256.exists()) {
            ps.println(String.format("Unable to verify '%s'.  No digest files found.  Looked for: %s", this.getName(), Join.join(", ", File::getName, md5, sha1, sha256)));
            return false;
        }

        final BinaryInputStream in = read();
        try {
            IO.copy(in, new Ignore());
        } catch (final IOException e) {
            throw new ReadException(this, e);
        }

        boolean passed = true;

        if (md5.exists()) {
            passed = verify(ps, "MD5", in.getMd5(), getMd5()) && passed;
        }
        if (sha1.exists()) {
            passed = verify(ps, "SHA-1", in.getSha1(), getSha1()) && passed;
        }
        if (sha256.exists()) {
            passed = verify(ps, "SHA-256", in.getSha256(), getSha256()) && passed;
        }

        return passed;
    }

    private boolean verify(final PrintStream ps, final String algorithm, final String actualHex, final String expectedHex) {
        if (!expectedHex.equals(actualHex)) {
            ps.println(String.format("Verification failed %s.  Expected %s hash of %s, found %s", this.getName(), algorithm, expectedHex, actualHex));
            return false;
        }

        return true;
    }

    public void generate() {
        generate(false);
    }

    public void generate(final boolean overwrite) {
        final BinaryInputStream in = read();
        try {
            IO.copy(in, new Ignore());
        } catch (final IOException e) {
            throw new ReadException(this, e);
        }

        if (!md5.exists() || overwrite) {
            try {
                IO.copy(in.getMd5(), md5);
            } catch (IOException e) {
                throw new WriteException(md5, e);
            }
        }

        if (!sha1.exists() || overwrite) {
            try {
                IO.copy(in.getSha1(), sha1);
            } catch (IOException e) {
                throw new WriteException(sha1, e);
            }
        }

        if (!sha256.exists() || overwrite) {
            try {
                IO.copy(in.getSha256(), sha256);
            } catch (IOException e) {
                throw new WriteException(sha256, e);
            }
        }
    }

    private void verify(final String algorithm, final String actualHex, final String expectedHex) {
        if (!expectedHex.equals(actualHex)) {
            throw new VerificationFailedException(this, algorithm, expectedHex, actualHex);
        }
    }

    private String content(final File file) {
        if (!file.exists()) return null;
        try {
            return IO.slurp(file);
        } catch (final IOException e) {
            throw new ReadException(file, e);
        }
    }

    public static Binary from(final File file) {
        if (file instanceof Binary) {
            return (Binary) file;
        }
        return new Binary(file);
    }

    public class BinaryOutputStream extends OutputStream {

        private final DigestsOutputStream md5;
        private final DigestsOutputStream sha1;
        private final DigestsOutputStream sha256;
        private final OutputStream out;

        public BinaryOutputStream() {
            try {
                final OutputStream out = IO.write(Binary.this);
                this.md5 = new DigestsOutputStream(out, MD5);
                this.sha1 = new DigestsOutputStream(md5, SHA1);
                this.sha256 = new DigestsOutputStream(sha1, SHA256);
                this.out = sha256;
            } catch (final FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void write(final int b) throws IOException {
            out.write(b);
        }

        @Override
        public void write(final byte[] b) throws IOException {
            out.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            out.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            out.close();
            IO.copy(IO.read(md5.hex()), Binary.this.md5);
            IO.copy(IO.read(sha1.hex()), Binary.this.sha1);
            IO.copy(IO.read(sha256.hex()), Binary.this.sha256);
        }

        public String getMd5() {
            return md5.hex();
        }

        public String getSha1() {
            return sha1.hex();
        }

        public String getSha256() {
            return sha256.hex();
        }
    }

    public class BinaryInputStream extends InputStream {
        private final DigestsInputStream md5;
        private final DigestsInputStream sha1;
        private final DigestsInputStream sha256;
        private final InputStream in;

        public BinaryInputStream() {
            try {
                final InputStream in = IO.read(Binary.this);
                this.md5 = new DigestsInputStream(in, MD5);
                this.sha1 = new DigestsInputStream(md5, SHA1);
                this.sha256 = new DigestsInputStream(sha1, SHA256);
                this.in = sha256;
            } catch (final FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public int read() throws IOException {
            return in.read();
        }

        @Override
        public int read(final byte[] b) throws IOException {
            return in.read(b);
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            return in.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            in.close();
        }

        public String getMd5() {
            return md5.hex();
        }

        public String getSha1() {
            return sha1.hex();
        }

        public String getSha256() {
            return sha256.hex();
        }
    }

    public static class ReadException extends RuntimeException {
        public ReadException(final File file, final Throwable cause) {
            super(String.format("Unable to read '%s'", file.getAbsolutePath()), cause);
        }
    }

    public static class WriteException extends RuntimeException {
        public WriteException(final File file, final Throwable cause) {
            super(String.format("Unable to write '%s'", file.getAbsolutePath()), cause);
        }
    }

    public static class VerificationFailedException extends RuntimeException {
        public VerificationFailedException(final File file, final String algorithm, final String expectedHex, final String actualHex) {
            super(String.format("Verification failed %s.  Expected %s hash of %s, found %s", file.getName(), algorithm, expectedHex, actualHex));
        }
    }

    public static class VerificationNotPossibleException extends RuntimeException {
        public VerificationNotPossibleException(final File file, final File... digests) {
            super(String.format("Unable to verify '%s'.  No digest files found.  Looked for%n%s", file.getName(), Join.join("\n", File::getName, digests)));
        }
    }

    private static class Ignore extends OutputStream {
        @Override
        public void write(final int b) throws IOException {
        }

        @Override
        public void write(final byte[] b) throws IOException {
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
        }
    }
}
