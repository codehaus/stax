package org.codehaus.stax.test.wstream;

import java.io.*;

import javax.xml.stream.*;

/**
 * Set of unit tests for verifying operation of {@link XMLStreamWriter}
 * in "repairing" mode.
 */
public class TestRepairingWriter
    extends BaseWriterTest
{
    public void testDummy()
        throws IOException, XMLStreamException
    {
        StringWriter strw = new StringWriter();
        XMLStreamWriter w = getRepairingWriter(strw);
        // !!! TBI
    }
}
