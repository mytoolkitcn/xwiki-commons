/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.job.internal;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import javax.inject.Provider;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.job.DefaultRequest;
import org.xwiki.job.Request;
import org.xwiki.job.annotation.Serializable;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.job.test.SerializableStandaloneComponent;
import org.xwiki.job.test.StandaloneComponent;

/**
 * Validate {@link JobStatusSerializer}.
 *
 * @version $Id$
 */
public class JobStatusSerializerTest
{
    private JobStatusSerializer serializer;

    private File testFile = new File("target/test/status.xml");

    private static class CrossReferenceObjectTest
    {
        public CrossReferenceObjectTest field;

        public CrossReferenceObjectTest()
        {
            this.field = this;
        }
    }

    private static class ObjectTest
    {
        public Object field;

        public ObjectTest(Object field)
        {
            this.field = field;
        }
    }

    private static class CustomObject
    {
        public String field;

        public CustomObject(String field)
        {
            this.field = field;
        }

        @Override
        public boolean equals(Object obj)
        {
            return Objects.equals(((CustomObject) obj).field, this.field);
        }
    }

    @Serializable
    private static class SerializableCustomObject
    {
        public String field;

        public SerializableCustomObject(String field)
        {
            this.field = field;
        }

        @Override
        public boolean equals(Object obj)
        {
            return Objects.equals(((SerializableCustomObject) obj).field, this.field);
        }
    }

    @Serializable(false)
    private static class NotSerializableCustomObject
    {
        public String field;

        public NotSerializableCustomObject(String field)
        {
            this.field = field;
        }

        @Override
        public boolean equals(Object obj)
        {
            return Objects.equals(((NotSerializableCustomObject) obj).field, this.field);
        }

        @Override
        public String toString()
        {
            return this.field;
        }
    }

    @Serializable
    private static class SerializableProvider implements Provider<String>
    {
        @Override
        public String get()
        {
            return null;
        }
    }

    private static class SerializableImplementationProvider implements Provider<String>, java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public String get()
        {
            return null;
        }
    }

    @Before
    public void before() throws ParserConfigurationException
    {
        this.serializer = new JobStatusSerializer();
    }

    private JobStatus writeread(JobStatus status) throws IOException
    {
        this.serializer.write(status, this.testFile);

        return this.serializer.read(this.testFile);
    }

    // Tests

    @Test
    public void test() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        writeread(status);
    }

    @Test
    public void testLog() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("error message");

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertEquals("error message", status.getLog().peek().getMessage());
    }

    @Test
    public void testLogWithException() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("error message", new Exception("exception message", new Exception("cause")));

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertEquals("error message", status.getLog().peek().getMessage());
        Assert.assertEquals("exception message", status.getLog().peek().getThrowable().getMessage());
    }

    @Test
    public void testLogWithArguments() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("error message", "arg1", "arg2");

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertEquals("error message", status.getLog().peek().getMessage());
        Assert.assertEquals("arg1", status.getLog().peek().getArgumentArray()[0]);
        Assert.assertEquals("arg2", status.getLog().peek().getArgumentArray()[1]);
    }

    @Test
    public void testLogWithComponentArgument() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("error message", new DefaultJobStatusStore());

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertEquals("error message", status.getLog().peek().getMessage());
        Assert.assertEquals(String.class, status.getLog().peek().getArgumentArray()[0].getClass());
    }

    @Test
    public void testLogWithStandaloneComponentArgument() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("error message", new StandaloneComponent());

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertEquals("error message", status.getLog().peek().getMessage());
        Assert.assertEquals(String.class, status.getLog().peek().getArgumentArray()[0].getClass());
    }

    @Test
    public void testLogWithSerializableStandaloneComponentArgument() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("error message", new SerializableStandaloneComponent());

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertEquals("error message", status.getLog().peek().getMessage());
        Assert.assertEquals(SerializableStandaloneComponent.class,
            status.getLog().peek().getArgumentArray()[0].getClass());
    }

    @Test
    public void testLogWithCrossReference() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("message", new CrossReferenceObjectTest());

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertNull(((CrossReferenceObjectTest) status.getLog().peek().getArgumentArray()[0]).field);
    }

    @Test
    public void testLogWithComponentField() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("error message", new ObjectTest(new DefaultJobStatusStore()));

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertNull(((ObjectTest) status.getLog().peek().getArgumentArray()[0]).field);
    }

    @Test
    public void testLogWithStandaloneComponentField() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("error message", new ObjectTest(new StandaloneComponent()));

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertNull(((ObjectTest) status.getLog().peek().getArgumentArray()[0]).field);
    }

    @Test
    public void testLogWithLoggerField() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("error message", new ObjectTest(mock(Logger.class)));

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertNull(((ObjectTest) status.getLog().peek().getArgumentArray()[0]).field);
    }

    @Test
    public void testLogWithProviderField() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("error message", new ObjectTest(mock(Provider.class)));

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertNull(((ObjectTest) status.getLog().peek().getArgumentArray()[0]).field);
    }

    @Test
    public void testLogWithComponentManagerField() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("error message", new ObjectTest(mock(ComponentManager.class)));

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertNull(((ObjectTest) status.getLog().peek().getArgumentArray()[0]).field);
    }

    @Test
    public void testLogWithSerializableProviderField() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("error message", new ObjectTest(new SerializableProvider()));

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertEquals("error message", status.getLog().peek().getMessage());
        Assert.assertEquals(SerializableProvider.class,
            ((ObjectTest) status.getLog().peek().getArgumentArray()[0]).field.getClass());
    }

    @Test
    public void testLogWithSerializableImplementationProviderField() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("error message", new ObjectTest(new SerializableImplementationProvider()));

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertEquals("error message", status.getLog().peek().getMessage());
        Assert.assertEquals(SerializableImplementationProvider.class, ((ObjectTest) status.getLog().peek()
            .getArgumentArray()[0]).field.getClass());
    }

    @Test
    public void testLogWithCustomObjectArgument() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("error message", new CustomObject("value"));

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertEquals("error message", status.getLog().peek().getMessage());
        Assert.assertEquals(new CustomObject("value"), status.getLog().peek().getArgumentArray()[0]);
    }

    @Test
    public void testLogWithSerializableCustomObjectArgument() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("error message", new SerializableCustomObject("value"));

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertEquals("error message", status.getLog().peek().getMessage());
        Assert.assertEquals(new SerializableCustomObject("value"), status.getLog().peek().getArgumentArray()[0]);
    }

    @Test
    public void testLogWithNotSerializableCustomObjectArgument() throws IOException
    {
        JobStatus status = new DefaultJobStatus<Request>(new DefaultRequest(), null, null, false);

        status.getLog().error("error message", new NotSerializableCustomObject("value"));

        status = writeread(status);

        Assert.assertNotNull(status.getLog());
        Assert.assertEquals("error message", status.getLog().peek().getMessage());
        Assert.assertEquals("value", status.getLog().peek().getArgumentArray()[0]);
    }
}
