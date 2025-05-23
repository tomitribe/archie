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
package com.example;

public class StaticConfiguration {
    public static byte[] getPublicKey() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public static void main(String[] args) {
        final StringBuilder sb = new StringBuilder();

        sb.append("return new byte[] {");

        final byte[] key = getPublicKey();

        for (int i = 0; i < key.length; i++) {
            if (i % 16 == 0) {
                sb.append("\n\t");
            }

            sb.append(key[i]);
            if (i < (key.length - 1)) {
                sb.append(", ");
            }
        }

        sb.append("};");

        System.out.println(sb.toString());
    }
}
