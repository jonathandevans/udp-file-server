import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code FileShareClient} is used to send requests to {@link FileShareServer}.
 * 
 * @author 210016688
 */
public class FileShareClient {

    // User commands
    private static final String GET = "GET";
    private static final String FILELIST = "FILELIST";
    private static final String RATE = "RATE";

    private static final int WAIT_TIME = 200;
    private static final int MIN_BUFFER = 508;
    private static final int MAX_BUFFER = 60000;
    private static final int HEADER_SIZE = 24;

    private static String address;
    public static int port = -1;
    private static String[] command;

    private static int bufferSize = 30000;

    public static void main(String[] args) {
        try {
            parseArguments(args);
        } catch (BadClientArgumentsException e) {
            System.err.println(e.getMessage());
            System.exit(0);
        }

        connect();
    }

    /**
     * Moves through the users arguments checking they are the correct tags, then
     * checks the next element is a valid assignment.
     * 
     * @param args the users arguments
     * @throws FileShareClient.BadClientArgumentsException thrown when the arguments
     *                                                     do not meet the expected
     *                                                     requirements
     */
    public static void parseArguments(String[] args) throws FileShareClient.BadClientArgumentsException {
        // At least four arguments are required for the server information and a command
        if (args.length <= 4)
            throw new BadClientArgumentsException("Incorrect number of arguments given.");

        // Regex pattern for tags
        Pattern pattern = Pattern.compile("--[a-z]+");

        // Move through the tags
        for (int i = 0; i < args.length; i += 2) {
            Matcher matcher = pattern.matcher(args[i]);

            if (matcher.find()) {
                if (args[i].equals("--port") && port == -1) // Port tag is given and hasn't been used yet
                    parsePort(args[i + 1]);
                else if (args[i].equals("--address") && address == null) // Address tag is given and hasn't beed used
                                                                         // yet
                    address = args[i + 1];
                else if (args[i].equals("--get")) // Get tag is given
                    command = new String[] { GET, args[i + 1] };
                else if (args[i].equals("--filelist")) { // Filelist tag is given
                    command = new String[] { FILELIST };
                    i--;
                } else if (args[i].equals("--rate")) { // Rate tag is given
                    command = new String[] { RATE, args[i + 1], parseScore(args[i + 2]) };
                    i++;
                } else // Not a recognised tag
                    throw new BadClientArgumentsException("Unknown tag given.");
            } else // Not a recognised argument
                throw new BadClientArgumentsException("Unexpected argument given.");
        }
    }

    /**
     * Parses the string after port flag.
     * Uses the try-catch to parse the string into an integer throwing an error if
     * the input is not a number.
     * 
     * @param givenPort the users given port number input
     * @throws FileShareClient.BadClientArgumentsException thrown when a non-integer
     *                                                     input is given for the
     *                                                     port
     */
    public static void parsePort(String givenPort) throws FileShareClient.BadClientArgumentsException {
        try {
            // Parse string input into integer port number
            port = Integer.parseInt(givenPort);
        } catch (NumberFormatException e) {
            throw new BadClientArgumentsException("Port given is not an integer.");
        }

        // Restricts numbers to port numbers
        if (port < 0 || port > 65535)
            throw new BadClientArgumentsException("Port number is not valid.");
    }

    /**
     * Parses the second string after the rate flag.
     * Uses the try-catch to parse the string into an integer throwing an error if
     * the input is not a number.
     * 
     * @param givenScore the users given score number input
     * @throws FileShareClient.BadClientArgumentsException thrown when a non-integer
     *                                                     input if given for the
     *                                                     score
     */
    public static String parseScore(String givenScore) throws FileShareClient.BadClientArgumentsException {
        int score;

        try {
            // Parse string input into integer score number
            score = Integer.parseInt(givenScore);
        } catch (NumberFormatException e) {
            throw new BadClientArgumentsException("Score given is not an integer.");
        }

        // Restricts numbers to 0-10
        if (score < 0 || score > 10)
            throw new BadClientArgumentsException("Score given is outside the range 0-10.");

        return givenScore;
    }

    /**
     * Exception class used to identify errors in the arguments given.
     */
    public static class BadClientArgumentsException extends Exception {
        static String expectedMsg = "\nExpected: --port <port> --address <server> --get <filename>\n          --port <port> --address <server> --filelist\n          --port <port> --address <server> --rate <filename> <score>";

