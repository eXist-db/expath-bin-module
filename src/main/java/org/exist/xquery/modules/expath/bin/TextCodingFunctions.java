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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.xquery.modules.expath.bin.ExpathBinModule.*;
import static org.exist.xquery.modules.expath.bin.FunctionSignatureHelpers.*;
import static org.exist.xquery.modules.expath.bin.Utils.*;

/**
 * @author <a href="mailto: adam@evolvedbinary.com">Adam Retter</a>
 */
public class TextCodingFunctions extends BasicFunction {

    private static final FunctionParameterSequenceType FS_CODING_PARAM_ENCODING = param("encoding", Type.STRING, "The character set encoding");

    private static final String FS_DECODE_STRING_NAME = "decode-string";
    private static final FunctionParameterSequenceType FS_DECODE_STRING_PARAM_IN = optParam("in", Type.BASE64_BINARY, "The binary data");
    private static final FunctionParameterSequenceType FS_DECODE_STRING_PARAM_OFFSET = param("offset", Type.INTEGER, "The offset to start decoding from");
    static final FunctionSignature[] FS_DECODE_STRING = functionSignatures(
            FS_DECODE_STRING_NAME,
            "Decodes binary data as a string in a given encoding.",
            returnsOpt(Type.STRING),
            arities(
                    arity(
                            FS_DECODE_STRING_PARAM_IN
                    ),
                    arity(
                            FS_DECODE_STRING_PARAM_IN,
                            FS_CODING_PARAM_ENCODING
                    ),
                    arity(
                            FS_DECODE_STRING_PARAM_IN,
                            FS_CODING_PARAM_ENCODING,
                            FS_DECODE_STRING_PARAM_OFFSET
                    ),
                    arity(
                            FS_DECODE_STRING_PARAM_IN,
                            FS_CODING_PARAM_ENCODING,
                            FS_DECODE_STRING_PARAM_OFFSET,
                            param("size", Type.INTEGER, "The number of octets to decode")
                    )
            )
    );

    private static final String FS_ENCODE_STRING_NAME = "encode-string";
    private static final FunctionParameterSequenceType FS_ENCODE_STRING_PARAM_IN = optParam("in", Type.STRING, "The string data to encode into binary data");
    static final FunctionSignature[] FS_ENCODE_STRING = functionSignatures(
            FS_ENCODE_STRING_NAME,
            "Encodes a string into binary data using a given encoding.",
            returnsOpt(Type.BASE64_BINARY),
            arities(
                    arity(
                            FS_ENCODE_STRING_PARAM_IN
                    ),
                    arity(
                            FS_ENCODE_STRING_PARAM_IN,
                            FS_CODING_PARAM_ENCODING
                    )
            )
    );

    public TextCodingFunctions(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        switch(getName().getLocalPart()) {
            case FS_DECODE_STRING_NAME:
                final Optional<BinaryValue> inBase64 = getBinaryArg(args, 0);
                if(inBase64.isPresent()) {
                    Optional<Charset> encoding = Optional.empty();
                    Optional<BigInteger> offset = Optional.empty();
                    Optional<BigInteger> size = Optional.empty();

                    switch(getArgumentCount()) {
                        case 4:
                            size = getIntegerArg(args, 3);

                        case 3:
                            offset = getIntegerArg(args, 2);

                        case 2:
                            final Optional<String> encodingString = getStringArg(args, 1);
                            try {
                                encoding = encodingString.map(Charset::forName);
                            } catch (final UnsupportedCharsetException e) {
                                throw new XPathException(this, ERROR_UNKNOWN_ENCODING, "$encoding is not recognized:" + encodingString.get());
                            }

                        default:
                    }

                    if(offset.isPresent() && offset.get().compareTo(BigInteger.ZERO) < 0) {
                        throw new XPathException(this, ERROR_INDEX_OUT_OF_RANGE, "$offset is negative:" + offset);
                    }

                    if(size.isPresent() && size.get().compareTo(BigInteger.ZERO) < 0) {
                        throw new XPathException(this, ERROR_NEGATIVE_SIZE, "$size is negative:" + offset);
                    }

                    return decode(inBase64.get(), encoding, offset, size);
                } else {
                    return Sequence.EMPTY_SEQUENCE;
                }

            case FS_ENCODE_STRING_NAME:
                final Optional<String> inString = getStringArg(args, 0);
                if(inString.isPresent()) {
                    final Optional<Charset> encoding;
                    if(getArgumentCount() == 2) {
                        final Optional<String> encodingString = getStringArg(args, 1);
                        try {
                            encoding = encodingString.map(Charset::forName);
                        } catch (final UnsupportedCharsetException e) {
                            throw new XPathException(this, ERROR_UNKNOWN_ENCODING, "$encoding is not recognized:" + encodingString.get());
                        }
                    } else {
                        encoding = Optional.empty();
                    }

                    return encode(inString.get(), encoding);

                } else {
                    return Sequence.EMPTY_SEQUENCE;
                }

            default:
                throw new XPathException(this, "No function: " + getName() + "#" + getSignature().getArgumentCount());
        }
    }

    private StringValue decode(final BinaryValue binaryValue, final Optional<Charset> encoding, final Optional<BigInteger> offset, final Optional<BigInteger> size) throws XPathException {
        final InputStream is;
        if(offset.isPresent()) {
            final int off = offset.map(BigInteger::intValue).get();
            is = new RegionFilterInputStream(binaryValue.getInputStream(), off, size.map(BigInteger::intValue).orElse(-1));
        } else {
            is = binaryValue.getInputStream();
        }

        final byte buf[] = new byte[4096];
        try(final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            int read = -1;
            while ((read = is.read(buf)) != -1) {
                baos.write(buf, 0, read);
            }

            return new StringValue(new String(baos.toByteArray(), encoding.orElse(UTF_8)));
        } catch(final RegionFilterInputStream.IndexOutOfRangeException e) {
            throw new XPathException(this, ERROR_INDEX_OUT_OF_RANGE, "$offset + $size is greater than the size of binary data $in");
        } catch(final IOException ioe) {
            throw new XPathException(this, ioe);
        }
    }

    private BinaryValue encode(final String stringValue, final Optional<Charset> encoding) throws XPathException {
        return newInMemoryBinary(context, stringValue.getBytes(encoding.orElse(UTF_8)));
    }
}
