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

import org.tomitribe.util.Hex;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;

public class DigestsOutputStream extends OutputStream {

    private final MessageDigest messageDigest;
    private OutputStream out;
    private volatile long byteCount;

    public DigestsOutputStream(final OutputStream out, final Digest digest) {
        this.out = out;
        this.messageDigest = digest.digest();
    }

    @Override
    public void write(final int b) throws IOException {
        messageDigest.update((byte) b);

        byteCount++;
        out.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        messageDigest.update(b);

        byteCount += b.length;
        out.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        messageDigest.update(b, off, len);

        byteCount += len;
        out.write(b, off, len);
    }

    public byte[] digest() {
        return messageDigest.digest();
    }

    public String hex() {
        return Hex.toString(digest());
    }

    public long getByteCount() {
        return byteCount;
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
