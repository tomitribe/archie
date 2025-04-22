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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public enum Digest {
    MD5("MD5"),
    SHA1("SHA-1"),
    SHA256("SHA-256");

    private final String algorithm;

    Digest(final String algorithm) {
        this.algorithm = algorithm;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public MessageDigest digest() {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            throw new NoSuchDigestAlgorithmException(algorithm, e);
        }
    }

    /**
     * Avoid this method for actual binaries.  This is
     * intended for small files like text files or the
     * files holding hashes of other binaries.
     */
    public String digest(final File file) {
        try {
            final DigestsInputStream in = digest(IO.read(file));
            IO.copy(in, OutputStream.nullOutputStream());
            in.close();
            return in.hex();
        } catch (final IOException e) {
            throw new DigestCreationException(this, file, e);
        }
    }

    public DigestsOutputStream digest(final OutputStream out) {
        return new DigestsOutputStream(out, this);
    }

    public DigestsInputStream digest(final InputStream out) {
        return new DigestsInputStream(out, this);
    }

    public static class DigestCreationException extends RuntimeException {

        public DigestCreationException(final Digest digest, final File file, final Throwable cause) {
            super(String.format("Unable to create %s hash for file '%s'", digest.name(), file.getName()), cause);
        }
    }
}
