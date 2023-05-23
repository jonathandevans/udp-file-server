// Update v1.1 : Add setPacketMax
import java.io.*;
import java.net.*;
import java.util.concurrent.ThreadLocalRandom;

class MaybeSendUDP {

    private static int percentageSuccess = 50;
    private static int packetMax = 60000;

    static void setPercentageSuccess(int newChance) {
        System.out.println("Setting UDP success % change to: " + newChance);
        percentageSuccess = newChance;
    }


    static void setPacketMax(int newPacketMax) {
	System.out.println("Setting packet max size to: " + newPacketMax);
	packetMax = newPacketMax;
    }

    static void maybeSend(DatagramSocket socket, byte[] bytes, InetAddress address, int port) throws IOException
    {
        if(bytes.length > packetMax) {
            return;
        }

        int chance = ThreadLocalRandom.current().nextInt(0, 100);
        if(chance < percentageSuccess) {
            DatagramPacket packet =
            new DatagramPacket(bytes, bytes.length, address, port);
            socket.send(packet);
        }
    }
}
