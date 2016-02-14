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
			server.RunServer(port);
		 }
		 catch( Exception e ) {
		    System.out.println(e) ;
		 }
	}
	
	void RunServer(int port) {
		try {
			socket = new DatagramSocket( port ) ;
			System.out.println( "The server is ready on port " + port ) ;
			for( ;; ) {
				DatagramPacket packet = new DatagramPacket( new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE ) ;
				socket.receive(packet) ;
				System.out.println(packet.getAddress().getHostAddress() + ":" + packet.getPort() + ": " +
					byteArrayToHex(packet.getData(), packet.getLength()) ) ;
			
				CommandBase command = CommandBase.Unserialize(packet.getData(), packet.getLength());
				if(command != null) {
					command.SetPeer(packet.getAddress());
					command.SetPeerPort(packet.getPort());
					CommandReceived(command);
				} else {
					System.out.println("Failed to parse command!");
				}
		    }
		}
		catch( Exception e ) {
			System.out.println( e ) ;
		}
	}
	void CommandReceived(CommandBase cmd) {
		System.out.println("Rcvd " + cmd.toString());
		RequestProcessor requestProcessor = new RequestProcessor(this, this);
		Client client = FindClinet(cmd.GetPeer());
		if(client != null) {
			client.MarkCommunication();
			System.out.println("Marking communication for " + client.toString());
		} else {
			Client newClient = new Client(cmd.GetPeer(), cmd.GetPeerPort(), clientPoolPeriod);
			System.out.println("Adding new client " + newClient.toString());
			clientList.add(newClient);
		}
		requestProcessor.Visit(cmd);
	}
	
	@Override
	public void SendCommand(CommandBase cmd) {
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
		SendStatusBroadcast();
		while(true) {
			try {
				Date now = new Date();
				CheckClients();
				if (shutdownTime != null) {
					synchronized(shutdownDelay) {
						long shutdownTimeMSec = shutdownTime.getTime();
						long nowMSec = now.getTime();
						if(shutdownTimeMSec > nowMSec) {
							shutdownDelay = (int)((shutdownTimeMSec - nowMSec)/1000);
							System.out.println("ShutdownDelay updated to " + shutdownDelay);
							if(shutdownDelay <=60)
								SendStatusToClients();
						}
						else {
							if(TryShutdown()){
								shutdownDelay = -1;//Deactivate delayed shutdown
								shutingdown = true;
								shutdownTime = null;
								SendStatusToClients();
							}
						}
					}
				}
				if(lastStatusReport == null || 
					now.getTime() - lastStatusReport.getTime() >= statusPublishPeriod) {
					SendStatusToClients();
				}
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.out.println("Server run sleep failed " + e.toString());
			} catch (Exception e){
				System.out.println("Server run sleep failed " + e.toString());
			}
		}
	}
	
	protected boolean TryShutdown(){
		boolean result = false;
		try {
			//TODO: change this if not running on Windows
			Runtime.getRuntime().exec("shutdown /s /f");
			result = true;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to shutdown: " + e.toString());
		}
		return result;
	}
	
	//TODO: move this method to utils?
	static String byteArrayToHex(byte[] a, int size) {
	   StringBuilder sb = new StringBuilder();
	   for (int i=0; i<size; i++) {
	      sb.append(String.format("%02x", a[i]&0xff));
	   }
	   return sb.toString();
	}
	public void SetShutdownDelay(int delayInSec) {
	   synchronized (shutdownDelay) {
		   if(delayInSec < 0) {
			   shutdownTime = null;
			   shutdownDelay = delayInSec;
		   } else {
			   shutdownDelay = delayInSec;
			   shutdownTime = new Date();
			   shutdownTime.setTime(shutdownTime.getTime() + delayInSec * 1000);
		   }
		   SendStatusToClients();
	   }
	}
	
	Client FindClinet(InetAddress address) {
		for(Client c : clientList) {
			if(c.GetAddress().equals(address))
				return c; 
		}
		return null;
	}
	
	void CheckClients() {
		for (Iterator<Client> iterator = clientList.iterator(); iterator.hasNext();) {
		    Client c = iterator.next();
		    if (c.IsTimeouting()) {
		        // Remove the current element from the iterator and the list.
		    	System.out.println("Removing " + c.toString());
		        iterator.remove();
		    }
		}
	}
	
	GetServerStatusRsp CreateStatusResponse(InetAddress peer, int peerPort) {
		GetServerStatusRsp getServerStatusRsp = new GetServerStatusRsp();
		getServerStatusRsp.SetPeer(peer);
		getServerStatusRsp.SetPeerPort(peerPort);
		getServerStatusRsp.SetMAC(Utils.GetMAC());
		getServerStatusRsp.SetName(Utils.GetName());
		getServerStatusRsp.SetShutdownIn(GetShutdownDelay());
		if(shutingdown) {
			getServerStatusRsp.SetStatus(GetServerStatusRsp.Status.ShutingDown.ordinal());
		} else {
			getServerStatusRsp.SetStatus(GetServerStatusRsp.Status.ON.ordinal());
		}
		return getServerStatusRsp;
	}
	
	void SendStatusBroadcast() {
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
					GetServerStatusRsp getServerStatusRsp = CreateStatusResponse(broadcast, DefaultClientPort);
					SendCommand(getServerStatusRsp);
				}
			}
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	void SendStatusToClients() {
		lastStatusReport = new Date();
		for(Client c : clientList) {
		    GetServerStatusRsp getServerStatusRsp = CreateStatusResponse(c.GetAddress(), c.GetPort());
			SendCommand(getServerStatusRsp);
		}
	}
	
   
   public int GetShutdownDelay(){
	   synchronized (shutdownDelay) {
		   return shutdownDelay;
	   }
   }
}
