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

import org.exist.xquery.BasicFunction;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

import static org.exist.xquery.modules.expath.bin.ExpathBinModule.*;
import static org.exist.xquery.modules.expath.bin.FunctionSignatureHelpers.*;
import static org.exist.xquery.modules.expath.bin.Utils.*;

/**
 * @author <a href="mailto: adam@evolvedbinary.com">Adam Retter</a>
 */
public class BasicFunctions extends BasicFunction {

    private static final FunctionParameterSequenceType FS_OPT_PARAM_IN = optParam("in", Type.BASE64_BINARY, "The binary data");

    private static final String FS_LENGTH_NAME = "length";
    static final FunctionSignature FS_LENGTH = functionSignature(
            FS_LENGTH_NAME,
            "Returns the size of binary data in octets.",
            returns(Type.INTEGER),
            param("in", Type.BASE64_BINARY, "The binary data")
    );

    private static final String FS_PART_NAME = "part";
    private static final FunctionParameterSequenceType FS_PART_PARAM_OFFSET = param("offset", Type.INTEGER, "The offset to start reading from");
    static final FunctionSignature[] FS_PART = functionSignatures(
            FS_PART_NAME,
            "Returns a specified part of binary data.",
            returnsOpt(Type.BASE64_BINARY),
            variableParams(
                arity(
                    FS_OPT_PARAM_IN,
                    FS_PART_PARAM_OFFSET
                ),
                arity(
                    FS_OPT_PARAM_IN,
                    FS_PART_PARAM_OFFSET,
                    param("size", Type.INTEGER, "The number of octets to read from the offset")
                )
            )
    );

    private static final String FS_JOIN_NAME = "join";
    static final FunctionSignature FS_JOIN = functionSignature(
            FS_JOIN_NAME,
            "Returns the binary data created by concatenating the binary data items in a sequence.",
            returns(Type.BASE64_BINARY),
            optManyParam("in", Type.BASE64_BINARY, "The binary data")
    );

    private static final String FS_INSERT_BEFORE_NAME = "insert-before";
    static final FunctionSignature FS_INSERT_BEFORE = functionSignature(
            FS_INSERT_BEFORE_NAME,
            "Returns a specified part of binary data.",
            returnsOpt(Type.BASE64_BINARY),
            FS_OPT_PARAM_IN,
            param("offset", Type.INTEGER, "The offset to insert at"),
            optParam("extra", Type.BASE64_BINARY, "The binary data to insert")
    );

    private static final String FS_PAD_LEFT_NAME = "pad-left";
    private static final FunctionParameterSequenceType FS_PAD_LEFT_SIZE_PARAM = param("size", Type.INTEGER, "The number of octets to pad from the left");
    static final FunctionSignature[] FS_PAD_LEFT = functionSignatures(
            FS_PAD_LEFT_NAME,
            "Returns the binary data created by padding $in with $size octets from the left. The padding octet values are $octet or zero if omitted.",
            returnsOpt(Type.BASE64_BINARY),
            variableParams(
                    arity(
                            FS_OPT_PARAM_IN,
                            FS_PAD_LEFT_SIZE_PARAM
                    ),
                    arity(
                            FS_OPT_PARAM_IN,
                            FS_PART_PARAM_OFFSET,
                            param("octet", Type.INTEGER, "The padding value")
                    )
            )
    );

    private static final String FS_PAD_RIGHT_NAME = "pad-right";
    private static final FunctionParameterSequenceType FS_PAD_RIGHT_SIZE_PARAM = param("size", Type.INTEGER, "The number of octets to pad from the right");
    static final FunctionSignature[] FS_PAD_RIGHT = functionSignatures(
            FS_PAD_RIGHT_NAME,
            "Returns the binary data created by padding $in with $size blank octets from the right. The padding octet values are $octet or zero if omitted.",
            returnsOpt(Type.BASE64_BINARY),
            variableParams(
                    arity(
                            FS_OPT_PARAM_IN,
                            FS_PAD_LEFT_SIZE_PARAM
                    ),
                    arity(
                            FS_OPT_PARAM_IN,
                            FS_PART_PARAM_OFFSET,
                            param("octet", Type.INTEGER, "The padding value")
                    )
            )
    );

    private static final String FS_FIND_NAME = "find";
    static final FunctionSignature FS_FIND= functionSignature(
            FS_FIND_NAME,
            "The function returns the first location of the binary search sequence in the input, or if not found, the empty sequence.",
            returnsOpt(Type.INTEGER),
            FS_OPT_PARAM_IN,
            param("offset", Type.INTEGER, "The offset to start searching from"),
            param("search", Type.BASE64_BINARY, "The binary data to search for")
    );


