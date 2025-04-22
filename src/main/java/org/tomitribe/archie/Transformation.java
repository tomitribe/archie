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

import lombok.Data;
import org.apache.commons.compress.archivers.ArchiveOutputStream;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Data
public class Transformation implements Function<byte[], byte[]> {
    private final Predicate<String> condition;
    private final Function<byte[], byte[]> transformation;

    public Transformation(final Predicate<String> condition, final Function<byte[], byte[]> transformation) {
        this.condition = condition;
        this.transformation = transformation;
    }

    public boolean applies(final String name) {
        return condition.test(name);
    }

    public byte[] apply(final byte[] bytes) {
        return transformation.apply(bytes);
    }

    @Data
    public static class Action implements Consumer<ArchiveOutputStream> {
        private final Predicate<String> condition;
        private final Consumer<ArchiveOutputStream> transformation;

        public boolean applies(final String name) {
            return condition.test(name);
        }

        public void accept(final ArchiveOutputStream archiveOutputStream) {
            transformation.accept(archiveOutputStream);
        }
    }
}
