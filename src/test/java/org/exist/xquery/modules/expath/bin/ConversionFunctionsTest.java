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

import gnu.crypto.util.Base64;
import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xquery.XPathException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.xquery.modules.expath.bin.TestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ConversionFunctionsTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existXmldbEmbeddedServer =
            new ExistXmldbEmbeddedServer(false, true);

    private static Collection testCollection;

    @BeforeClass
    public static void setup() throws XMLDBException, IOException {
        final Collection root = existXmldbEmbeddedServer.getRoot();
        try {
            testCollection = existXmldbEmbeddedServer.createCollection(root, TEST_COLLECTION_NAME);
        } finally {
            root.close();
        }
    }

    @AfterClass
    public static void cleanup() throws XMLDBException {
        final CollectionManagementService collectionManagementService = (CollectionManagementService)testCollection.getParentCollection().getService("CollectionManagementService", "1.0");
        collectionManagementService.removeCollection(TEST_COLLECTION_NAME);
    }

    @Test
    public void hex() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "bin:hex(\"11223F4E\")";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals("ESI/Tg==", resourceSet.getResource(0).getContent());
    }

    @Test
    public void hex_empty() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:hex(())";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(0, resourceSet.getSize());
    }

    @Test
    public void hex_nonNumeric() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:hex(\"ZZ\")";

        try {
            existXmldbEmbeddedServer.executeQuery(query);
            fail("Expected error bin:non-numeric-character");
        } catch(final XMLDBException e) {
            final Throwable cause = e.getCause();
            if(cause instanceof XPathException) {
                final XPathException xpe = ((XPathException)cause);
                assertEquals(ExpathBinModule.ERROR_NON_NUMERIC_CHARACTER, xpe.getErrorCode());
            } else {
                fail("Expected error bin:non-numeric-character");
            }
        }
    }

    @Test
    public void bin() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:bin(\"1101000111010101\")";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals("0dU=", resourceSet.getResource(0).getContent());
    }

    @Test
    public void bin_2() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:bin(\"1000111010101\")";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals("EdU=", resourceSet.getResource(0).getContent());
    }

    @Test
    public void bin_3() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:bin(\"1101000111010101100011101010111010101000000101\")";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals("NHVjq6oF", resourceSet.getResource(0).getContent());
    }

    @Test
    public void bin_empty() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:bin(())";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(0, resourceSet.getSize());
    }

    @Test
    public void bin_nonNumeric() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:bin(\"ZZ\")";

        try {
            existXmldbEmbeddedServer.executeQuery(query);
            fail("Expected error bin:non-numeric-character");
        } catch(final XMLDBException e) {
            final Throwable cause = e.getCause();
            if(cause instanceof XPathException) {
                final XPathException xpe = ((XPathException)cause);
                assertEquals(ExpathBinModule.ERROR_NON_NUMERIC_CHARACTER, xpe.getErrorCode());
            } else {
                fail("Expected error bin:non-numeric-character");
            }
        }
    }

    @Test
    public void octal() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:octal(\"11223047\")";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals("JSYn", resourceSet.getResource(0).getContent());
    }

    @Test
    public void octal_empty() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:octal(())";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(0, resourceSet.getSize());
    }

    @Test
    public void octal_nonNumeric() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:octal(\"ZZ\")";

        try {
            existXmldbEmbeddedServer.executeQuery(query);
            fail("Expected error bin:non-numeric-character");
        } catch(final XMLDBException e) {
            final Throwable cause = e.getCause();
            if(cause instanceof XPathException) {
                final XPathException xpe = ((XPathException)cause);
                assertEquals(ExpathBinModule.ERROR_NON_NUMERIC_CHARACTER, xpe.getErrorCode());
            } else {
                fail("Expected error bin:non-numeric-character");
            }
        }
    }

    @Test
    public void toOctets() throws XMLDBException {
        final String base64Data = Base64.encode("hello".getBytes(UTF_8));
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:to-octets(xs:base64Binary(\"" + base64Data + "\"))";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(5, resourceSet.getSize());
        assertEquals(104, Integer.parseInt(resourceSet.getResource(0).getContent().toString()));
        assertEquals(101, Integer.parseInt(resourceSet.getResource(1).getContent().toString()));
        assertEquals(108, Integer.parseInt(resourceSet.getResource(2).getContent().toString()));
        assertEquals(108, Integer.parseInt(resourceSet.getResource(3).getContent().toString()));
        assertEquals(111, Integer.parseInt(resourceSet.getResource(4).getContent().toString()));
    }

    @Test
    public void toOctets_empty() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:to-octets(xs:base64Binary(\"\"))";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(0, resourceSet.getSize());
    }

    @Test
    public void fromOctets() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:from-octets((104, 101, 108, 108, 111))";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals("aGVsbG8=", resourceSet.getResource(0).getContent());
    }

    @Test
    public void fromOctets_empty() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:from-octets(())";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals("", resourceSet.getResource(0).getContent());
    }

    @Test
    public void fromOctets_octetOutOfRange() throws XMLDBException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:from-octets((256,-1))";

        try {
            existXmldbEmbeddedServer.executeQuery(query);
            fail("Expected error bin:octet-out-of-range");
        } catch(final XMLDBException e) {
            final Throwable cause = e.getCause();
            if(cause instanceof XPathException) {
                final XPathException xpe = ((XPathException)cause);
                assertEquals(ExpathBinModule.ERROR_OCTET_OUT_OF_RANGE, xpe.getErrorCode());
            } else {
                fail("Expected error bin:octet-out-of-range");
            }
        }
    }
}