    public BasicFunctions(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        switch(getName().getLocalPart()) {
            case FS_LENGTH_NAME:
                final Optional<BinaryValue> inLengthBase64 = getBinaryArg(args, 0);
                if(inLengthBase64.isPresent()) {
                    return length(inLengthBase64.get());
                } else {
                    throw new XPathException(this, "$in argument cannot be absent");
                }

            case FS_PART_NAME:
                final Optional<BinaryValue> inPartBase64 = getBinaryArg(args, 0);
                if(inPartBase64.isPresent()) {
                    final BigInteger offset = getIntegerArg(args, 1).orElse(BigInteger.ZERO);
                    if(offset.compareTo(BigInteger.ZERO) < 0) {
                        throw new XPathException(this, ERROR_INDEX_OUT_OF_RANGE, "$offset is negative:" + offset);
                    }
                    final Optional<BigInteger> size;
                    if(args.length == 3) {
                        size = getIntegerArg(args, 2);
                    } else {
                        size = Optional.empty();
                    }
                    if (size.map(s -> s.compareTo(BigInteger.ZERO) < 0).orElse(false)) {
                        throw new XPathException(this, ERROR_NEGATIVE_SIZE, "$size is negative:" + size.get());
                    }

                    return part(inPartBase64.get(), offset, size);
                } else {
                    return Sequence.EMPTY_SEQUENCE;
                }

            case FS_JOIN_NAME:
                final Optional<BinaryValue[]> inJoinBase64s = getBinarySequenceArg(args, 0);
                if(inJoinBase64s.isPresent()) {
                    return join(inJoinBase64s.get());
                } else {
                    return newEmptyBinary(context);
                }

            case FS_INSERT_BEFORE_NAME:
                final Optional<BinaryValue> inInsertBeforeBase64 = getBinaryArg(args, 0);
                final BigInteger offset = getIntegerArg(args, 1).orElse(BigInteger.ZERO);
                if(offset.compareTo(BigInteger.ZERO) < 0) {
                    throw new XPathException(this, ERROR_INDEX_OUT_OF_RANGE, "$offset is negative:" + offset);
                }
                final Optional<BinaryValue> extraBase64 = getBinaryArg(args, 2);
                if(inInsertBeforeBase64.isPresent()) {
                    if(extraBase64.isPresent()) {
                        return insertBefore(inInsertBeforeBase64.get(), offset, extraBase64.get());
                    } else {
                        return inInsertBeforeBase64.get();
                    }
                } else {
                    return Sequence.EMPTY_SEQUENCE;
                }

            case FS_PAD_LEFT_NAME:
            case FS_PAD_RIGHT_NAME:
                final Optional<BinaryValue> inPadBase64 = getBinaryArg(args, 0);
                if(inPadBase64.isPresent()) {
                    final BigInteger size = getIntegerArg(args, 1).orElse(BigInteger.ZERO);
                    if (size.compareTo(BigInteger.ZERO) < 0) {
                        throw new XPathException(this, ERROR_NEGATIVE_SIZE, "$size is negative:" + size);
                    }
                    final BigInteger octet;
                    if(args.length == 3) {
                        octet = getIntegerArg(args, 2).orElse(BigInteger.ZERO);
                    } else {
                        octet = BigInteger.ZERO;
                    }
                    if(octet.compareTo(BigInteger.ZERO) < 0 || octet.compareTo(BigInteger.valueOf(0xff)) > 1) {
                        throw new XPathException(this, ERROR_OCTET_OUT_OF_RANGE, "$octet: " + octet + " is out of range");
                    }

                    if(getName().getLocalPart().equals(FS_PAD_LEFT_NAME)) {
                        return padLeft(inPadBase64.get(), size, octet.intValue());
                    } else {
                        return padRight(inPadBase64.get(), size, octet.intValue());
                    }
                } else {
                    return Sequence.EMPTY_SEQUENCE;
                }

            case FS_FIND_NAME:
                final Optional<BinaryValue> inFindBase64 = getBinaryArg(args, 0);
                if(inFindBase64.isPresent()) {
                    final BigInteger findOffset = getIntegerArg(args, 1).orElse(BigInteger.ZERO);
                    if(findOffset.compareTo(BigInteger.ZERO) < 0) {
                        throw new XPathException(this, ERROR_INDEX_OUT_OF_RANGE, "$offset is negative:" + findOffset);
                    }
                    final BinaryValue searchBase64 = getBinaryArg(args, 2).orElse(newEmptyBinary(context));
                    return find(inFindBase64.get(), findOffset, searchBase64);
                } else {
                    return Sequence.EMPTY_SEQUENCE;
                }

            default:
                throw new XPathException(this, "No function named: " + getName());
        }
    }

