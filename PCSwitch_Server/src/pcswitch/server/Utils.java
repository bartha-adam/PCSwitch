package pcswitch.server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;

import pcswitch.common.*;

public class Utils {
	private static MACAddress CachedMacAddress = null;
	private static String CachedMachineName = null;
	private static String CachedOsName = null;
	private static Boolean CachedIsLinux = null;
	private static Boolean CachedIsWindows = null;
	
	static Boolean isLinux()
	{
		if(CachedIsLinux != null)
			return CachedIsLinux;
		if(CachedOsName == null) {
			CachedOsName = System.getProperty("os.name");
		}
		CachedIsLinux = CachedOsName.indexOf("nix") >= 0 || CachedOsName.indexOf("nux") >= 0 || CachedOsName.indexOf("aix") > 0;
		return CachedIsLinux;
	}
	
	static Boolean isWindows()
	{
		if(CachedIsWindows != null)
			return CachedIsWindows;
		if(CachedOsName == null) {
			CachedOsName = System.getProperty("os.name");
		}
		CachedIsWindows = CachedOsName.indexOf("win") >= 0;
		return CachedIsWindows;
	}
	
	static MACAddress getMAC() {
		if(CachedMacAddress != null)
			return CachedMacAddress;
		if(isWindows())	
			CachedMacAddress = getMAC_Windows();
		else if (isLinux())	
			CachedMacAddress = getMAC_Linux();
		
		return CachedMacAddress;
	}
	
	static MACAddress getMAC_Linux() {
		MACAddress result = null;
		try {
	        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
	        while(networkInterfaces.hasMoreElements())
	        {
	            NetworkInterface network = networkInterfaces.nextElement();
	            byte[] mac = network.getHardwareAddress();
	            if(mac != null) {
	                if(mac.length >= MACAddress.MAC_SIZE)
						result = new MACAddress(mac);
	                break;
	            }
	        }
	    } catch (SocketException e){

	        e.printStackTrace();

	    }
		return result;
	}
	
	static MACAddress getMAC_Windows() {
		MACAddress result = null;
		try {
			InetAddress ip = InetAddress.getLocalHost();
			if(ip == null)
			{
				NetworkInterface network = NetworkInterface.getByInetAddress(ip);
				if(network != null) {
					byte[] mac = network.getHardwareAddress();
					if(mac.length >= MACAddress.MAC_SIZE)
						result = new MACAddress(mac);
				} else {
					System.out.println("Failed to get MAC address, network interface is null");
				}
			}
			else
			{
				System.out.println("Failed to get MAC address, ip is null");
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e){
			e.printStackTrace();
		}
		return result;
		
	}
	
	public static String getMachineName() {
		if(CachedMachineName != null)
			return CachedMachineName;
		if(isWindows()) {
			Map<String, String> env = System.getenv();
		    if (env.containsKey("COMPUTERNAME"))
		        CachedMachineName = env.get("COMPUTERNAME");
		    else if (env.containsKey("HOSTNAME"))
		    	CachedMachineName = env.get("HOSTNAME");
		} else if (isLinux()) {
			try {
				CachedMachineName = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				System.out.print("Failed to obtain hostname!");
				e.printStackTrace();
			}
		} else {
			System.out.print("Failed to get hostname, unknown OS!");
		}
		
		if(CachedMachineName == null)
	    	CachedMachineName = "Unknown Computer";
		
		
		return CachedMachineName;
	}
	
	static String byteArrayToHex(byte[] a, int size) {
	   StringBuilder sb = new StringBuilder();
	   for (int i=0; i<size; i++) {
	      sb.append(String.format("%02x", a[i]&0xff));
	   }
	   return sb.toString();
	}

}
