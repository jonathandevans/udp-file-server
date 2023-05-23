
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


public class FileShareServerTest {
    
    private static String expectedMsg = "\nExpected: --port <port> --directory <directory>";

    /**
     * Checks parsePort() using a range of valid arguments.
     * 
     * @throws BadServerArgumentsException thrown on BadServerArgumentException
     */
    @Test
    public void checkParsePort() throws BadServerArgumentsException {
        // Normal value
        FileShareServer.parsePort("1999");
        assertEquals(1999, FileShareServer.port);

        // Extreme values
        FileShareServer.parsePort("1");
        assertEquals(1, FileShareServer.port);
        FileShareServer.parsePort("65535");
        assertEquals(65535, FileShareServer.port);
    }

    /**
     * Checks the correct exception and message is thrown when a string containing
     * non-integers is parsed.
     */
    @Test
    public void checkParsePortUsingNonIntegerString() {
        // Normal values
        Exception e = assertThrows(BadServerArgumentsException.class, () -> FileShareServer.parsePort("seven"));
        assertEquals("Port given is not an integer." + expectedMsg, e.getMessage());
        Exception e1 = assertThrows(BadServerArgumentsException.class, () -> FileShareServer.parsePort("3.3"));
        assertEquals("Port given is not an integer." + expectedMsg, e1.getMessage());
    }

    /**
     * Checks the correct exception and message is thrown when a null value is
     * parsed.
     */
    @Test
    public void checkParsePortUsingNull() {
        // Normal value
        Exception e = assertThrows(BadServerArgumentsException.class, () -> FileShareServer.parsePort(null));
        assertEquals("Port given is not an integer." + expectedMsg, e.getMessage());
    }

    /**
     * Checks the correct exception and message is thrown when a negative value is
     * parsed.
     */
    @Test
    public void checkParsePortUsingNegativeNumber() {
        // Normal value
        Exception e = assertThrows(BadServerArgumentsException.class, () -> FileShareServer.parsePort("-1999"));
        assertEquals("Port number is not valid." + expectedMsg, e.getMessage());

        // Extreme value
        Exception e1 = assertThrows(BadServerArgumentsException.class, () -> FileShareServer.parsePort("-1"));
        assertEquals("Port number is not valid." + expectedMsg, e1.getMessage());
    }

    /**
     * Checks the correct exception and message is thrown when a very large port
     * number is parsed.
     */
    @Test
    public void checkParsePortUsingTooLargeNumber() {
        // Normal value
        Exception e = assertThrows(BadServerArgumentsException.class, () -> FileShareServer.parsePort("65536"));
        assertEquals("Port number is not valid." + expectedMsg, e.getMessage());

        // Extreme value
        Exception e1 = assertThrows(BadServerArgumentsException.class, () -> FileShareServer.parsePort("1000000"));
        assertEquals("Port number is not valid." + expectedMsg, e1.getMessage());
    }

    /**
     * Checks parseDirectory() using a range of valid arguments.
     * 
     * @throws BadServerArgumentsException thrown on BadServerArgumentsException
     */
    @Test
    public void checkParseDirectory() throws BadServerArgumentsException {
        // Normal values
        String directory = "../foo/bar/";
        FileShareServer.parseDirectory(directory);
        assertEquals(directory, FileShareServer.directory);
        directory = "foo";
        FileShareServer.parseDirectory(directory);
        assertEquals(directory, FileShareServer.directory);
        directory = "./bar";
        FileShareServer.parseDirectory(directory);
        assertEquals(directory, FileShareServer.directory);
    }

    /**
     * Checks parseDirectory() throws an exception when an invalid directory is given
     * 
     * @throws BadServerArgumentsException thrown on BadServerArgumentException
     */
    @Test
    public void checkParseDirectoryUsngInvalidDirectoryStructure() throws BadServerArgumentsException {
        // Normal values
        Exception e = assertThrows(BadServerArgumentsException.class, () -> FileShareServer.parseDirectory(""));
        assertEquals("Directory given is not a valid filepath."+ expectedMsg, e.getMessage());
    }
}
