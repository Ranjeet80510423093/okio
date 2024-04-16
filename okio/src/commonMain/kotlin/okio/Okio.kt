/*
 * Copyright (C) 2019 Square, Inc.
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

/** Essential APIs for working with Okio. */
@file:JvmMultifileClass
@file:JvmName("Okio")

package okio

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Returns a new source that buffers reads from `source`. The returned source will perform bulk
 * reads into its in-memory buffer. Use this wherever you read a source to get an ergonomic and
 * efficient access to data.
 */
fun Source.buffer(): BufferedSource = RealBufferedSource(this)

/**
 * Returns a new sink that buffers writes to `sink`. The returned sink will batch writes to `sink`.
 * Use this wherever you write to a sink to get an ergonomic and efficient access to data.
 */
fun Sink.buffer(): BufferedSink = RealBufferedSink(this)

/** Returns a sink that writes nowhere. */
@JvmName("blackhole")
fun blackholeSink(): Sink = BlackholeSink()

private class BlackholeSink : Sink {
  override fun write(source: Buffer, byteCount: Long) = source.skip(byteCount)
  override fun flush() {}
  override fun timeout() = Timeout.NONE
  override fun close() {}
}

/** Execute [block] then close this. This will be closed even if [block] throws. */
inline fun <T : Closeable?, R> T.use(block: (T) -> R): R {
  var thrown: Throwable? = null

  val result = try {
    block(this)
  } catch (t: Throwable) {
    thrown = t
    null
  } finally {
    try {
      this?.close()
    } catch (t: Throwable) {
      if (thrown == null) {
        thrown = t
      } else {
        thrown.addSuppressed(t)
      }
    }
  }

  if (thrown != null) throw thrown
  @Suppress("UNCHECKED_CAST")
  return result as R
}

/**
 * Returns a new file system that forwards all calls to this, and that also returns [extension]
 * when it is requested.
 *
 * When [E] is requested on the returned file system, it will return [extension], regardless of what
 * is returned by this file system.
 */
inline fun <reified E : FileSystemExtension> FileSystem.extend(extension: E): FileSystem =
  extend(E::class, extension)

/** Returns the extension for [E] if it exists, and null otherwise. */
inline fun <reified E : FileSystemExtension> FileSystem.extension(): E? = extension(E::class)


fun chain(outer: PathMapper, inner: PathMapper): PathMapper {
  return object : PathMapper {
    override fun onPathParameter(path: Path, functionName: String, parameterName: String): Path {
      return inner.onPathParameter(
        outer.onPathParameter(
          path,
          functionName,
          parameterName,
        ),
        functionName,
        parameterName,
      )
    }

    override fun onPathResult(path: Path, functionName: String): Path {
      return outer.onPathResult(
        inner.onPathResult(
          path,
          functionName,
        ),
        functionName,
      )
    }
  }
}
