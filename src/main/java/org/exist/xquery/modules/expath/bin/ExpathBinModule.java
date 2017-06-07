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

import org.exist.dom.QName;
import org.exist.xquery.AbstractInternalModule;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.FunctionDef;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.exist.xquery.modules.expath.bin.FunctionSignatureHelpers.functionDefs;

/**
 * Implementation of
 * <a href="http://expath.org/spec/binary">EXPath Bin Module</a> 1.0
 * for eXist-db
 *
 * @author <a href="mailto: adam@evolvedbinary.com">Adam Retter</a>
 */
public class ExpathBinModule extends AbstractInternalModule {

    public static final String NAMESPACE_URI = "http://expath.org/ns/binary";
    public static final String PREFIX = "bin";
    public static final String RELEASED_IN_VERSION = "eXist-3.2.1";

    public static final FunctionDef[] functions = functionDefs(
            functionDefs(ConversionFunctions.class,
                    ConversionFunctions.FS_HEX,
                    ConversionFunctions.FS_BIN,
                    ConversionFunctions.FS_OCTAL,
                    ConversionFunctions.FS_TO_OCTETS,
                    ConversionFunctions.FS_FROM_OCTETS),

            functionDefs(BasicFunctions.class,
                    BasicFunctions.FS_LENGTH,
                    BasicFunctions.FS_PART[0],
                    BasicFunctions.FS_PART[1],
                    BasicFunctions.FS_JOIN,
                    BasicFunctions.FS_INSERT_BEFORE,
                    BasicFunctions.FS_PAD_LEFT[0],
                    BasicFunctions.FS_PAD_LEFT[1],
                    BasicFunctions.FS_PAD_RIGHT[0],
                    BasicFunctions.FS_PAD_RIGHT[1],
                    BasicFunctions.FS_FIND),

            functionDefs(TextCodingFunctions.class,
                    TextCodingFunctions.FS_DECODE_STRING[0],
                    TextCodingFunctions.FS_DECODE_STRING[1],
                    TextCodingFunctions.FS_DECODE_STRING[2],
                    TextCodingFunctions.FS_DECODE_STRING[3],
                    TextCodingFunctions.FS_ENCODE_STRING[0],
                    TextCodingFunctions.FS_ENCODE_STRING[1])
    );

    public ExpathBinModule(final Map<String, List<? extends Object>> parameters) {
        super(functions, parameters);
    }

    @Override
    public String getNamespaceURI() {
        return NAMESPACE_URI;
    }

    @Override
    public String getDefaultPrefix() {
        return PREFIX;
    }

    @Override
    public String getDescription() {
        return "EXPath Bin Module for eXist-db XQuery";
    }

    @Override
    public String getReleaseVersion() {
        return RELEASED_IN_VERSION;
    }

    static FunctionSignature functionSignature(final String name, final String description, final FunctionReturnSequenceType returnType, final FunctionParameterSequenceType... paramTypes) {
        return FunctionSignatureHelpers.functionSignature(new QName(name, NAMESPACE_URI), description, returnType, paramTypes);
    }

    static FunctionSignature[] functionSignatures(final String name, final String description, final FunctionReturnSequenceType returnType, final FunctionParameterSequenceType[][] variableParamTypes) {
        return Arrays.stream(variableParamTypes)
                .map(paramTypes -> functionSignature(name, description, returnType, paramTypes))
                .toArray(FunctionSignature[]::new);
    }

    static class ExpathBinModuleErrorCode extends ErrorCodes.ErrorCode {
        private ExpathBinModuleErrorCode(final String code, final String description) {
            super(new QName(code, NAMESPACE_URI, PREFIX), description);
        }
    }

    static final ErrorCodes.ErrorCode ERROR_DIFFERENING_LENGTH_ARGUMENTS = new ExpathBinModuleErrorCode("differing-length-arguments", "The arguments to a bitwise operation are of differing length.");
    static final ErrorCodes.ErrorCode ERROR_INDEX_OUT_OF_RANGE = new ExpathBinModuleErrorCode("index-out-of-range", "Attempting to retrieve data outside the meaningful range of a binary data type.");
    static final ErrorCodes.ErrorCode ERROR_NEGATIVE_SIZE = new ExpathBinModuleErrorCode("negative-size", "Size of binary portion, required numeric size or padding is negative.");
    static final ErrorCodes.ErrorCode ERROR_OCTET_OUT_OF_RANGE = new ExpathBinModuleErrorCode("octet-out-of-range", "Attempting to pack binary value with octet outside range.");
    static final ErrorCodes.ErrorCode ERROR_NON_NUMERIC_CHARACTER = new ExpathBinModuleErrorCode("non-numeric-character", "Wrong character in binary 'numeric constructor' string.");
    static final ErrorCodes.ErrorCode ERROR_UNKNOWN_ENCODING = new ExpathBinModuleErrorCode("unknown-encoding", "The specified encoding is not supported.");
    static final ErrorCodes.ErrorCode ERROR_CONVERSION_ERROR = new ExpathBinModuleErrorCode("conversion-error", "Error in converting to/from a string.");
    static final ErrorCodes.ErrorCode ERROR_UNKNOWN_SIGNIFICANCE_ORDER = new ExpathBinModuleErrorCode("unknown-significance-order", "Unknown octet-order value.");
}
