package one.jodi.core.service.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransformationFileVisitorTest {
    private static final String ROOT_DIR = File.separator + "root" + File.separator;

    private ErrorWarningMessageJodi errorWarningMessages;
    private BasicFileAttributes attributes;
    private TransformationFileVisitor fixture;

    @Before
    public void setUp() {
        this.errorWarningMessages = ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();
        this.attributes = mock(BasicFileAttributes.class);
        this.fixture = new TransformationFileVisitor(errorWarningMessages);
    }

    @Test
    public void testCollectInPathWithVisitorDir() {
        Path path = Paths.get(ROOT_DIR + "SomeDir");
        try {
            when(attributes.isDirectory()).thenReturn(true);
            FileVisitResult result = fixture.visitFile(path, attributes);
            assertEquals(FileVisitResult.CONTINUE, result);
            assertNotNull(fixture.getPathList());
            assertTrue(fixture.getPathList()
                              .isEmpty());
        } catch (IOException e) {
            fail("Exception was not expected");
        }
    }

    @Test
    public void testCollectInPathWithVisitorNameNoMatch() {
        Path path = Paths.get(ROOT_DIR + "Something");
        try {
            FileVisitResult result = fixture.visitFile(path, attributes);
            assertEquals(FileVisitResult.CONTINUE, result);
            assertNotNull(fixture.getPathList());
            assertTrue(fixture.getPathList()
                              .isEmpty());
        } catch (IOException e) {
            fail("Exception was not expected");
        }
    }

    @Test
    public void testCollectInPathWithVisitorNameOtherXml() {
        Path path = Paths.get(ROOT_DIR + "Procedure.xml");
        try {
            FileVisitResult result = fixture.visitFile(path, attributes);
            assertEquals(FileVisitResult.CONTINUE, result);
            assertNotNull(fixture.getPathList());
            assertTrue(fixture.getPathList()
                              .isEmpty());
        } catch (IOException e) {
            fail("Exception was not expected");
        }
    }

    @Test
    public void testCollectInPathWithVisitorNameSpaceSpecialChar() {
        Path path = Paths.get(ROOT_DIR + "Some Other File #4.xml");
        try {
            FileVisitResult result = fixture.visitFile(path, attributes);
            assertEquals(FileVisitResult.CONTINUE, result);
            assertNotNull(fixture.getPathList());
            assertTrue(fixture.getPathList()
                              .isEmpty());
        } catch (IOException e) {
            fail("Exception was not expected");
        }
    }

    @Test
    public void testCollectInPathWithVisitorNameOtherXml2() {
        Path path = Paths.get(ROOT_DIR + "Procedure-2.xml");
        try {
            FileVisitResult result = fixture.visitFile(path, attributes);
            assertEquals(FileVisitResult.CONTINUE, result);
            assertNotNull(fixture.getPathList());
            assertTrue(fixture.getPathList()
                              .isEmpty());
        } catch (IOException e) {
            fail("Exception was not expected");
        }
    }

    @Test
    public void testCollectInPathWithVisitorName1LeadNumber() {
        Path path = Paths.get(ROOT_DIR + "1_Transform-something.xml");
        try {
            FileVisitResult result = fixture.visitFile(path, attributes);
            assertEquals(FileVisitResult.CONTINUE, result);
            assertNotNull(fixture.getPathList());
            assertTrue(fixture.getPathList()
                              .isEmpty());
        } catch (IOException e) {
            fail("Exception was not expected");
        }
    }

    @Test
    public void testCollectInPathWithVisitorName2LeadingNumbers() {
        Path path = Paths.get(ROOT_DIR + "12_Trans_Something.xml");
        try {
            FileVisitResult result = fixture.visitFile(path, attributes);
            assertEquals(FileVisitResult.CONTINUE, result);
            assertNotNull(fixture.getPathList());
            assertFalse(fixture.getPathList()
                               .isEmpty());
            assertEquals(1, fixture.getPathList()
                                   .size());
        } catch (IOException e) {
            fail("Exception was not expected");
        }
    }

    @Test
    public void testCollectInPathWithVisitorName10LeadingNumbers() {
        Path path = Paths.get(ROOT_DIR + "1234567890_Trans_Something.xml");
        try {
            FileVisitResult result = fixture.visitFile(path, attributes);
            assertEquals(FileVisitResult.CONTINUE, result);
            assertNotNull(fixture.getPathList());
            assertTrue(fixture.getPathList()
                              .isEmpty());
        } catch (IOException e) {
            fail("Exception was not expected");
        }
    }

    @Test
    public void testCollectInPathWithVisitorNameLoadPlan() {
        Path path = Paths.get(ROOT_DIR + File.separator + "loadPlans" + File.separator + "123_Trans_Something.xml");
        try {
            FileVisitResult result = fixture.visitFile(path, attributes);
            assertEquals(FileVisitResult.CONTINUE, result);
            assertNotNull(fixture.getPathList());
            assertTrue(fixture.getPathList()
                              .isEmpty());
        } catch (IOException e) {
            fail("Exception was not expected");
        }
    }

}
