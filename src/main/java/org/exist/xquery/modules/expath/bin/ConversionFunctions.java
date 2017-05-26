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

import org.apache.commons.lang3.StringUtils;
import org.exist.xquery.*;
import org.exist.xquery.value.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;

import static org.exist.xquery.modules.expath.bin.ExpathBinModule.ERROR_NON_NUMERIC_CHARACTER;
import static org.exist.xquery.modules.expath.bin.ExpathBinModule.ERROR_OCTET_OUT_OF_RANGE;
import static org.exist.xquery.modules.expath.bin.ExpathBinModule.functionSignature;
import static org.exist.xquery.modules.expath.bin.FunctionSignatureHelpers.*;
import static org.exist.xquery.modules.expath.bin.Utils.*;

/**
 * @author <a href="mailto: adam@evolvedbinary.com">Adam Retter</a>
 */
public class ConversionFunctions extends BasicFunction {

    private static final String FS_HEX_NAME = "hex";
    static final FunctionSignature FS_HEX = functionSignature(
            FS_HEX_NAME,
            "Returns the binary form of the set of octets written as a sequence of (ASCII) hex digits ([0-9A-Fa-f]).",
            returnsOpt(Type.BASE64_BINARY),
            optParam("in", Type.STRING, "The hex digits")
    );

    private static final String FS_BIN_NAME = "bin";
    static final FunctionSignature FS_BIN = functionSignature(
            FS_BIN_NAME,
            "Returns the binary form of the set of octets written as a sequence of (8-wise) (ASCII) binary digits ([01]).",
            returnsOpt(Type.BASE64_BINARY),
            optParam("in", Type.STRING, "The binary digits")
    );

    private static final String FS_OCTAL_NAME = "octal";
    static final FunctionSignature FS_OCTAL = functionSignature(
            FS_OCTAL_NAME,
            "Returns the binary form of the set of octets written as a sequence of (ASCII) octal digits ([0-7]).",
            returnsOpt(Type.BASE64_BINARY),
            optParam("in", Type.STRING, "The octal digits")
    );

    private static final String FS_TO_OCTETS_NAME = "to-octets";
    static final FunctionSignature FS_TO_OCTETS = functionSignature(
            FS_TO_OCTETS_NAME,
            "Returns binary data as a sequence of octets.",
            returnsOptMany(Type.INTEGER),
            optParam("in", Type.BASE64_BINARY, "The binary data")
    );

    private static final String FS_FROM_OCTETS_NAME = "from-octets";
    static final FunctionSignature FS_FROM_OCTETS = functionSignature(
            FS_FROM_OCTETS_NAME,
            "Converts a sequence of octets into binary data.",
            returns(Type.BASE64_BINARY),
            optManyParam("in", Type.INTEGER, "A sequence of Octets")
    );

    public ConversionFunctions(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        switch(getName().getLocalPart()) {
            case FS_HEX_NAME:
                final Optional<String> inHex = getStringArg(args, 0);
                if(inHex.isPresent()) {
                    return hex(inHex.get());
                } else {
                    return Sequence.EMPTY_SEQUENCE;
                }

            case FS_BIN_NAME:
                final Optional<String> inBin = getStringArg(args, 0);
                if(inBin.isPresent()) {
                    return bin(inBin.get());
                } else {
                    return Sequence.EMPTY_SEQUENCE;
                }

            case FS_OCTAL_NAME:
                final Optional<String> inOct = getStringArg(args, 0);
                if(inOct.isPresent()) {
                    return octal(inOct.get());
                } else {
                    return Sequence.EMPTY_SEQUENCE;
                }

            case FS_TO_OCTETS_NAME:
                final Optional<BinaryValue> inBase64 = getBinaryArg(args, 0);
                if(inBase64.isPresent()) {
                    return toOctets(inBase64.get());
                } else {
                    throw new XPathException(this, "$in argument cannot be absent");
                }

            case FS_FROM_OCTETS_NAME:
                final Optional<BigInteger[]> inOctets = getIntegerSequenceArg(args, 0);
                if(inOctets.isPresent()) {
                    return fromOctets(inOctets.get());
                } else {
                    return newEmptyBinary(context);
                }

            default:
                throw new XPathException(this, "No function named: " + getName());
        }
    }

    private BinaryValue hex(final String hexDigits) throws XPathException {
        try {
            return new BinaryValueFromBinaryString(new HexBinaryValueType(), hexDigits).convertTo(new Base64BinaryValueType());
        } catch (final XPathException e) {
            throw new XPathException(this, ERROR_NON_NUMERIC_CHARACTER, e);
        }
    }

    private BinaryValue bin(final String binaryDigits) throws XPathException {
        int len = (int)Math.ceil(binaryDigits.length() / 8);
        if(binaryDigits.length() > 0 && len == 0) {
            len = 1;
        }

        final byte data[] = new byte[len];
        for(int i = 0; i < len; i++) {
            final int start = i * 8;
            final int end = start + 8 > binaryDigits.length() - 1 ? binaryDigits.length() - 1 : start + 8;
            String bins = binaryDigits.substring(start, end);
            if(bins.length() != 8) {
                bins = StringUtils.leftPad(bins, 8 - bins.length(), '0');
            }

            try {
                final byte b = (byte)Integer.parseUnsignedInt(bins, 2);
                data[i] = b;
            } catch (final NumberFormatException e) {
                throw new XPathException(this, ERROR_NON_NUMERIC_CHARACTER, e);
            }
        }

//        final byte data[] = new BigInteger(binaryDigits, 2).toByteArray();

        return BinaryValueFromInputStream.getInstance(context, new Base64BinaryValueType(), new ByteArrayInputStream(data));

    }

    private BinaryValue octal(final String octalDigits) throws XPathException {
        try {
            final byte data[] = new BigInteger(octalDigits, 8).toByteArray();
            return BinaryValueFromInputStream.getInstance(context, new Base64BinaryValueType(), new ByteArrayInputStream(data));
        } catch(final NumberFormatException e) {
            throw new XPathException(this, ERROR_NON_NUMERIC_CHARACTER, e);
        }
    }

    private Optional<String> getStringArg(final Sequence[] args, final int idx) throws XPathException {
        if(args.length > idx) {
            final Sequence arg = args[idx];
            if (!arg.isEmpty()) {

                final Item item = arg.itemAt(0);
                if (item != null) {
                    return Optional.ofNullable(item.getStringValue());
                }
            }
        }

        return Optional.empty();
    }

    private Sequence toOctets(final BinaryValue binValue) throws XPathException {
        try(final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            binValue.streamBinaryTo(baos);

            final ValueSequence valueSequence = new ValueSequence();

            final byte[] data = baos.toByteArray();
            for(int i = 0; i < data.length; i++) {
              valueSequence.add(new IntegerValue(data[i] & 0xFF));
            }

            return valueSequence;
        } catch (final IOException e) {
            throw new XPathException(this, e);
        }
    }

    private BinaryValue fromOctets(final BigInteger[] octets) throws XPathException {
        final byte data[] = new byte[octets.length];
        for(int i = 0; i < octets.length; i++) {
            final int octet = octets[i].intValue();
            if(octet < 0 || octet > 255) {
                throw new XPathException(this, ERROR_OCTET_OUT_OF_RANGE, "octet at index " + i + " is out of range");
            }
            data[i] = (byte)(octet & 0xff);
        }
        return newInMemoryBinary(context, data);
    }
}
