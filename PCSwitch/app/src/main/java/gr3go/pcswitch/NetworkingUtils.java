package gr3go.pcswitch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

public class NetworkingUtils {

	Logger LOG = LoggerFactory.getLogger(NetworkingUtils.class);
	public NetworkingUtils(ConnectivityManager connManager_, WifiManager wifi_) {
		connManager = connManager_;
		wifi = wifi_;
	}
	public boolean IsWifiConnected(){
		NetworkInfo wifiNetworkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return wifiNetworkInfo.isConnected();
	}
	
	public int GetIPv4(){
		DhcpInfo dhcpInfo = wifi.getDhcpInfo();
		return dhcpInfo.ipAddress;
	}
	
	public String GetIPv4String() {
		return Ipv4ToString(GetIPv4());
	}
	
	public int GetGatewayIPv4() {
		DhcpInfo dhcpInfo = wifi.getDhcpInfo();
		return dhcpInfo.gateway;
	}
	
	public String GetGatewayIPv4String() {
		return Ipv4ToString(GetGatewayIPv4());
	}
	
	public int GetNetMask() {
		DhcpInfo dhcpInfo = wifi.getDhcpInfo();
		return dhcpInfo.netmask;
	}
	
	public String GetNetMaskString() {
		return Ipv4ToString(GetNetMask());
	}
	
	public InetAddress GetBroadcastAddress() {
		int subnetId= GetIPv4() & GetNetMask();
		int broadcastAddress = subnetId | ~GetNetMask();

		try {
			InetAddress remoteInetAddress = InetAddress.getByAddress(IPv4To_ByteArray(broadcastAddress));
			return remoteInetAddress;
	    }
		catch (UnknownHostException e1) {
			LOG.error("Failed to send ping to " + NetworkingUtils.Ipv4ToString(broadcastAddress));
			return null;
		} catch (IOException e) {
			LOG.error("Failed to send ping to " + NetworkingUtils.Ipv4ToString(broadcastAddress));
			return null;
		}
	}

    //TODO move to utils/common
	static public byte[] IPv4To_ByteArray(int binaryIPv4){
        return new byte[]{
                (byte)binaryIPv4,
                (byte)(binaryIPv4>>>8),
                (byte)(binaryIPv4>>>16),
                (byte)(binaryIPv4>>>24)};
    }
	
	public void SendPacketTo(InetAddress serverAddress, int serverPort, byte[] bytes) throws IOException {
		try {
			DatagramSocket s = new DatagramSocket();
			DatagramPacket p = new DatagramPacket(bytes, bytes.length, serverAddress, serverPort);
			s.send(p);
		}
		catch (SocketException e) {
			e.printStackTrace();
			throw e;
		}
		catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
    }

	public static String Ipv4ToString(int ipv4) {
    	return Formatter.formatIpAddress(ipv4);
    }
	
	ConnectivityManager connManager;
	WifiManager wifi;

}
