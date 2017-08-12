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

import net.jcip.annotations.NotThreadSafe;
import org.exist.util.io.CachingFilterInputStream;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.XPathErrorProvider;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.exist.xquery.modules.expath.bin.ExpathBinModule.ERROR_INDEX_OUT_OF_RANGE;

/**
 * Created by aretter on 24/05/2017.
 */
@NotThreadSafe
public class RegionFilterInputStream extends FilterInputStream {

    public static final int END_OF_STREAM = -1;

    // the region within `in` that is accessible to us
    private final int regionOffset;
    private final int regionLen;

    private int curOffset = 0;

    /**
     * @param in The input stream to provide a region of
     * @param regionOffset The offset for the start of the region
     * @param regionLen The length of the region (starting at regionOffset) within the input stream, or -1 to extend the region to the end of the stream
     */
    public RegionFilterInputStream(final InputStream in, final int regionOffset, final int regionLen) {
        super(in);
        this.regionOffset = regionOffset;
        this.regionLen = regionLen;
        if (in instanceof CachingFilterInputStream) {
            final CachingFilterInputStream cachingFilterInputStream = (CachingFilterInputStream) in;
            cachingFilterInputStream.incrementSharedReferences();
        }
    }

    @Override
    public int read() throws IOException {
        if(curOffset == 0) {
            seekRegionStart();
        }

        if(regionLen != END_OF_STREAM && curOffset == regionOffset + regionLen) {
            return END_OF_STREAM;
        }

        final int data = in.read();
        if(data > END_OF_STREAM) {
            curOffset++;
        }

        if(data == END_OF_STREAM && curOffset < regionOffset + regionLen) {
            // reached end of stream before all bytes were read
            throw new IndexOutOfRangeException("Reached underlying END_OF_STREAM, before end of region");
        }

        return data;
    }

    @Override
    public int read(final byte[] b, final int off, int len) throws IOException {
        if(curOffset == 0) {
            seekRegionStart();
        }

        // restrict to within the region
        if(regionLen != END_OF_STREAM) {
            if(curOffset + len > regionOffset + regionLen) {
                len = (regionOffset + regionLen) - curOffset;
                if(len == 0) {
                    return END_OF_STREAM;
                }
            }
        }

        final int read = in.read(b, off, len);
        if(read > END_OF_STREAM) {
            curOffset += read;
        }

        if(read < len && curOffset < regionOffset + regionLen) {
            // reached end of stream before all bytes were read
            throw new IndexOutOfRangeException("Reached underlying END_OF_STREAM, before end of region");
        }

        return read;
    }

    @Override
    public long skip(final long n) throws IOException {
        if(curOffset == 0) {
            seekRegionStart();
        }

        if(n > regionLen) {
            return in.skip(regionLen);
        } else {
            return in.skip(n);
        }
    }

    @Override
    public int available() throws IOException {
        if(curOffset == 0) {
            seekRegionStart();
        }

        if(regionLen != END_OF_STREAM) {
            final int availableInRegion = (regionOffset + regionLen) - curOffset;
            return Math.min(in.available(), availableInRegion);
        } else {
            return in.available();
        }
    }

    private void seekRegionStart() throws IOException {
        if(curOffset != 0) {
            return;
        }

        int toSkip = regionOffset;

        while(toSkip > 0) {
            final int skipped = (int) in.skip(regionOffset);

            if(skipped == END_OF_STREAM) {
                return;
            }

            toSkip -= skipped;
            curOffset += skipped;
        }

        if(toSkip > 0) {
            throw new ArrayIndexOutOfBoundsException("Reached end of stream whilst trying to seek to region start. regionOffset=" + regionOffset + ", curOffset=" + curOffset);
        }
    }

    public static class IndexOutOfRangeException extends IOException implements XPathErrorProvider {
        public IndexOutOfRangeException(final String message) {
            super(message);
        }

        @Override
        public ErrorCodes.ErrorCode getErrorCode() {
            return ERROR_INDEX_OUT_OF_RANGE;
        }
    }
}
