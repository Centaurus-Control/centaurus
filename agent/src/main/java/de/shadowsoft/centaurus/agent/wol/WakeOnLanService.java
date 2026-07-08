package de.shadowsoft.centaurus.agent.wol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import org.springframework.stereotype.Service;

@Service
public class WakeOnLanService {

    public void send(String macAddress, String broadcastAddress, int port) {
        byte[] macBytes = parseMac(macAddress);
        byte[] packetBytes = new byte[6 + 16 * macBytes.length];
        for (int i = 0; i < 6; i++) {
            packetBytes[i] = (byte) 0xff;
        }
        for (int i = 6; i < packetBytes.length; i += macBytes.length) {
            System.arraycopy(macBytes, 0, packetBytes, i, macBytes.length);
        }
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            DatagramPacket packet = new DatagramPacket(
                packetBytes,
                packetBytes.length,
                InetAddress.getByName(broadcastAddress),
                port
            );
            socket.send(packet);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not send Wake-on-LAN packet", exception);
        }
    }

    private byte[] parseMac(String macAddress) {
        String normalized = macAddress.replace("-", "").replace(":", "");
        if (normalized.length() != 12) {
            throw new IllegalArgumentException("MAC address must contain 12 hex characters");
        }
        byte[] bytes = new byte[6];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(normalized.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }
}
