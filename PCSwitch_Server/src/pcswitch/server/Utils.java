package pcswitch.server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;

import pcswitch.common.*;

public class Utils {
	private static MACAddress CachedMacAddress = null;
	static MACAddress GetMAC() {
		if(CachedMacAddress != null)
			return CachedMacAddress;
		InetAddress ip;
		try {
	 
			ip = InetAddress.getLocalHost();
			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
			byte[] mac = network.getHardwareAddress();
			if(mac.length >= MACAddress.MAC_SIZE)
				CachedMacAddress = new MACAddress(mac);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e){
			e.printStackTrace();
		}
		return CachedMacAddress;
	}
	
	private static String CachedName = null;
	public static String GetName() {
		if(CachedName != null)
			return CachedName;
		
		Map<String, String> env = System.getenv();
	    if (env.containsKey("COMPUTERNAME"))
	        CachedName = env.get("COMPUTERNAME");
	    else if (env.containsKey("HOSTNAME"))
	    	CachedName = env.get("HOSTNAME");
	    else
	    	CachedName = "Unknown Computer";
		
		return CachedName;
	}

}
