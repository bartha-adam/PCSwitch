package pcswitch.server;


import java.net.InetAddress;
import java.util.Date;

public class Client {
	
	private InetAddress addr;
	private int port;
	private Date lastCommunication;
	private int keepAlivePeriod; //Seconds
	
	
	public Client(InetAddress remoteAddress, int port, int keepAlivePeriod) {
		addr = remoteAddress;
		this.keepAlivePeriod = keepAlivePeriod;
		this.port = port;
		lastCommunication = new Date();
	}
	
	public void markCommunication() {
		lastCommunication = new Date();
	}
	
	public boolean isTimeouting() {
		Date now = new Date();
		if(now.getTime() - lastCommunication.getTime() > 2 * keepAlivePeriod * 1000)
			return true;
		return false;
	}
	
	public InetAddress getAddress() {
		return addr;
	}
	
	public int GetPort() {
		return port;
	}
	
	public String toString() {
		String result = "Client[addr=";
		if(addr != null)
			//getHostName is slow
			//No reason to slow down server only for logging
			//result += addr.getHostName();
			result += addr.toString();
		else
			result += "Unknown";
		result += "]";
		return result;
	}
	
}
