/*
 * Copyright (C) 2024 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okio

actual class InflaterSource actual constructor(
  source: Source,
  internal val inflater: Inflater,
) : Source {
  private val source: BufferedSource = source.buffer()

  @Throws(IOException::class)
  override fun read(sink: Buffer, byteCount: Long): Long {
    require(byteCount >= 0L) { "byteCount < 0: $byteCount" }

    return inflater.dataProcessor.readBytesToTarget(
      source = source,
      targetMaxByteCount = byteCount,
      target = sink,
    )
  }

  override fun timeout(): Timeout {
    return source.timeout()
  }

  override fun close() {
    if (inflater.dataProcessor.closed) return

    inflater.dataProcessor.close()

    source.close()
  }
}
