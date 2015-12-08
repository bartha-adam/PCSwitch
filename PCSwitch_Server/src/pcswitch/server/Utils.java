package pcswitch.server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import pcswitch.common.*;

public class Utils {

	static MACAddress GetMAC()
	{
		InetAddress ip;
		MACAddress MAC = null;
		try {
	 
			ip = InetAddress.getLocalHost();
			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
			byte[] mac = network.getHardwareAddress();
			if(mac.length >= MACAddress.MAC_SIZE)
				MAC = new MACAddress(mac);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e){
			e.printStackTrace();
		}
		return MAC;
	}

}
