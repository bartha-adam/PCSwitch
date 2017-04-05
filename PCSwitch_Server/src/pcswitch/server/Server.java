package pcswitch.server;

import java.io.IOException;
import java.net.*;
import java.util.*;

import pcswitch.common.commands.*;

public class Server extends Thread implements CommandSender{

	private final static int MAX_PACKET_SIZE = 100 ;
	
	protected DatagramSocket socket;
	protected Integer shutdownDelay = -1;
	protected boolean shutingdown = false;
	protected Date shutdownTime = null;
	protected Date lastStatusReport = null;
	protected List<Client> clientList = new ArrayList<Client>();
	protected int clientPoolPeriod = 60; //60sec
	protected int statusPublishPeriod = 10000; //10sec
	protected int DefaultClientPort = 12001;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Check the arguments
		if( args.length != 1 ) {
			System.out.println( "usage: PCRemote_Server <port>" ) ;
			return ;
		}
		try {
			int port = Integer.parseInt(args[0]) ;
			Server server = new Server();
			server.start();
			server.runServer(port);
		 }
		 catch( Exception e ) {
		    System.out.println(e) ;
		 }
	}
	
	void runServer(int port) {
		try {
			socket = new DatagramSocket( port ) ;
			System.out.println( "The server is ready on port " + port ) ;
			for( ;; ) {
				DatagramPacket packet = new DatagramPacket( new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE ) ;
				socket.receive(packet) ;
				System.out.println(packet.getAddress().getHostAddress() + ":" + packet.getPort() + ": " +
					Utils.byteArrayToHex(packet.getData(), packet.getLength()) ) ;
			
				CommandBase command = CommandBase.Unserialize(packet.getData(), packet.getLength());
				if(command != null) {
					command.SetPeer(packet.getAddress());
					command.SetPeerPort(packet.getPort());
					commandReceived(command);
				} else {
					System.out.println("Failed to parse command!");
				}
		    }
		}
		catch( Exception e ) {
			System.out.println( e ) ;
		}
	}
	
	void commandReceived(CommandBase cmd) {
		System.out.println("Rcvd " + cmd.toString());
		RequestProcessor requestProcessor = new RequestProcessor(this, this);
		Client client = findClinet(cmd.GetPeer());
		if(client != null) {
			client.markCommunication();
			System.out.println("Marking communication for " + client.toString());
		} else {
			Client newClient = new Client(cmd.GetPeer(), cmd.GetPeerPort(), clientPoolPeriod);
			System.out.println("Adding new client " + newClient.toString());
			clientList.add(newClient);
		}
		requestProcessor.Visit(cmd);
	}
	
	@Override
	public void sendCommand(CommandBase cmd) {
		System.out.println("Send " + cmd.toString());
		try {
			byte[] binaryCommand = cmd.Serialize();
			if(socket != null) {
				DatagramPacket packet = new DatagramPacket(binaryCommand, binaryCommand.length, cmd.GetPeer(), cmd.GetPeerPort() ) ;
				socket.send(packet);
			} else {
				System.out.println("SendCommand: socket os null!");
			}
			
		} catch (IOException e) {
			System.out.println("SendCommand: Failed (" + e.toString() + ")");
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		// Notify all clients 
		sendStatusBroadcast();
		while(true) {
			try {
				Date now = new Date();
				checkClients();
				if (shutdownTime != null) {
					synchronized(shutdownDelay) {
						long shutdownTimeMSec = shutdownTime.getTime();
						long nowMSec = now.getTime();
						if(shutdownTimeMSec > nowMSec) {
							shutdownDelay = (int)((shutdownTimeMSec - nowMSec)/1000);
							System.out.println("ShutdownDelay updated to " + shutdownDelay);
							if(shutdownDelay <=60)
								sendStatusToClients();
						}
						else {
							if(tryShutdown()){
								shutdownDelay = -1;//Deactivate delayed shutdown
								shutingdown = true;
								shutdownTime = null;
								sendStatusToClients();
							}
						}
					}
				}
				if(lastStatusReport == null || 
					now.getTime() - lastStatusReport.getTime() >= statusPublishPeriod) {
					sendStatusToClients();
				}
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.out.println("Server run sleep failed " + e.toString());
			} catch (Exception e){
				System.out.println("Server run sleep failed " + e.toString());
			}
		}
	}
	
	protected boolean tryShutdown(){
		boolean result = false;
		try {
			if(Utils.isWindows()) {
				Runtime.getRuntime().exec("shutdown /s /f");
				result = true;
			} else if (Utils.isLinux()) {
				Runtime.getRuntime().exec("shutdown -h now");
				result = true;
			} else {
				System.out.println("Failed to shut down, unknown OS");
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to shutdown: " + e.toString());
		}
		return result;
	}
	
	public void setShutdownDelay(int delayInSec) {
	   synchronized (shutdownDelay) {
		   if(delayInSec < 0) {
			   shutdownTime = null;
			   shutdownDelay = delayInSec;
		   } else {
			   shutdownDelay = delayInSec;
			   shutdownTime = new Date();
			   shutdownTime.setTime(shutdownTime.getTime() + delayInSec * 1000);
		   }
		   sendStatusToClients();
	   }
	}
	
	Client findClinet(InetAddress address) {
		for(Client c : clientList) {
			if(c.getAddress().equals(address))
				return c; 
		}
		return null;
	}
	
	void checkClients() {
		for (Iterator<Client> iterator = clientList.iterator(); iterator.hasNext();) {
		    Client c = iterator.next();
		    if (c.isTimeouting()) {
		        // Remove the current element from the iterator and the list.
		    	System.out.println("Removing " + c.toString());
		        iterator.remove();
		    }
		}
	}
	
	GetServerStatusRsp createStatusResponse(InetAddress peer, int peerPort) {
		GetServerStatusRsp getServerStatusRsp = new GetServerStatusRsp();
		getServerStatusRsp.SetPeer(peer);
		getServerStatusRsp.SetPeerPort(peerPort);
		getServerStatusRsp.SetMAC(Utils.getMAC());
		getServerStatusRsp.SetName(Utils.getMachineName());
		getServerStatusRsp.SetShutdownIn(getShutdownDelay());
		if(shutingdown) {
			getServerStatusRsp.SetStatus(GetServerStatusRsp.Status.ShutingDown.ordinal());
		} else {
			getServerStatusRsp.SetStatus(GetServerStatusRsp.Status.ON.ordinal());
		}
		return getServerStatusRsp;
	}
	
	void sendStatusBroadcast() {
		Enumeration<NetworkInterface> interfaces = null;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
		
			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = interfaces.nextElement();
				if (networkInterface.isLoopback()) {
					continue;    // Don't want to broadcast to the loopback interface
				}
				for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
					InetAddress broadcast = interfaceAddress.getBroadcast();
					if (broadcast == null)
						continue;
					
					// Use this address
					GetServerStatusRsp getServerStatusRsp = createStatusResponse(broadcast, DefaultClientPort);
					sendCommand(getServerStatusRsp);
				}
			}
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	void sendStatusToClients() {
		lastStatusReport = new Date();
		for(Client c : clientList) {
		    GetServerStatusRsp getServerStatusRsp = createStatusResponse(c.getAddress(), c.GetPort());
			sendCommand(getServerStatusRsp);
		}
	}
	
   
   public int getShutdownDelay(){
	   synchronized (shutdownDelay) {
		   return shutdownDelay;
	   }
   }
}
