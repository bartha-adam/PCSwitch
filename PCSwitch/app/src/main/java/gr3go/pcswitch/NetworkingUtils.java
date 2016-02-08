package gr3go.pcswitch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;

public class NetworkingUtils extends BroadcastReceiver{

    public interface WifiStateObserver
    {
        public void OnWifiConnected();
        public void OnWifiDisconnected();
    }

	Logger LOG = LoggerFactory.getLogger(NetworkingUtils.class);
	public NetworkingUtils(ConnectivityManager connManager_,
                           WifiManager wifi_,
                           WifiStateObserver wifiObserver_) {
		connManager = connManager_;
		wifi = wifi_;
        wifiObserver = wifiObserver_;
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
		catch (Exception ex) {
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

	public static String Ipv4ToString(int ipv4) {
    	return Formatter.formatIpAddress(ipv4);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
            if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)){
                if(wifiObserver != null)
                    wifiObserver.OnWifiConnected();
            } else {
                if(wifiObserver != null)
                    wifiObserver.OnWifiDisconnected();
            }
        }
    }

    ConnectivityManager connManager;
    WifiManager wifi;
    WifiStateObserver wifiObserver;

}
