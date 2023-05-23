import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code FileShareClient} is used to send requests to {@link FileShareServer}.
 * 
 * @author 210016688
 */
public class FileShareServer {

    // User commands
    private static final String GET = "GET";
    private static final String FILELIST = "FILELIST";
    private static final String RATE = "RATE";

    private static final int HEADER_SIZE = 24;
    private static final int DATA_PARTITION_SIZE = 256;
    private static final String RATE_FILE = "./rate.txt";

    public static int port = -1;
    public static String directory;
    private static int bufferSize = 1024;

    public static void main(String[] args) {
        try {
            parseArguments(args);
        } catch (BadServerArgumentsException e) {
            System.err.println(e.getMessage());
            System.exit(0);
        }

        openSocket();
    }

    /**
     * Moves through the users arguments checking they are the correct tags, then
     * checks the next element is a valid assignment.
     * 
     * @param args the users arguments
     * @throws FileShareServer.BadServerArgumentsException thrown when the arguments
     *                                                     do not meet the expected
     *                                                     requirements
     */
    public static void parseArguments(String[] args) throws FileShareServer.BadServerArgumentsException {
        // Four arguments are required to implement server
        if (args.length != 4)
            throw new BadServerArgumentsException("Incorrect number of arguments given.");

        // Regex pattern for tags
        Pattern pattern = Pattern.compile("--[a-z]+");

        // Move through the tags
        for (int i = 0; i < args.length; i += 2) {
            Matcher matcher = pattern.matcher(args[i]);

            if (matcher.find()) {
                if (args[i].equals("--port") && port == -1) // Port tag is given and hasn't been used yet
                    parsePort(args[i + 1]);
                else if (args[i].equals("--directory") && directory == null) // Directory tag is given and hasn't beed
                                                                             // used yet
                    parseDirectory(args[i + 1]);
                else // Not a recognised tag
                    throw new BadServerArgumentsException("Unknown/Repeated tag given.");
            } else // Not a recognised argument
                throw new BadServerArgumentsException("Unexpected argument given.");
        }
    }

    /**
     * Parses the string after port flag.
     * Uses the try-catch to parse the string into an integer throwing an error if
     * the input is not a number.
     * 
     * @param givenPort the users given port number input
     * @throws FileShareServer.BadServerArgumentsException thrown when a non-integer
     *                                                     input is given for the
     *                                                     port
     */
    public static void parsePort(String givenPort) throws FileShareServer.BadServerArgumentsException {
        try {
            // Parse string input into integer port number
            port = Integer.parseInt(givenPort);
        } catch (NumberFormatException e) {
            throw new BadServerArgumentsException("Port given is not an integer.");
        }

        // Restricts numbers to port numbers
        if (port < 0 || port > 65535)
            throw new BadServerArgumentsException("Port number is not valid.");
    }

    /**
     * Parses the string after the directory flag.
     * Using regex checks whether the string is a valid filepath.
     * 
     * @param givenDirectory the users given filepath input
     * @throws FileShareServer.BadServerArgumentsException thrown when a bad
     *                                                     filepath is given
     */
    public static void parseDirectory(String givenDirectory) throws FileShareServer.BadServerArgumentsException {
        // Regex expression for a filepath
        Pattern pattern = Pattern.compile("(((\\.\\.|\\.)/)?(([^/]*/)*)?([^\\.]+))");
        Matcher matcher = pattern.matcher(givenDirectory);

        if (matcher.find())
            directory = givenDirectory;
        else
            throw new BadServerArgumentsException("Directory given is not a valid filepath.");
    }

    /**
     * Exception class used to identify errors in the arguments given.
     */
    public static class BadServerArgumentsException extends Exception {
        static String expectedMsg = "\nExpected: --port <port> --directory <directory>";

        public BadServerArgumentsException(String message) {
            super(message + expectedMsg);
        }
    }

