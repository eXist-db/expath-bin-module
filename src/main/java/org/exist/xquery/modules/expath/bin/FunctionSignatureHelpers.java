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
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;

/**
 * @author <a href="mailto: adam@evolvedbinary.com">Adam Retter</a>
 */
public class FunctionSignatureHelpers {

    //TODO(AR) move to eXist-db

    public static FunctionSignature functionSignature(final QName name, final String description, final FunctionReturnSequenceType returnType, final FunctionParameterSequenceType... paramTypes) {
        return new FunctionSignature(
                name,
                description,
                paramTypes,
                returnType
        );
    }

    public static FunctionParameterSequenceType optParam(final String name, final int type, final String description) {
        return param(name, type, Cardinality.ZERO_OR_ONE, description);
    }

    public static FunctionParameterSequenceType param(final String name, final int type, final String description) {
        return param(name, type, Cardinality.EXACTLY_ONE, description);
    }

    public static FunctionParameterSequenceType manyParam(final String name, final int type, final String description) {
        return param(name, type, Cardinality.ONE_OR_MORE, description);
    }

    public static FunctionParameterSequenceType optManyParam(final String name, final int type, final String description) {
        return param(name, type, Cardinality.ZERO_OR_MORE, description);
    }

    public static FunctionParameterSequenceType param(final String name, final int type, final int cardinality, final String description) {
        return new FunctionParameterSequenceType(name, type, cardinality, description);
    }

    public static FunctionReturnSequenceType returns(final int type) {
        return returns(type, Cardinality.EXACTLY_ONE);
    }

    public static FunctionReturnSequenceType returnsOpt(final int type) {
        return returns(type, Cardinality.ZERO_OR_ONE);
    }

    public static FunctionReturnSequenceType returnsMany(final int type) {
        return returns(type, Cardinality.ONE_OR_MORE);
    }

    public static FunctionReturnSequenceType returnsOptMany(final int type) {
        return returns(type, Cardinality.ZERO_OR_MORE);
    }

    public static FunctionReturnSequenceType returns(final int type, final int cardinality) {
        return returns(type, cardinality, null);
    }

    public static FunctionReturnSequenceType returns(final int type, final int cardinality, final String description) {
        return new FunctionReturnSequenceType(type, cardinality, description);
    }
}
