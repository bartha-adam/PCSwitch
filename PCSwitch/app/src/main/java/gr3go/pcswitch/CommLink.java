package gr3go.pcswitch;

import pcswitch.common.commands.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.os.AsyncTask;
import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommLink extends Thread {
    private enum PacketSendAsyncMode {
        UsingReceiveThread,
        UsingAsyncTask
    };

    Logger LOG = LoggerFactory.getLogger(CommLink.class);

	private boolean 	        bKeepRunning = true;
	private DatagramSocket 		socket;
	private int					localPort;
	private CommLinkListener 	listener;
    private PacketSendAsyncMode packetSendMode;
    private ConcurrentLinkedQueue<DatagramPacket> packetQueue;

    static final int            MAX_INCOMING_MSG_LEN = 256;


    private class SendSocketAsyncParams {
        public SendSocketAsyncParams(CommLink link_, DatagramPacket packet_, DatagramSocket socket_) {
            link = link_;
            packet = packet_;
            socket = socket_;
        }
        public CommLink         link;
        public DatagramPacket   packet;
        public DatagramSocket   socket;
    }


    private class SendSocketAsyncOperation extends AsyncTask<SendSocketAsyncParams, Void, Void> {
        Logger LOG = LoggerFactory.getLogger(SendSocketAsyncOperation.class);
        @Override
        protected Void doInBackground(SendSocketAsyncParams... params){
            int count = params.length;
            try {
                for (int i = 0; i < count; i++) {
                    SendSocketAsyncParams param = params[i];
                    param.socket.send(param.packet);
                }
            }
            catch (Exception e) {
                LOG.debug("Socket send failed:" + e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
	
	public CommLink(int localPort, CommLinkListener listener) throws SocketException {
        LOG.debug("Creating CommLink at port " + localPort);
		this.localPort = localPort;
		this.listener = listener;
		socket = new DatagramSocket(localPort);
        packetSendMode = PacketSendAsyncMode.UsingAsyncTask;

        if(packetSendMode == PacketSendAsyncMode.UsingReceiveThread) {
            // If using receive thread for sending, the receive operation needs to timeout
            socket.setSoTimeout(100);
        }
        LOG.debug("Socket created");
        packetQueue = new ConcurrentLinkedQueue<>();
		start();
	}

	public void sendCommand(CommandBase command, InetAddress peerAddress, int peerPort)
            throws IOException {
		try {
			LOG.info("Send " + command.toString());
			byte[] rawCommand = command.Serialize();

            DatagramPacket packet = new DatagramPacket(rawCommand, rawCommand.length,
                    peerAddress, peerPort);

            if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                switch(packetSendMode) {
                    case UsingReceiveThread:
                        packetQueue.add(packet);
                        break;
                    case UsingAsyncTask:
                        new SendSocketAsyncOperation().execute(new SendSocketAsyncParams(this, packet, socket));
                        break;
                }
            } else {
                socket.send(packet);
            }
		}
		catch (SocketException e) {
			e.printStackTrace();
			throw e;
		}
		catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
	
	public void run() {
		byte[] received_message = new byte[MAX_INCOMING_MSG_LEN];
		DatagramPacket received_packet = new DatagramPacket(received_message, received_message.length);
        bKeepRunning = true;
        try {
            while(bKeepRunning) {
                if(packetSendMode == PacketSendAsyncMode.UsingReceiveThread && !packetQueue.isEmpty()) {
                    DatagramPacket packetToSend = packetQueue.poll();
                    socket.send(packetToSend);
                }
                try {
                    socket.receive(received_packet);
                    CommandBase cmd = CommandBase.Unserialize(received_message, received_packet.getLength());
                    if (cmd == null) {
                        LOG.info("Failed to parse command: " + Utils.byteArrayToHex(received_message, received_packet.getLength()));
                        continue;
                    }
                    cmd.SetPeer(received_packet.getAddress());
                    cmd.SetPeerPort(received_packet.getPort());
                    LOG.info("Rcvd " + cmd.toString());
                    if (listener != null) {
                        listener.CommandReceived(cmd, received_packet.getAddress());
                    }
                }
                catch(SocketTimeoutException ex) {
                    //Do nothing
                }
                catch(SocketException ex){
                    //Probably terminate() was called, and socket was closed
                }
            }
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
	}
	
	public void terminate() {
        bKeepRunning = false;
        if(socket != null) {
            socket.close();
            socket = null;
        }
        //Drop all pending requests
        packetQueue.clear();
    }
}