    /**
     * Used to create socket for client requests.
     * Waits for a request using the keyword to determine which function to run.
     */
    private static void openSocket() {
        try {
            // Creates socket using given port
            DatagramSocket server = createSocket();

            // Loops server meaning it's always waiting for requests
            boolean running = true;
            while (running) {
                // Holds the program until a packet is received
                DatagramPacket receivedPacket = waitForPacket(server);

                // Packet received
                String keyword = new String(receivedPacket.getData()).split(" ")[0];
                // Decides which function to perform
                if (keyword.equals(GET))
                    getCommand(server, receivedPacket);
                else if (keyword.equals(FILELIST))
                    filelistCommand(server, receivedPacket);
                else if (keyword.equals(RATE))
                    rateCommand(server, receivedPacket);
            }

            server.close();
        } catch (SocketException | UnknownError e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Creates socket on given port outputting socket details to the user.
     * 
     * @return the open socket
     * @throws SocketException      thrown on SocketException
     * @throws UnknownHostException thrown on UnknownHostException
     */
    private static DatagramSocket createSocket() throws SocketException, UnknownHostException {
        DatagramSocket server = new DatagramSocket(port, InetAddress.getLocalHost());

        // Outputs socket information to the user
        System.out.println("Host: " + server.getLocalAddress().getHostName());
        System.out.println("      " + server.getLocalAddress().getHostAddress());
        System.out.println("Port: " + server.getLocalPort());

        return server;
    }

    /**
     * Receives and returns packets from the client.
     * 
     * @param server the socket receiving the packet
     * @return the clients packet
     * @throws IOException thrown on IOException
     */
    private static DatagramPacket waitForPacket(DatagramSocket server) throws IOException {
        byte[] buffer = new byte[bufferSize];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
        server.receive(receivedPacket);

        System.out.println("Recieved packet from client.");

        receivedPacket.setLength(receivedPacket.getLength());
        return receivedPacket;
    }

    /**
     * Accesses given file and returns a packet to a client containing data
     * partition requested.
     * 
     * @param server         the socket receiving the packet
     * @param receivedPacket the packet received from the client
     * @throws IOException thrown on IOException
     */
    private static void getCommand(DatagramSocket server, DatagramPacket receivedPacket) throws IOException {
        String[] request = new String(receivedPacket.getData()).split(" ");
        String filename = request[1].trim();

        int dataPartitionNumber = Integer.parseInt(request[2].trim());
        int maxReturnPacketSize = Integer.parseInt(request[3].trim());
        int numOfPartitions = Math.floorDiv(maxReturnPacketSize - HEADER_SIZE, DATA_PARTITION_SIZE);

        String more = "T";

        // Try to access given file
        try (FileInputStream input = new FileInputStream(directory + "/" + filename)) {
            // Moves through the file until correct data partition is reached
            for (int i = 0; i < dataPartitionNumber; i++) {
                byte[] bin = new byte[DATA_PARTITION_SIZE];
                input.read(bin, 0, DATA_PARTITION_SIZE);
            }

            int returnPacketSize = (numOfPartitions * DATA_PARTITION_SIZE) + HEADER_SIZE;
            byte[] response = new byte[returnPacketSize];
            // Reads the file content into response array
            for (int i = HEADER_SIZE; i < response.length; i++) {
                int num = input.read();

                if (num != -1)
                    response[i] = (byte) num;
                else {
                    more = "F";
                    response = createFinalPacket(response, i);
                    break;
                }
            }

            int endDataPartitionNumber = dataPartitionNumber + numOfPartitions - 1;
            // Adds header to response
            byte[] header = (more + " " + dataPartitionNumber + " " + endDataPartitionNumber).getBytes();
            for (int i = 0; i < header.length; i++)
                response[i] = header[i];
            
            System.out.println("Trying to send packet: " + dataPartitionNumber + "-" + endDataPartitionNumber);
            MaybeSendUDP.maybeSend(server, response, receivedPacket.getAddress(), receivedPacket.getPort());
        } catch (FileNotFoundException e) {
            System.err.println("Client requested non-existing file.");
            
            byte[] reponse = ("FNF 0 0").getBytes();
            MaybeSendUDP.maybeSend(server, reponse, receivedPacket.getAddress(), receivedPacket.getPort());
        }
    }

    /**
     * Used to create the smaller final packet to send in the GET request.
     * 
     * @param response the bytes to send
     * @param i        the number of bytes populated
     * @return the adjusted buffer size
     */
    private static byte[] createFinalPacket(byte[] response, int i) {
        byte[] newResponse = new byte[i];
        System.arraycopy(response, HEADER_SIZE, newResponse, HEADER_SIZE, i - HEADER_SIZE);
        return newResponse;
    }

    /**
     * Access directory to return a file along with its rating.
     * 
     * @param server         the socket waiting for the clients packet
     * @param receivedPacket the packet received from the client
     * @throws IOException thrown on IOException
     */
    private static void filelistCommand(DatagramSocket server, DatagramPacket receivedPacket) throws IOException {
        int dataPartitionNumber = Integer.parseInt(new String(receivedPacket.getData()).split(" ")[1].trim());
        HashMap<String, int[]> ratings = readRateFile();

        File folder = new File(directory);
        File[] files = folder.listFiles();

        String more = "T";
        if (dataPartitionNumber < files.length) {
            String filename = files[dataPartitionNumber].getName();
            if (dataPartitionNumber == files.length - 1)
                more = "F";

            // Adds required contents to byte[]
            byte[] response = new byte[bufferSize];
            byte[] line = (filename + " " + getRating(filename, ratings)).getBytes();
            for (int i = 0; i < line.length; i++)
                response[HEADER_SIZE + i] = line[i];

            // Adds header to response
            byte[] header = (more + " " + dataPartitionNumber).getBytes();
            for (int i = 0; i < header.length; i++)
                response[i] = header[i];

            System.out.println("Trying to send packet: " + dataPartitionNumber);
            MaybeSendUDP.maybeSend(server, response, receivedPacket.getAddress(), receivedPacket.getPort());
        }
    }

    /**
     * Reads the file holding the ratings for all the other files returning a
     * hashmap, mapping filenames to ratings.
     * 
     * @return the hashmap mapping filenames to ratings
     * @throws IOException thrown an IOExceptiom
     */
    private static HashMap<String, int[]> readRateFile() throws IOException {
        HashMap<String, int[]> ratings = new HashMap<String, int[]>();

        // Check if rating file exists
        File rateFile = new File(RATE_FILE);
        if (!rateFile.exists())
            return ratings;

        try (BufferedReader reader = new BufferedReader(new FileReader(RATE_FILE));) {
            String line;
            // Move through each line of the file
            while ((line = reader.readLine()) != null) {
                String[] rating = line.trim().split(":");
                if (rating.length == 2) {
                    // Parse the second object into int otherwise ignore that line
                    try {
                        String[] temp = rating[1].trim().split(" ");
                        int[] num = new int[] { Integer.parseInt(temp[0]), Integer.parseInt(temp[1]) };
                        ratings.put(rating[0], num);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
        return ratings;
    }

    /**
     * Returns the stored rating or a string of NR.
     * 
     * @param filename the filename of desired rating
     * @param ratings  the hashmap of filenames to ratings
     * @return the current rating
     */
    private static String getRating(String filename, HashMap<String, int[]> ratings) {
        if (ratings.containsKey(filename)) {
            int score = ratings.get(filename)[0] / ratings.get(filename)[1];
            return "" + score;
        } else
            return "NR";
    }

    /**
     * Accesses rate.txt and directory to confirm whether a file exists before
     * allowing the user to assign it a rating.
     * 
     * @param server         the socket waiting for the clients packet
     * @param receivedPacket the packet recieved from the client
     * @throws IOException thrown on IOException
     */
    private static void rateCommand(DatagramSocket server, DatagramPacket receivedPacket) throws IOException {
        int dataPartitionNumber = Integer.parseInt(new String(receivedPacket.getData()).split(" ")[1]);
        String filename = new String(receivedPacket.getData()).split(" ")[2].trim();
        int score = Integer.parseInt(new String(receivedPacket.getData()).split(" ")[3].trim());

        byte[] content;

        // Check if given file exists
        if (checkFileExists(filename)) {
            // Maps filename to average rating
            HashMap<String, int[]> ratings = readRateFile();
            if (ratings.keySet().contains(filename)) { // File has rating
                // Rating is stored using two values, [0] is total [1] is number of entries
                int[] rating = ratings.get(filename);
                rating[0] += score;
                rating[1]++;
                ratings.replace(filename, rating);
            } else { // File doesn't have rating
                int[] rating = new int[] { score, 1 };
                ratings.put(filename, rating);
            }

            // Update rate.txt
            writeRateFile(ratings);
            // Calculates file rating
            int num = ratings.get(filename)[0] / ratings.get(filename)[1];
            content = ("Average score for " + filename + " is now " + num).getBytes();
        } else
            content = ("File does not exist").getBytes();

        // Adds required contents to byte[]
        byte[] response = new byte[bufferSize];
        for (int i = 0; i < content.length; i++)
            response[i + HEADER_SIZE] = content[i];

        // Adds header to response
        byte[] header = ("F " + dataPartitionNumber).getBytes();
        for (int i = 0; i < header.length; i++)
            response[i] = header[i];

        System.out.println("Try to send packet.");
        MaybeSendUDP.maybeSend(server, response, receivedPacket.getAddress(), receivedPacket.getPort());
    }

    /**
     * Using filename checks if the directory contains the file.
     * 
     * @param filename the given filename
     * @return whether the file exists
     */
    private static boolean checkFileExists(String filename) {
        boolean exists = false;
        // Moves through the files in the directory
        for (File file : new File(directory).listFiles()) {
            if (file.getName().equals(filename)) {
                exists = true;
                break;
            }
        }
        return exists;
    }

    /**
     * Outputs the current ratings to the rate.txt file.
     * 
     * @param ratings the hashmap mapping filename to current rating
     */
    private static void writeRateFile(HashMap<String, int[]> ratings) {
        try (OutputStream output = new FileOutputStream(RATE_FILE)) {
            // Move through each rating adding it to the rate.txt
            for (String key : ratings.keySet()) {
                byte[] line = (key + ": " + ratings.get(key)[0] + " " + ratings.get(key)[1] + "\n").getBytes();
                output.write(line);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}