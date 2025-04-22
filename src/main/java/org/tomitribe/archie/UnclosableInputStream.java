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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UnclosableInputStream extends InputStream {
    private final InputStream in;

    public UnclosableInputStream(final InputStream in) {
        this.in = in;
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
    public byte[] readAllBytes() throws IOException {
        return in.readAllBytes();
    }

    @Override
    public byte[] readNBytes(final int len) throws IOException {
        return in.readNBytes(len);
    }

    @Override
    public int readNBytes(final byte[] b, final int off, final int len) throws IOException {
        return in.readNBytes(b, off, len);
    }

    @Override
    public long skip(final long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        // ignore
    }

    @Override
    public void mark(final int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public long transferTo(final OutputStream out) throws IOException {
        return in.transferTo(out);
    }
}
