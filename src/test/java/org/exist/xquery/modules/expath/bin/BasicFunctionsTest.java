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
import org.exist.xmldb.EXistResource;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.exist.xquery.modules.expath.bin.TestUtils.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class BasicFunctionsTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existXmldbEmbeddedServer =
            new ExistXmldbEmbeddedServer(false, true);

    private static String TEST_BIN_FILE_NAME = "file.bin";
    private static String TEST_IMG_FILE_NAME = "cc0.jpg";
    private static Collection testCollection;
    private static Path binFile;
    private static Path imgFile;

    @BeforeClass
    public static void setup() throws XMLDBException, IOException, URISyntaxException {
        final Collection root = existXmldbEmbeddedServer.getRoot();
        try {
            testCollection = existXmldbEmbeddedServer.createCollection(root, TEST_COLLECTION_NAME);

            binFile = createRandomDataFile(1024 * 1024 * 100);
            final Resource binResource = testCollection.createResource(TEST_BIN_FILE_NAME, BinaryResource.RESOURCE_TYPE);
            binResource.setContent(binFile);
            testCollection.storeResource(binResource);

            imgFile = getTestResource(TEST_IMG_FILE_NAME);
            final Resource imgResource = testCollection.createResource(TEST_IMG_FILE_NAME, BinaryResource.RESOURCE_TYPE);
            ((EXistResource)imgResource).setMimeType("image/jpeg");
            imgResource.setContent(imgFile);
            testCollection.storeResource(imgResource);

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
                + "bin:length(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_BIN_FILE_NAME + "'))";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(expectedSize, Integer.parseInt(resourceSet.getResource(0).getContent().toString()));
    }

    @Test
    public void part_offset_start() throws XMLDBException, IOException {
        final int offset = 0;

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "import module namespace util = \"http://exist-db.org/xquery/util\";\n"
                + "bin:part(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_BIN_FILE_NAME + "'), " + offset + ")";

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
                + "bin:part(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_BIN_FILE_NAME + "'), " + offset + ", " + size + ")";

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
                + "bin:part(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_BIN_FILE_NAME + "'), " + offset + ", " + size + ")";

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
                + "bin:part(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_BIN_FILE_NAME + "'), -1)";

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
                        + "bin:part(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_BIN_FILE_NAME + "'), 0, -1)";

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
        final int fileSize = (int)FileUtils.sizeQuietly(binFile);
        final int offset = fileSize - 256;
        final int size = fileSize * 2;

        final String query =
                "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "import module namespace util = \"http://exist-db.org/xquery/util\";\n"
                        + "bin:part(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_BIN_FILE_NAME + "'), " + offset + ", " + size + ") cast as xs:string";
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
    public void twoParts() throws XMLDBException, IOException {
        final String query =
                "(\n"
                + "    bin:part(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_IMG_FILE_NAME + "'), 0, 10),\n"
                + "    bin:part(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_IMG_FILE_NAME + "'), 50000, 10)\n"
                +")";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(2, resourceSet.getSize());

        final byte[] expectedPart1 = readFilePart(imgFile, 0, 10);
        final byte[] expectedPart2 = readFilePart(imgFile, 50000, 10);
        final byte[] part1 = Base64.decode(resourceSet.getResource(0).getContent().toString());
        final byte[] part2 = Base64.decode(resourceSet.getResource(1).getContent().toString());

        assertArrayEquals(expectedPart1, part1);
        assertArrayEquals(expectedPart2, part2);
    }

    @Test
    public void twoParts_reuseDoc() throws XMLDBException, IOException {
        final String query =
                "let $bin-doc := util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_IMG_FILE_NAME + "')\n"
                + "return (\n"
                + "    bin:part($bin-doc, 0, 10),\n"
                + "    bin:part($bin-doc, 50000, 10)\n"
                +")";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(2, resourceSet.getSize());

        final byte[] expectedPart1 = readFilePart(imgFile, 0, 10);
        final byte[] expectedPart2 = readFilePart(imgFile, 50000, 10);
        final byte[] part1 = Base64.decode(resourceSet.getResource(0).getContent().toString());
        final byte[] part2 = Base64.decode(resourceSet.getResource(1).getContent().toString());

        assertArrayEquals(expectedPart1, part1);
        assertArrayEquals(expectedPart2, part2);
    }

    @Test
    public void parts() throws XMLDBException, IOException {
        final int[] offsets = {1097, 27931, 49288, 98576, 4857600};
        final int size = 21234;

        final StringBuilder query = new StringBuilder()
            .append("import module namespace bin = \"http://expath.org/ns/binary\";\n")
            .append("import module namespace util = \"http://exist-db.org/xquery/util\";\n")
            .append("\n")
            .append("(\n");

            for(int i = 0; i < offsets.length; i++) {
                final int offset = offsets[i];
                query.append("    bin:part(util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_BIN_FILE_NAME + "'), " + offset + ", " + size + ")");
                if(i < offsets.length - 1) {
                    query.append(",");
                }
                query.append("\n");
            }

            query.append(")\n");

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query.toString());
        assertEquals(offsets.length, resourceSet.getSize());

        for(int i = 0; i < offsets.length; i++) {
            final int offset = offsets[i];
            final byte[] expectedPart = readFilePart(binFile, offset, size);
            final byte[] part = Base64.decode(resourceSet.getResource(i).getContent().toString());
            assertArrayEquals(expectedPart, part);
        }
    }

    @Test
    public void parts_reuseBinDoc() throws XMLDBException, IOException {
        final int[] offsets = {1097, 27931, 49288, 98576, 4857600};
        final int size = 21234;

        final StringBuilder query = new StringBuilder()
                .append("import module namespace bin = \"http://expath.org/ns/binary\";\n")
                .append("import module namespace util = \"http://exist-db.org/xquery/util\";\n")
                .append("\n")
                .append("let $bin-doc := util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/" + TEST_BIN_FILE_NAME + "')\n")
                .append("return (\n");

        for(int i = 0; i < offsets.length; i++) {
            final int offset = offsets[i];
            query.append("    bin:part($bin-doc, " + offset + ", " + size + ")");
            if(i < offsets.length - 1) {
                query.append(",");
            }
            query.append("\n");
        }

        query.append(")\n");

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query.toString());
        assertEquals(offsets.length, resourceSet.getSize());

        for(int i = 0; i < offsets.length; i++) {
            final int offset = offsets[i];
            final byte[] expectedPart = readFilePart(binFile, offset, size);
            final byte[] part = Base64.decode(resourceSet.getResource(i).getContent().toString());
            assertArrayEquals("part " + i + " is not correct", expectedPart, part);
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
                + "bin:insert-before(xs:base64Binary(\"" + base64Data1 + "\"), 100, xs:base64Binary(\"" + base64Data2 + "\")) cast as xs:string";

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

    @Test
    public void integration_join_parts() throws XMLDBException, UnsupportedEncodingException {
        final String query =
                "import module namespace util = \"http://exist-db.org/xquery/util\";\n"
                + "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "\n"
                + "let $data := util:base64-encode('ohhaithere!icanhazcheezburger?')\n"
                + "let $part1 := bin:part($data, 0, 2)\n"
                + "let $part2 := bin:part($data, 5, 5)\n"
                + "let $part3 := bin:part($data, 18, 5)\n"
                + "let $part4 := bin:part($data, 10, 1)\n"
                + "return\n"
                + "    bin:join(($part1, $part2, $part3, $part4)) cast as xs:string";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(1, resourceSet.getSize());

        final String actual = new String(Base64.decode(resourceSet.getResource(0).getContent().toString()), UTF_8);
        assertEquals("ohtherecheez!", actual);
    }

    /**
     * Takes a binary file, breaks it into blocks,
     * joins the blocks together and stores them into
     * the database as a single file
     *
     * Compares the checksum of the original and
     * rejoined file to ensure they are the same
     */
    @Test
    public void integration_length_part_join() throws XMLDBException, IOException, URISyntaxException {
        final String query =
                "import module namespace util = \"http://exist-db.org/xquery/util\";\n"
                + "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "import module namespace xmldb = \"http://exist-db.org/xquery/xmldb\";\n"
                + "\n"
                + "let $block-size := 200000\n"
                + "let $local-binary-file-path := '/db/" + TEST_COLLECTION_NAME + "/" + TEST_IMG_FILE_NAME + "'\n"
                + "let $binary-file-data := util:binary-doc($local-binary-file-path)\n"
                + "let $total-size := bin:length($binary-file-data)\n"
                + "\n"
                + "(: Split file into blocks... :)\n"
                + "let $blocks :="
                + "    let $num-blocks := xs:integer(\n"
                + "        if($total-size mod $block-size gt 0)then\n"
                + "            ceiling($total-size div $block-size)\n"
                + "        else\n"
                + "            $total-size div $block-size\n"
                + "    )\n"
                + "\n"
                + "    for $i in (0 to $num-blocks - 1)\n"
                + "    let $block-start := $i * $block-size\n"
                + "    let $block-length :=\n"
                + "        if($block-start + $block-size gt $total-size) then\n"
                + "            $total-size - $block-start\n"
                + "        else\n"
                + "            $block-size\n"
                + "    return\n"
                + "        bin:part($binary-file-data, $block-start, $block-length)\n"
                + "return\n"
                + "    let $local-joined-binary-file-path := xmldb:store('" + TEST_COLLECTION_NAME + "', 'joined_" + TEST_IMG_FILE_NAME + "', bin:join($blocks), 'image/jpeg')\n"
                + "    return\n"
                + "(util:binary-doc($local-binary-file-path) cast as xs:string, util:binary-doc($local-joined-binary-file-path) cast as xs:string)";


        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(2, resourceSet.getSize());

        final byte[] expectedBytes = Files.readAllBytes(getTestResource(TEST_IMG_FILE_NAME));
        final byte[] storedBytes = Base64.decode(resourceSet.getResource(0).getContent().toString());
        final byte[] storedJoinedBytes = Base64.decode(resourceSet.getResource(1).getContent().toString());

        assertArrayEquals(expectedBytes, storedBytes);
        assertArrayEquals(expectedBytes, storedJoinedBytes);
    }

    /**
     * Takes a binary file, breaks it into blocks,
     * records the length of each block,
     * logs the lengths,
     * joins the blocks together and stores them into
     * the database as a single file
     *
     * Compares the checksum of the original and
     * rejoined file to ensure they are the same
     */
    @Test
    public void integration_length_part_length_join() throws XMLDBException, IOException, URISyntaxException {
        final String query =
                "import module namespace util = \"http://exist-db.org/xquery/util\";\n"
                + "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "import module namespace xmldb = \"http://exist-db.org/xquery/xmldb\";\n"
                + "\n"
                + "let $block-size := 1000\n"
                + "let $local-binary-file-path := '/db/" + TEST_COLLECTION_NAME + "/" + TEST_IMG_FILE_NAME + "'\n"
                + "let $binary-file-data := util:binary-doc($local-binary-file-path)\n"
                + "let $total-size := bin:length($binary-file-data)\n"
                + "\n"
                + "let $num-blocks := xs:integer(\n"
                + "    if($total-size mod $block-size gt 0)then\n"
                + "        ceiling($total-size div $block-size)\n"
                + "    else\n"
                + "        $total-size div $block-size\n"
                + ")\n"
                + "\n"
                + "let $blocks-and-lengths :="
                + "    for $i in (0 to $num-blocks - 1)\n"
                + "    let $block-start := $i * $block-size\n"
                + "    let $block-length :=\n"
                + "        if($block-start + $block-size gt $total-size) then\n"
                + "            $total-size - $block-start\n"
                + "        else\n"
                + "            $block-size\n"
                + "    let $part := bin:part($binary-file-data, $block-start, $block-length)\n"
                + "    let $length := bin:length($part)\n"
                + "    return\n"
                + "        ($length, $part)\n"
                + "return\n"
                + "    let $blocks := for $block-idx in (1 to count($blocks-and-lengths))[. mod 2 eq 0] return $blocks-and-lengths[$block-idx]\n"
                + "    let $lengths := for $length-idx in (1 to count($blocks-and-lengths))[. mod 2 ne 0] return $blocks-and-lengths[$length-idx]\n"
                + "    let $local-joined-binary-file-path := xmldb:store('" + TEST_COLLECTION_NAME + "', 'joined_" + TEST_IMG_FILE_NAME + "', bin:join($blocks), 'image/jpeg')\n"
                + "    return\n"
                + "(util:binary-doc($local-binary-file-path) cast as xs:string, util:binary-doc($local-joined-binary-file-path) cast as xs:string, $lengths)";


        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(1090, resourceSet.getSize());

        final byte[] expectedBytes = Files.readAllBytes(getTestResource(TEST_IMG_FILE_NAME));
        final byte[] storedBytes = Base64.decode(resourceSet.getResource(0).getContent().toString());
        final byte[] storedJoinedBytes = Base64.decode(resourceSet.getResource(1).getContent().toString());

        assertArrayEquals(expectedBytes, storedBytes);
        assertArrayEquals(expectedBytes, storedJoinedBytes);

        // check the sizes returned by bin:length
        for(int i = 2; i < 1089; i++) {
            assertEquals(1000, Integer.parseInt(resourceSet.getResource(i).getContent().toString()));
        }
        assertEquals(330, Integer.parseInt(resourceSet.getResource(1089).getContent().toString()));
    }

    @Test
    public void integration_length_part_loop() throws XMLDBException, IOException, URISyntaxException {
        final String query =
                "import module namespace util = \"http://exist-db.org/xquery/util\";\n"
                + "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                + "import module namespace xmldb = \"http://exist-db.org/xquery/xmldb\";\n"
                + "\n"
                + "let $block-size := 1000\n"
                + "let $local-binary-file-path := '/db/" + TEST_COLLECTION_NAME + "/" + TEST_IMG_FILE_NAME + "'\n"
                + "let $binary-file-data := util:binary-doc($local-binary-file-path)\n"
                + "let $total-size := bin:length($binary-file-data)\n"
                + "\n"
                + "let $num-blocks := 3\n"
                + "return\n"
                + "    for $i in (0 to $num-blocks - 1)\n"
                + "    let $block-start := $i * $block-size\n"
                + "    let $block-length :=\n"
                + "        if($block-start + $block-size gt $total-size) then\n"
                + "            $total-size - $block-start\n"
                + "        else\n"
                + "            $block-size\n"
                + "    let $part := bin:part($binary-file-data, $block-start, $block-length)\n"
                + "    return\n"
                + "       $part cast as xs:string";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(3, resourceSet.getSize());

        final byte[] expectedBytes1 = readFilePart(getTestResource(TEST_IMG_FILE_NAME), 0, 1000);
        final byte[] storedBytes1 = Base64.decode(resourceSet.getResource(0).getContent().toString());
        assertArrayEquals(expectedBytes1, storedBytes1);

        final byte[] expectedBytes2 = readFilePart(getTestResource(TEST_IMG_FILE_NAME), 1000, 1000);
        final byte[] storedBytes2 = Base64.decode(resourceSet.getResource(1).getContent().toString());
        assertArrayEquals(expectedBytes2, storedBytes2);

        final byte[] expectedBytes3 = readFilePart(getTestResource(TEST_IMG_FILE_NAME), 2000, 1000);
        final byte[] storedBytes3 = Base64.decode(resourceSet.getResource(2).getContent().toString());
        assertArrayEquals(expectedBytes3, storedBytes3);
    }

    @Test
    public void integration_length_part_length_loop() throws XMLDBException, IOException, URISyntaxException {
        final String query =
                "import module namespace util = \"http://exist-db.org/xquery/util\";\n"
                        + "import module namespace bin = \"http://expath.org/ns/binary\";\n"
                        + "import module namespace xmldb = \"http://exist-db.org/xquery/xmldb\";\n"
                        + "\n"
                        + "let $block-size := 1000\n"
                        + "let $local-binary-file-path := '/db/" + TEST_COLLECTION_NAME + "/" + TEST_IMG_FILE_NAME + "'\n"
                        + "let $binary-file-data := util:binary-doc($local-binary-file-path)\n"
                        + "let $total-size := bin:length($binary-file-data)\n"
                        + "let $num-blocks := 2\n"
                        + "return\n"
                        + "\n"
                        + "    for $i in (0 to $num-blocks - 1)\n"
                        + "    let $block-start := $i * $block-size\n"
                        + "    let $block-length :=\n"
                        + "        if($block-start + $block-size gt $total-size) then\n"
                        + "            $total-size - $block-start\n"
                        + "        else\n"
                        + "            $block-size\n"
                        + "    let $part := bin:part($binary-file-data, $block-start, $block-length)\n"
                        + "    return\n"
                        + "       bin:length($part)";

        final ResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(query);
        assertEquals(2, resourceSet.getSize());
        assertEquals(1000, Long.parseLong(resourceSet.getResource(0).getContent().toString()));
        assertEquals(1000, Long.parseLong(resourceSet.getResource(1).getContent().toString()));
    }

    private static Path getTestResource(final String filename) throws URISyntaxException {
        final URL url = BasicFunctionsTest.class.getClassLoader().getResource(filename);
        return Paths.get(url.toURI());
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