        public BadClientArgumentsException(String message) {
            super(message + expectedMsg);
        }
    }

    /**
     * Used to connect to the given server continually requesting packets until the
     * given command is completed.
     */
    private static void connect() {
        try {
            // Creates socket
            DatagramSocket client = new DatagramSocket();

            // Decides which function to run based of user arguments
            if (command[0].equals(GET))
                getCommand(client);
            else if (command[0].equals(FILELIST))
                filelistCommand(client);
            else if (command[0].equals(RATE))
                rateCommand(client);
        } catch (SocketException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Used to run the GET command on the server.
     * 
     * @param client the client sending the request packets
     * @throws IOException thrown on IOException
     */
    private static void getCommand(DatagramSocket client) throws IOException {
        String filename = command[1];
        int dataPartitionNumber = 0;

        boolean completed = false;
        while (!completed) {
            // Send packet and wait for response
            PacketSender sender = new PacketSender(client, filename, dataPartitionNumber);
            sender.start();
            DatagramPacket receivedPacket = waitForPacket(client, sender, dataPartitionNumber);

            // Packet received
            byte[] header = accessHeader(receivedPacket.getData());
            if ((new String(header).split(" ")[0].trim()).equals("FNF")) {  // Prevents errors on request for non existing files
                System.out.println("File does not exist.");
                return;
            }
            if ((new String(header).split(" ")[0].trim()).equals("F")) { // If the packet is the final required packet
                writeFile(filename, receivedPacket.getData(), receivedPacket.getLength() - HEADER_SIZE);
                return;
            }
            // Moves the required partition to the next partition
            dataPartitionNumber = Integer.parseInt(new String(header).split(" ")[2].trim()) + 1;
            writeFile(filename, receivedPacket.getData(), receivedPacket.getLength() - HEADER_SIZE);

            // Increases packet received size when appropriate
            if ((bufferSize * 3) < MAX_BUFFER)
                bufferSize *= 3;
            else if ((bufferSize * 1.5) < MAX_BUFFER)
                bufferSize *= 1.5;
        }
    }

    /**
     * Used to run the FILELIST command on the server.
     * 
     * @param client the client sending the request packets
     * @throws IOException thrown on IOException
     */
    private static void filelistCommand(DatagramSocket client) throws IOException {
        int dataPartitionNumber = 0;

        boolean completed = false;
        while (!completed) {
            // Send packet and wait for response
            PacketSender sender = new PacketSender(client, dataPartitionNumber);
            sender.start();
            DatagramPacket receivedPacket = waitForPacket(client, sender, dataPartitionNumber);

            // Packet received
            byte[] header = accessHeader(receivedPacket.getData());
            if ((new String(header).split(" ")[0].trim()).equals("F")) {
                outputToScreen(receivedPacket);
                return;
            }

            // Moves the required partition to the next position
            dataPartitionNumber = Integer.parseInt(new String(header).split(" ")[1].trim()) + 1;
            outputToScreen(receivedPacket);
        }
    }

    /**
     * Used to run the RATE command on the server.
     * 
     * @param client the client sending the request packets
     * @throws IOException thrown on IOException
     */
    private static void rateCommand(DatagramSocket client) throws IOException {
        String filename = command[1];
        String score = command[2];

        // Send packet and wait for response
        PacketSender sender = new PacketSender(client, filename, score);
        sender.start();
        DatagramPacket receivedPacket = waitForPacket(client, sender, 0);

        // Packet received
        outputToScreen(receivedPacket);
    }

    /**
     * Thread class used to continuously send packets to the server until a response
     * is given.
     */
    static class PacketSender extends Thread {
        String keyword;

        DatagramSocket client;
        String filename;
        int dataPartitionNumber;
        String score;

        volatile boolean end = false;

        // Constructor used by GET
        PacketSender(DatagramSocket client, String filename, int dataPartitionNumber) {
            this.keyword = GET;

            this.client = client;
            this.filename = filename;
            this.dataPartitionNumber = dataPartitionNumber;
        }

        // Constructor used by FILELIST
        PacketSender(DatagramSocket client, int dataPartitionNumber) {
            this.keyword = FILELIST;

            this.client = client;
            this.dataPartitionNumber = dataPartitionNumber;
        }

        // Constructor used by RATE
        PacketSender(DatagramSocket client, String filename, String score) {
            this.keyword = RATE;

            this.client = client;
            this.filename = filename;
            this.score = score;
        }

        public void run() {
            while (!end) {
                byte[] request = createRequest();

                // Attempt to send packet
                try {
                    MaybeSendUDP.maybeSend(client, request, InetAddress.getByName(address), port);
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }

                // Give server time to respond
                try {
                    Thread.sleep(WAIT_TIME);
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }

                // If the packet succeded stop
                if (end)
                    break;

                // Adjusts expected packet size as previous packet failed
                if (bufferSize < (MIN_BUFFER * 2))
                    bufferSize *= 2;
                else
                    bufferSize /= 1.5;
            }
        }

        /**
         * Generates different request depending on the entered flag.
         * 
         * @return the generated request
         */
        private byte[] createRequest() {
            if (keyword.equals(GET))
                return createGetRequest();
            else if (keyword.equals(FILELIST))
                return createFileListRequest();
            else if (keyword.equals(RATE))
                return createRateRequest();

            return null;
        }

        /**
         * Used to create GET requests that will be sent to the server.
         * 
         * @return the GET request
         */
        private byte[] createGetRequest() {
            return (GET + " " + filename + " " + dataPartitionNumber + " " + bufferSize).getBytes();
        }

        /**
         * Used to create FILELSIT requests that will be sent to the server.
         * 
         * @return the FILELIST request
         */
        private byte[] createFileListRequest() {
            return (FILELIST + " " + dataPartitionNumber).getBytes();
        }

        /**
         * Used to create RATE requests that will be sent to the server.
         * 
         * @return the RATE request
         */
        private byte[] createRateRequest() {
            return (RATE + " " + 0 + " " + filename + " " + score).getBytes();
        }
    }

    /**
     * Accepts packets on the client socket checking if the data partition matches
     * the one required before returning.
     * 
     * @param client              the open socket
     * @param sender              the thread sending packets to the server
     * @param dataPartitionNumber the partition required
     * @return the received packet
     * @throws IOException thrown on IOException
     */
    private static DatagramPacket waitForPacket(DatagramSocket client, PacketSender sender, int dataPartitionNumber)
            throws IOException {
        DatagramPacket packet = null;

        boolean waiting = true;
        while (waiting) {
            // Receives a packet
            byte[] buffer = new byte[MAX_BUFFER];
            packet = new DatagramPacket(buffer, buffer.length);
            client.receive(packet);
            packet.setLength(packet.getLength());

            // Checks the packet is currently usable
            byte[] header = new byte[HEADER_SIZE];
            System.arraycopy(packet.getData(), 0, header, 0, HEADER_SIZE);
            int startPartition = Integer.parseInt(new String(header).split(" ")[1].trim());

            if (dataPartitionNumber == startPartition) // Checks the data starting position is the next data partition
                                                       // needed
                waiting = false;
        }

        // Stops requesting the server for the same data
        sender.end = true;
        return packet;
    }

    /**
     * Access and returns the custom header of a packet as a byte[].
     * 
     * @param contents the contents of a packet
     * @return the header of the given packet contents
     */
    private static byte[] accessHeader(byte[] contents) {
        byte[] header = new byte[HEADER_SIZE];
        System.arraycopy(contents, 0, header, 0, HEADER_SIZE);
        return header;
    }

    /**
     * Used write the contents of a packet to a given file.
     * Ignores the header.
     * 
     * @param filename the filename to write out to
     * @param bytes    the contents to write
     * @param length   the length of contents to write
     */
    private static void writeFile(String filename, byte[] bytes, int length) {
        try (OutputStream output = new FileOutputStream(filename, true)) {
            output.write(bytes, HEADER_SIZE, length);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Used to print the contents of a packet to the stdout.
     * Ignores the header.
     * 
     * @param packet the packet to output
     */
    private static void outputToScreen(DatagramPacket packet) {
        byte[] bytes = new byte[packet.getLength() - HEADER_SIZE];
        System.arraycopy(packet.getData(), HEADER_SIZE, bytes, 0, packet.getLength() - HEADER_SIZE);
        System.out.println(new String(bytes));
    }
}
