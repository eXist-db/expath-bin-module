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
import org.exist.util.FileUtils;
import org.exist.xquery.XPathException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.CollectionManagementService;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.exist.xquery.modules.expath.bin.TestUtils.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author <a href="mailto: adam@evolvedbinary.com">Adam Retter</a>
 */
public class BasicFunctionsTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existXmldbEmbeddedServer =
            new ExistXmldbEmbeddedServer(false, true);

    private static String TEST_FILE_NAME = "file.bin";
    private static Collection testCollection;
    private static Path binFile;

    @BeforeClass
    public static void setup() throws XMLDBException, IOException {
        final Collection root = existXmldbEmbeddedServer.getRoot();
        try {
            binFile = createRandomDataFile(1024 * 1024 * 100);
            testCollection = existXmldbEmbeddedServer.createCollection(root, TEST_COLLECTION_NAME);
            final Resource resource = testCollection.createResource(TEST_FILE_NAME, BinaryResource.RESOURCE_TYPE);
            resource.setContent(binFile);
            testCollection.storeResource(resource);
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
    public void length() throws XMLDBException {
        final long expectedSize = FileUtils.sizeQuietly(binFile);

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "import module namespace util = \"http://exist-db.org/xquery/util\";\n"
                + "bin:length(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_FILE_NAME + "'))";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(expectedSize, Integer.parseInt(resourceSet.getResource(0).getContent().toString()));
    }

    @Test
    public void part_offset_start() throws XMLDBException, IOException {
        final int offset = 0;

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "import module namespace util = \"http://exist-db.org/xquery/util\";\n"
                + "bin:part(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_FILE_NAME + "'), " + offset + ")";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        final byte[] part = Base64.decode(resourceSet.getResource(0).getContent().toString());

        final byte[] expectedPart = Files.readAllBytes(binFile);

        assertArrayEquals(expectedPart, part);
    }

    @Test
    public void part_offsetAndSize_start() throws XMLDBException, IOException {
        final int offset = 0;
        final int size = 1024 * 1024;

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "import module namespace util = \"http://exist-db.org/xquery/util\";\n"
                + "bin:part(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_FILE_NAME + "'), " + offset + ", " + size + ")";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        final byte[] part = Base64.decode(resourceSet.getResource(0).getContent().toString());

        final byte[] expectedPart = readFilePart(binFile, offset, size);

        assertArrayEquals(expectedPart, part);
    }

    @Test
    public void part_offsetAndSize_mid() throws XMLDBException, IOException {
        final int offset = (int)FileUtils.sizeQuietly(binFile) / 2;
        final int size = 1024 * 1024;

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "import module namespace util = \"http://exist-db.org/xquery/util\";\n"
                + "bin:part(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_FILE_NAME + "'), " + offset + ", " + size + ")";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        final byte[] part = Base64.decode(resourceSet.getResource(0).getContent().toString());

        final byte[] expectedPart = readFilePart(binFile, offset, size);

        assertArrayEquals(expectedPart, part);
    }

    @Test
    public void part_empty() throws XMLDBException, IOException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "bin:part((), 0)";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(0, resourceSet.getSize());
    }

    @Test
    public void part_negative_offset() throws XMLDBException, IOException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "import module namespace util = \"http://exist-db.org/xquery/util\";\n"
                + "bin:part(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_FILE_NAME + "'), -1)";

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
    public void part_negative_size() throws XMLDBException, IOException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "import module namespace util = \"http://exist-db.org/xquery/util\";\n"
                        + "bin:part(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_FILE_NAME + "'), 0, -1)";

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
    public void part_overflow() throws XMLDBException, IOException {
        final int offset = (int)FileUtils.sizeQuietly(binFile) - 256;
        final int size = 1024 * 1024;

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "import module namespace util = \"http://exist-db.org/xquery/util\";\n"
                        + "bin:part(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_FILE_NAME + "'), " + offset + ", " + size + ")";
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
    public void join() throws XMLDBException, UnsupportedEncodingException {
        final String base64Data1 = Base64.encode("oh".getBytes(UTF_8));
        final String base64Data2 = Base64.encode("hai".getBytes(UTF_8));
        final String base64Data3 = Base64.encode("there".getBytes(UTF_8));

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "bin:join((xs:base64Binary(\"" + base64Data1 + "\"), xs:base64Binary(\"" + base64Data2 + "\"), xs:base64Binary(\"" + base64Data3 + "\")))";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        final String actual = new String(Base64.decode(resourceSet.getResource(0).getContent().toString()), UTF_8);
        assertEquals("ohhaithere", actual);
    }

    @Test
    public void join_empty() throws XMLDBException, UnsupportedEncodingException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "bin:join(())";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals("", resourceSet.getResource(0).getContent().toString());
    }

    @Test
    public void insertBefore() throws XMLDBException, UnsupportedEncodingException {
        final String base64Data1 = Base64.encode("world".getBytes(UTF_8));
        final String base64Data2 = Base64.encode("hello".getBytes(UTF_8));

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "bin:insert-before(xs:base64Binary(\"" + base64Data1 + "\"), 0, xs:base64Binary(\"" + base64Data2 + "\"))";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        final String actual = new String(Base64.decode(resourceSet.getResource(0).getContent().toString()), UTF_8);
        assertEquals("helloworld", actual);
    }

    @Test
    public void insertBefore_offset() throws XMLDBException, UnsupportedEncodingException {
        final String base64Data1 = Base64.encode("world".getBytes(UTF_8));
        final String base64Data2 = Base64.encode("hello".getBytes(UTF_8));

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:insert-before(xs:base64Binary(\"" + base64Data1 + "\"), 2, xs:base64Binary(\"" + base64Data2 + "\"))";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        final String actual = new String(Base64.decode(resourceSet.getResource(0).getContent().toString()), UTF_8);
        assertEquals("wohellorld", actual);
    }

    @Test
    public void insertBefore_empty() throws XMLDBException, UnsupportedEncodingException {
        final String base64Data2 = Base64.encode("hello".getBytes(UTF_8));

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:insert-before((), 0, xs:base64Binary(\"" + base64Data2 + "\"))";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(0, resourceSet.getSize());
    }

    @Test
    public void insertBefore_empty_extra() throws XMLDBException, UnsupportedEncodingException {
        final String base64Data1 = Base64.encode("hello".getBytes(UTF_8));

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:insert-before(xs:base64Binary(\"" + base64Data1 + "\"), 0, ())";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        final String actual = new String(Base64.decode(resourceSet.getResource(0).getContent().toString()), UTF_8);
        assertEquals("hello", actual);
    }

    @Test
    public void insertBefore_negative_offset() throws XMLDBException, UnsupportedEncodingException {
        final String base64Data1 = Base64.encode("world".getBytes(UTF_8));
        final String base64Data2 = Base64.encode("hello".getBytes(UTF_8));

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "bin:insert-before(xs:base64Binary(\"" + base64Data1 + "\"), -1, xs:base64Binary(\"" + base64Data2 + "\"))";

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
    public void insertBefore_overflow_offset() throws XMLDBException, UnsupportedEncodingException {
        final String base64Data1 = Base64.encode("world".getBytes(UTF_8));
        final String base64Data2 = Base64.encode("hello".getBytes(UTF_8));

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "bin:insert-before(xs:base64Binary(\"" + base64Data1 + "\"), 100, xs:base64Binary(\"" + base64Data2 + "\"))";

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
    public void padLeft() throws XMLDBException, UnsupportedEncodingException {
        final String base64Data1 = Base64.encode("123456789".getBytes(UTF_8));

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "bin:pad-left(xs:base64Binary(\"" + base64Data1 + "\"), 4)";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        final String actual = new String(Base64.decode(resourceSet.getResource(0).getContent().toString()), UTF_8);
        assertEquals("\0\0\0\0" + "123456789", actual);
    }

    @Test
    public void padLeft_octet() throws XMLDBException, UnsupportedEncodingException {
        final String base64Data1 = Base64.encode("123456789".getBytes(UTF_8));

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "bin:pad-left(xs:base64Binary(\"" + base64Data1 + "\"), 4, 48)";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        final String actual = new String(Base64.decode(resourceSet.getResource(0).getContent().toString()), UTF_8);
        assertEquals("0000123456789", actual);
    }

    @Test
    public void padLeft_empty() throws XMLDBException, UnsupportedEncodingException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "bin:pad-left((), 4)";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(0, resourceSet.getSize());
    }

    @Test
    public void padLeft_negative_size() throws XMLDBException, UnsupportedEncodingException {
        final String base64Data1 = Base64.encode("123456789".getBytes(UTF_8));

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "bin:pad-left(xs:base64Binary(\"" + base64Data1 + "\"), -1)";

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
    public void padLeft_octet_out_of_range() throws XMLDBException, UnsupportedEncodingException {
        final String base64Data1 = Base64.encode("123456789".getBytes(UTF_8));

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "bin:pad-left(xs:base64Binary(\"" + base64Data1 + "\"), 4, 300)";

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

    @Test
    public void padRight() throws XMLDBException, UnsupportedEncodingException {
        final String base64Data1 = Base64.encode("123456789".getBytes(UTF_8));

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:pad-left(xs:base64Binary(\"" + base64Data1 + "\"), 4)";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        final String actual = new String(Base64.decode(resourceSet.getResource(0).getContent().toString()), UTF_8);
        assertEquals("\0\0\0\0" + "123456789", actual);
    }

    @Test
    public void padRight_octet() throws XMLDBException, UnsupportedEncodingException {
        final String base64Data1 = Base64.encode("123456789".getBytes(UTF_8));

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:pad-left(xs:base64Binary(\"" + base64Data1 + "\"), 4, 48)";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        final String actual = new String(Base64.decode(resourceSet.getResource(0).getContent().toString()), UTF_8);
        assertEquals("0000123456789", actual);
    }

    @Test
    public void padRight_empty() throws XMLDBException, UnsupportedEncodingException {
        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:pad-left((), 4)";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(0, resourceSet.getSize());
    }

    @Test
    public void padRight_negative_size() throws XMLDBException, UnsupportedEncodingException {
        final String base64Data1 = Base64.encode("123456789".getBytes(UTF_8));

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:pad-left(xs:base64Binary(\"" + base64Data1 + "\"), -1)";

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
    public void padRight_octet_out_of_range() throws XMLDBException, UnsupportedEncodingException {
        final String base64Data1 = Base64.encode("123456789".getBytes(UTF_8));

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "bin:pad-left(xs:base64Binary(\"" + base64Data1 + "\"), 4, 300)";

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

    private byte[] readFilePart(final Path file, final int offset, final int len) throws IOException {
        final byte[] part = new byte[len];
        try(final InputStream is = Files.newInputStream(file)) {
            long skipped = 0;
            while(skipped < offset) {
                skipped += is.skip(offset - skipped);
            }
            is.read(part);

            return part;
        }
    }
}
