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

import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.*;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.Optional;

/**
 * @author <a href="mailto: adam@evolvedbinary.com">Adam Retter</a>
 */
public class Utils {

    static Optional<BinaryValue> getBinaryArg(final Sequence[] args, final int idx) throws XPathException {
        if(args.length > idx) {
            final Sequence arg = args[idx];
            if (!arg.isEmpty()) {

                final Item item = arg.itemAt(0);
                if (item != null) {
                    return Optional.ofNullable((BinaryValue)item.convertTo(Type.BASE64_BINARY));
                }
            }
        }

        return Optional.empty();
    }

    static Optional<BinaryValue[]> getBinarySequenceArg(final Sequence[] args, final int idx) throws XPathException {
        if(args.length > idx) {
            final Sequence arg = args[idx];
            if (!arg.isEmpty()) {

                final int len = arg.getItemCount();
                final BinaryValue[] bins = new BinaryValue[len];

                for(int i = 0; i < len; i++) {
                    final BinaryValue bin = (BinaryValue)arg.itemAt(i).convertTo(Type.BASE64_BINARY);
                    bins[i] = bin;
                }

                return Optional.of(bins);
            }
        }

        return Optional.empty();
    }

    static Optional<BigInteger> getIntegerArg(final Sequence[] args, final int idx) throws XPathException {
        if(args.length > idx) {
            final Sequence arg = args[idx];
            if (!arg.isEmpty()) {

                final Item item = arg.itemAt(0);
                if (item != null) {
                    return Optional.ofNullable(item.toJavaObject(BigInteger.class));
                }
            }
        }

        return Optional.empty();
    }

    static Optional<BigInteger[]> getIntegerSequenceArg(final Sequence[] args, final int idx) throws XPathException {
        if(args.length > idx) {
            final Sequence arg = args[idx];
            if (!arg.isEmpty()) {

                final int len = arg.getItemCount();
                final BigInteger[] bigInts = new BigInteger[len];

                for(int i = 0; i < len; i++) {
                    final IntegerValue intValue = (IntegerValue)arg.itemAt(i).convertTo(Type.INTEGER);
                    final BigInteger bigInt = intValue.toJavaObject(BigInteger.class);
                    bigInts[i] = bigInt;
                }

                return Optional.of(bigInts);
            }
        }

        return Optional.empty();
    }

    static Optional<String> getStringArg(final Sequence[] args, final int idx) throws XPathException {
        if(args.length > idx) {
            final Sequence arg = args[idx];
            if (!arg.isEmpty()) {

                final Item item = arg.itemAt(0);
                if (item != null) {
                    return Optional.ofNullable(item.toJavaObject(String.class));
                }
            }
        }

        return Optional.empty();
    }

    static BinaryValue newEmptyBinary(final XQueryContext context) throws XPathException {
        return newInMemoryBinary(context, new byte[0]);
    }

    static BinaryValue newInMemoryBinary(final XQueryContext context, final byte[] data) throws XPathException {
        return BinaryValueFromInputStream.getInstance(context, new Base64BinaryValueType(), new ByteArrayInputStream(data));
    }
}
