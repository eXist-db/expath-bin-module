/**
 * Copyright © 2017, eXist-db
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.exist.xquery.modules.expath.bin;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by aretter on 24/05/2017.
 */
public class JoinFilterInputStream extends FilterInputStream {

    public static final int END_OF_STREAM = -1;

    private final InputStream[] ins;
    private int insIdx = 0;

    public JoinFilterInputStream(final InputStream[] ins) {
        super(ins[0]);
        this.ins = ins;
    }

    @Override
    public int read() throws IOException {
        final int data = ins[insIdx].read();
        if(data == END_OF_STREAM) {
            // can we move to the next input stream?
            if(insIdx + 1 < ins.length) {
                // read from the next input stream
                insIdx++;
                return read();
            }
            return END_OF_STREAM;
        } else {
            return data;
        }
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {

        int remainingLen = len;
        int totalRead = 0;
        int writeOffset = off;

        while(remainingLen > 0) {
            final int read = ins[insIdx].read(b, writeOffset, remainingLen);
            if(read == END_OF_STREAM) {
                // can we move to the next input stream?
                if(insIdx + 1 < ins.length) {
                    // move to the next input stream
                    insIdx++;
                } else {
                    return totalRead == 0 ? END_OF_STREAM : totalRead;
                }
            } else if(read < remainingLen) {
                totalRead += read;
                // can we move to the next input stream?
                if(insIdx + 1 < ins.length) {
                    // move to the next input stream
                    insIdx++;
                    writeOffset += read;
                    remainingLen -= read;
                } else {
                    return totalRead;
                }
            }
        }

        return totalRead;
    }

    @Override
    public long skip(final long n) throws IOException {
        int skipped = 0;
        while(skipped < n) {
            final long inSkip = ins[insIdx].skip(n - skipped);
            if(inSkip <= 0) {
                // can we move to the next input stream?
                if(insIdx + 1 < ins.length) {
                    // move to the next input stream
                    insIdx++;
                } else {
                    // END of input streams
                    break;
                }
            } else {
                skipped += inSkip;
            }
        }
        return skipped;
    }

    @Override
    public int available() throws IOException {
        return ins[insIdx].available();
    }

    @Override
    public void close() throws IOException {
        IOException firstException = null;

        for(int i = ins.length - 1; i > -1; i--) {
            try {
                ins[i].close();
            } catch(final IOException e) {
                if(firstException == null) {
                    firstException = e;
                }
            }
        }

        if(firstException != null) {
            throw new IOException("first exception on close", firstException);
        }
    }
}
