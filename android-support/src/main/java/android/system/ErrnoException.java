/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.system;

import java.io.IOException;
import java.net.SocketException;

/**
 * A checked exception thrown when Os methods fail. This exception contains the native
 * errno value, for comparison against the constants in OsConstants, should sophisticated
 * callers need to adjust their behavior based on the exact failure.
 */
public final class ErrnoException extends Exception {
    private final String functionName;

    /**
     * The errno value, for comparison with the {@code E} constants in OsConstants.
     */
    public final int errno;

    /**
     * Constructs an instance with the given function name and errno value.
     */
    public ErrnoException(String functionName, int errno) {
        this.functionName = functionName;
        this.errno = errno;
    }

    /**
     * Constructs an instance with the given function name, errno value, and cause.
     */
    public ErrnoException(String functionName, int errno, Throwable cause) {
        super(cause);
        this.functionName = functionName;
        this.errno = errno;
    }

    /**
     * Converts the stashed function name and errno value to a human-readable string.
     * We do this here rather than in the constructor so that callers only pay for
     * this if they need it.
     */
    @Override public String getMessage() {
        return "dummy";
    }

    /**
     * @hide - internal use only.
     */
    public IOException rethrowAsIOException() throws IOException {
        IOException newException = new IOException(getMessage());
        newException.initCause(this);
        throw newException;
    }

    /**
     * @hide - internal use only.
     */
    public SocketException rethrowAsSocketException() throws SocketException {
        throw new SocketException(getMessage());
    }
}
