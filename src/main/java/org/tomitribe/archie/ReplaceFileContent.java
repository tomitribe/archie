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

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class ReplaceFileContent implements Function<byte[], byte[]> {
    private final String content;

    private ReplaceFileContent(final String content) {
        this.content = content;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public byte[] apply(byte[] bytes) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public static class Builder {
        private String content = null;

        private Builder() {
        }

        public Builder content(final String content) {
            this.content = content;
            return this;
        }

        public ReplaceFileContent build() {
            if (content == null) {
                throw new IllegalArgumentException("No content provided");
            }

            return new ReplaceFileContent(content);
        }
    }
}
