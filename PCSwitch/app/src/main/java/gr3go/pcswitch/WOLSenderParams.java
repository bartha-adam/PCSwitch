package gr3go.pcswitch;

import java.net.InetAddress;

import pcswitch.common.MACAddress;

/**
 * Created by gr3go on 2/8/2016.
 */
public class WOLSenderParams {
    private InetAddress broadcastAddress;
    private MACAddress mac;

    public WOLSenderParams(InetAddress broadcastAddress, MACAddress mac) {
        this.broadcastAddress = broadcastAddress;
        this.mac = mac;
    }

    InetAddress GetDestinationAddress() {
        return broadcastAddress;
    }

    MACAddress GetDestinationMAC() {
        return mac;
    }
}
