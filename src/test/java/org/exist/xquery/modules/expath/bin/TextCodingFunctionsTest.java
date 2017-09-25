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

import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xquery.XPathException;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class TextCodingFunctionsTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existXmldbEmbeddedServer =
            new ExistXmldbEmbeddedServer(false, true);

    @Test
    public void decode() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "bin:decode-string(xs:base64Binary('b2hkZWFyd2hhdHdlbnR3cm9uZw=='))";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(1, resourceSet.getSize());

        assertEquals("ohdearwhatwentwrong", resourceSet.getResource(0).getContent().toString());
    }

    @Test
    public void decode_empty() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "bin:decode-string(())";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(0, resourceSet.getSize());
    }

    @Test
    public void decode_encoding() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:decode-string(xs:base64Binary('iaaWpJOEk4mShaOWo5mogYeBiZU='), 'CP037')";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(1, resourceSet.getSize());

        assertEquals("iwouldliketotryagain", resourceSet.getResource(0).getContent().toString());
    }

    @Test
    public void decode_unknown_encoding() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:decode-string(xs:base64Binary('b2hkZWFyd2hhdHdlbnR3cm9uZw=='), 'LIZBERT')";

        try {
            existXmldbEmbeddedServer.executeQuery(query);
            fail("Expected error bin:unknown-encoding");
        } catch(final XMLDBException e) {
            final Throwable cause = e.getCause();
            if(cause instanceof XPathException) {
                final XPathException xpe = ((XPathException)cause);
                assertEquals(ExpathBinModule.ERROR_UNKNOWN_ENCODING, xpe.getErrorCode());
            } else {
                fail("Expected error bin:unknown-encoding");
            }
        }
    }

    @Test
    public void decode_offset() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:decode-string(xs:base64Binary('b2hkZWFyd2hhdHdlbnR3cm9uZw=='), 'UTF-8', 6)";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(1, resourceSet.getSize());

        assertEquals("whatwentwrong", resourceSet.getResource(0).getContent().toString());
    }

    @Test
    public void decode_offset_size() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:decode-string(xs:base64Binary('b2hkZWFyd2hhdHdlbnR3cm9uZw=='), 'UTF-8', 6, 4)";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(1, resourceSet.getSize());

        assertEquals("what", resourceSet.getResource(0).getContent().toString());
    }

    @Test
    public void decode_negative_offset() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "bin:decode-string(xs:base64Binary('b2hkZWFyd2hhdHdlbnR3cm9uZw=='), 'UTF-8', -7)";

        try {
            existXmldbEmbeddedServer.executeQuery(query);
            fail("Expected error bin:index-out-of-range");
        } catch(final XMLDBException e) {
            final Throwable cause = e.getCause();
            if(cause instanceof XPathException) {
                final XPathException xpe = ((XPathException)cause);
                assertEquals(ExpathBinModule.ERROR_INDEX_OUT_OF_RANGE, xpe.getErrorCode());
            } else {
                fail("Expected error bin:index-out-of-range");
            }
        }
    }

    @Test
    public void decode_negative_size() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:decode-string(xs:base64Binary('b2hkZWFyd2hhdHdlbnR3cm9uZw=='), 'UTF-8', 7, -4)";

        try {
            existXmldbEmbeddedServer.executeQuery(query);
            fail("Expected error bin:negative-size");
        } catch(final XMLDBException e) {
            final Throwable cause = e.getCause();
            if(cause instanceof XPathException) {
                final XPathException xpe = ((XPathException)cause);
                assertEquals(ExpathBinModule.ERROR_NEGATIVE_SIZE, xpe.getErrorCode());
            } else {
                fail("Expected error bin:negative-size");
            }
        }
    }

    @Test
    public void decode_offset_size_overflow() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:decode-string(xs:base64Binary('b2hkZWFyd2hhdHdlbnR3cm9uZw=='), 'UTF-8', 6, 25)";

        try {
            final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
            fail("Expected error bin:index-out-of-range");
        } catch(final XMLDBException e) {
            final Throwable cause = e.getCause();
            if(cause instanceof XPathException) {
                final XPathException xpe = ((XPathException)cause);
                assertEquals(ExpathBinModule.ERROR_INDEX_OUT_OF_RANGE, xpe.getErrorCode());
            } else {
                fail("Expected error bin:index-out-of-range");
            }
        }
    }

    @Test
    public void encode() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:encode-string('ohdearwhatwentwrong')";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(1, resourceSet.getSize());

        assertEquals("b2hkZWFyd2hhdHdlbnR3cm9uZw==", resourceSet.getResource(0).getContent().toString());
    }

    @Test
    public void encode_empty() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:encode-string(())";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(0, resourceSet.getSize());
    }

    @Test
    public void encode_encoding() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:encode-string('iwouldliketotryagain', 'CP037')";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(1, resourceSet.getSize());

        assertEquals("iaaWpJOEk4mShaOWo5mogYeBiZU=", resourceSet.getResource(0).getContent().toString());
    }

    @Test
    public void encode_unknown_encoding() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:encode-string('iwouldliketotryagain', 'LIZBERTASAUROUS')";

        try {
            existXmldbEmbeddedServer.executeQuery(query);
            fail("Expected error bin:unknown-encoding");
        } catch(final XMLDBException e) {
            final Throwable cause = e.getCause();
            if(cause instanceof XPathException) {
                final XPathException xpe = ((XPathException)cause);
                assertEquals(ExpathBinModule.ERROR_UNKNOWN_ENCODING, xpe.getErrorCode());
            } else {
                fail("Expected error bin:unknown-encoding");
            }
        }
    }
}
