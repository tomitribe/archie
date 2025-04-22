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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @version $Revision$ $Date$
 */
public class Asmifier {

    public static String asmify(final byte[] actualBytes) throws IOException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        final ClassReader reader = new ClassReader(actualBytes);
        final TraceClassVisitor visitor = new TraceClassVisitor(null, new ASMifier(), new PrintWriter(byteArrayOutputStream));
        reader.accept(visitor, ClassReader.SKIP_DEBUG);
        byteArrayOutputStream.close();

        return new String(byteArrayOutputStream.toByteArray());
    }
}
