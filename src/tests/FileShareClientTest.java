
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * JUnit tests for {@code FileShareClient}.
 * 
 * @author 210016688
 */
public class FileShareClientTest {

    private static String expectedMsg = "\nExpected: --port <port> --address <server> --get <filename>\n          --port <port> --address <server> --filelist\n          --port <port> --address <server> --rate <filename> <score>";

    /**
     * Checks parsePort() using a range of valid arguments.
     * 
     * @throws BadClientArgumentsException thrown on BadClientArgumentException
     */
    @Test
    public void checkParsePort() throws BadClientArgumentsException {
        // Normal value
        FileShareClient.parsePort("1999");
        assertEquals(1999, FileShareClient.port);

        // Extreme values
        FileShareClient.parsePort("1");
        assertEquals(1, FileShareClient.port);
        FileShareClient.parsePort("65535");
        assertEquals(65535, FileShareClient.port);
    }

    /**
     * Checks the correct exception and message is thrown when a string containing
     * non-integers is parsed.
     */
    @Test
    public void checkParsePortUsingNonIntegerString() {
        // Normal values
        Exception e = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parsePort("seven"));
        assertEquals("Port given is not an integer." + expectedMsg, e.getMessage());
        Exception e1 = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parsePort("3.3"));
        assertEquals("Port given is not an integer." + expectedMsg, e1.getMessage());
    }

    /**
     * Checks the correct exception and message is thrown when a null value is
     * parsed.
     */
    @Test
    public void checkParsePortUsingNull() {
        // Normal value
        Exception e = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parsePort(null));
        assertEquals("Port given is not an integer." + expectedMsg, e.getMessage());
    }

    /**
     * Checks the correct exception and message is thrown when a negative value is
     * parsed.
     */
    @Test
    public void checkParsePortUsingNegativeNumber() {
        // Normal value
        Exception e = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parsePort("-1999"));
        assertEquals("Port number is not valid." + expectedMsg, e.getMessage());

        // Extreme value
        Exception e1 = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parsePort("-1"));
        assertEquals("Port number is not valid." + expectedMsg, e1.getMessage());
    }

    /**
     * Checks the correct exception and message is thrown when a very large port
     * number is parsed.
     */
    @Test
    public void checkParsePortUsingTooLargeNumber() {
        // Normal value
        Exception e = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parsePort("65536"));
        assertEquals("Port number is not valid." + expectedMsg, e.getMessage());

        // Extreme value
        Exception e1 = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parsePort("1000000"));
        assertEquals("Port number is not valid." + expectedMsg, e1.getMessage());
    }

    /**
     * Checks parseScore() using a range of valid arguments.
     * 
     * @throws BadClientArgumentsException thrown on BadClientArgumentsException
     */
    @Test
    public void checkParseScore() throws BadClientArgumentsException {
        // Normal value
        String score = "5";
        assertEquals(score, FileShareClient.parseScore(score));

        // Extreme values
        score = "0";
        assertEquals(score, FileShareClient.parseScore(score));
        score = "10";
        assertEquals(score, FileShareClient.parseScore(score));
    }

    /**
     * Checks the correct exception and message is thrown when a string containing
     * non-integers is parsed.
     */
    @Test
    public void checkParseScoreUsingNonIntegerString() {
        // Normal values
        Exception e = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parseScore("seven"));
        assertEquals("Score given is not an integer." + expectedMsg, e.getMessage());
        Exception e1 = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parseScore("3.3"));
        assertEquals("Score given is not an integer." + expectedMsg, e1.getMessage());
    }

    /**
     * Checks the correct exception and message is thrown when a null value is
     * parsed.
     */
    @Test
    public void checkParseScoreUsingNull() {
        // Normal value
        Exception e = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parseScore(null));
        assertEquals("Score given is not an integer." + expectedMsg, e.getMessage());
    }

    /**
     * Checks the correct exception and message is thrown when a negative value is
     * parsed.
     */
    @Test
    public void checkParseScoreUsingNegativeNumber() {
        // Normal value
        Exception e = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parseScore("-7"));
        assertEquals("Score given is outside the range 0-10." + expectedMsg, e.getMessage());

        // Extreme value
        Exception e1 = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parseScore("-1"));
        assertEquals("Score given is outside the range 0-10." + expectedMsg, e1.getMessage());
    }

    /**
     * Checks the correct exception and message is thrown when a very large port
     * number is parsed.
     */
    @Test
    public void checkParseScoreUsingTooLargeNumber() {
        // Normal value
        Exception e = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parseScore("11"));
        assertEquals("Score given is outside the range 0-10." + expectedMsg, e.getMessage());

        // Extreme value
        Exception e1 = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parseScore("1000000"));
        assertEquals("Score given is outside the range 0-10." + expectedMsg, e1.getMessage());
    }

    /**
     * Checks parseArguments() using a range of valid arguments.
     * 
     * @throws BadClientArgumentsException thrown on BadClientArgumentException
     */
    @Test
    public void checkParseArguments() throws BadClientArgumentsException {
        String[] args = { "--port", "1999", "--filelist", "--address", "address" };
        FileShareClient.parseArguments(args);
        String[] args = { "--port", "1999", "--rate", "littlewomen.txt", "5", "--address", "address" };
        FileShareClient.parseArguments(args);
    }

    /**
     * Checks the correct exception and message is thrown when too few arguments are given when parsing arguments.
     */
    @Test
    public void checkParseArgumentsUsingTooFewArguments() {
        String[] args = new String[] { "--port", "1999" };
        Exception e = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parseArguments(args));
        assertEquals("Incorrect number of arguments given." + expectedMsg, e.getMessage());
    }

    /**
     * Checks the correct exception and message is thrown when too few arguments are given when parsing arguments.
     */
    @Test
    public void checkParseArgumentsUsingNullArgument() {
        assertThrows(NullPointerException.class, () -> FileShareClient.parseArguments(null));
    }

    /**
     * Checks the correct exception and message is thrown when an unknown tags is parsed.
     */
    @Test
    public void checkParseArgumentsUsingUnknownTag() {
        String[] args = new String[] { "--port", "1999", "--address", "127.0.0.1", "--file" };
        Exception e = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parseArguments(args));
        assertEquals("Unknown tag given." + expectedMsg, e.getMessage());
    }

    /**
     * Checks the correct exception and message is thrown when and unknown argument is parsed.
     */
    @Test
    public void checkParseArgumentsUsingUnknownArgument() {
        String[] args = new String[] { "--port", "1999", "--address", "127.0.0.1", "file" };
        Exception e = assertThrows(BadClientArgumentsException.class, () -> FileShareClient.parseArguments(args));
        assertEquals("Unexpected argument given." + expectedMsg, e.getMessage());
    }
}