    private IntegerValue length(final BinaryValue binValue) throws XPathException {
        final byte buf[] = new byte[4096];
        long len = 0;
        try {
            // we don't need to close the stream, it will be closed by BinaryValueFromInputStream when it goes out of context
            final InputStream is = binValue.getInputStream();
            int read = -1;
            while((read = is.read(buf)) > -1) {
                len += read;
            }
            return new IntegerValue(len);
        } catch(final IOException e) {
            throw new XPathException(this, e);
        }
    }

    private BinaryValue part(final BinaryValue binValue, final BigInteger offset, final Optional<BigInteger> size) throws XPathException {
        // we don't need to close the stream, it will be closed by BinaryValueFromInputStream when it goes out of context
        return BinaryValueFromInputStream.getInstance(context, new Base64BinaryValueType(), new RegionFilterInputStream(binValue.getInputStream(), offset.intValue(), size.orElse(BigInteger.valueOf(RegionFilterInputStream.END_OF_STREAM)).intValue()));
    }

    private BinaryValue join(final BinaryValue[] binValues) throws XPathException {
        // we don't need to close the streams, they will be closed by BinaryValueFromInputStream when it goes out of context
        return BinaryValueFromInputStream.getInstance(context, new Base64BinaryValueType(), new JoinFilterInputStream(Arrays.stream(binValues).map(BinaryValue::getInputStream).toArray(InputStream[]::new)));
    }

    private BinaryValue insertBefore(final BinaryValue data, final BigInteger offset, final BinaryValue extra) throws XPathException {
        final InputStream[] streams;
        if(offset.intValue() == 0) {
            streams = new InputStream[2];
            streams[0] = extra.getInputStream();
            streams[1] = new RegionFilterInputStream(data.getInputStream(), offset.intValue(), RegionFilterInputStream.END_OF_STREAM);
        } else {
            streams = new InputStream[3];
            streams[0] = new RegionFilterInputStream(data.getInputStream(), 0, offset.intValue());
            streams[1] = extra.getInputStream();
            streams[2] = new RegionFilterInputStream(data.getInputStream(), offset.intValue(), RegionFilterInputStream.END_OF_STREAM);
        }

        // we don't need to close the streams, they will be closed by BinaryValueFromInputStream when it goes out of context
        return BinaryValueFromInputStream.getInstance(context, new Base64BinaryValueType(), new JoinFilterInputStream(streams));
    }

    private BinaryValue padLeft(final BinaryValue data, final BigInteger size, final int octet) throws XPathException {
        final byte[] padding = new byte[size.intValue()];
        final byte b = (byte)(octet & 0xff);
        Arrays.fill(padding, b);

        return BinaryValueFromInputStream.getInstance(context, new Base64BinaryValueType(), new JoinFilterInputStream(new InputStream[] {
                new ByteArrayInputStream(padding),
                data.getInputStream()
        }));
    }

    private BinaryValue padRight(final BinaryValue data, final BigInteger size, final int octet) throws XPathException {
        final byte[] padding = new byte[size.intValue()];
        final byte b = (byte)(octet & 0xff);
        Arrays.fill(padding, b);

        return BinaryValueFromInputStream.getInstance(context, new Base64BinaryValueType(), new JoinFilterInputStream(new InputStream[] {
                data.getInputStream(),
                new ByteArrayInputStream(padding)
        }));
    }

    private Sequence find(final BinaryValue data, final BigInteger offset, final BinaryValue search) throws XPathException {
        final int off = offset.intValue();
        try(final ByteArrayOutputStream baosData = new ByteArrayOutputStream()) {
            data.streamTo(baosData);
            final byte[] bufData = baosData.toByteArray();

            if(off > bufData.length) {
                throw new XPathException(this, ERROR_INDEX_OUT_OF_RANGE, "$offset is larger than the size of the binary data in $in");
            }

            try(final ByteArrayOutputStream baosSearch = new ByteArrayOutputStream()) {
                search.streamTo(baosSearch);
                final byte[] bufSearch = baosSearch.toByteArray();

                if(bufSearch.length == 0) {
                    // If $search is empty $offset is returned.
                    return new IntegerValue(offset);
                }

                if(bufSearch.length > bufData.length) {
                    // $search is larger than $in
                    return Sequence.EMPTY_SEQUENCE;
                }

                if(off + bufSearch.length > bufData.length) {
                    // $offset + $search is larger than $in
                    return Sequence.EMPTY_SEQUENCE;
                }

                final int foundOffset = findOffset(bufData, off, bufSearch);
                if(foundOffset == -1) {
                    return Sequence.EMPTY_SEQUENCE;
                } else {
                    return new IntegerValue(foundOffset);
                }
            }
        } catch (final IOException e) {
            throw new XPathException(this, e);
        }
    }

    private int findOffset(final byte[] data, final int offset, final byte[] search) {
        for(int i = offset; i < data.length - search.length + 1; i++) {
            boolean found = true;
            for(int j = 0; j < search.length; ++j) {
                if (data[i + j] != search[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }
}
