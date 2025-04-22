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

public interface Transformer {
    void transform(final InputStream in, final OutputStream out) throws IOException;

    default void transform(final File src, final File dest) throws IOException {
        try (final InputStream in = IO.read(src); final OutputStream out = dest instanceof Binary ? ((Binary) dest).write() : IO.write(dest)) {
            transform(in, out);
        }
    }
}
