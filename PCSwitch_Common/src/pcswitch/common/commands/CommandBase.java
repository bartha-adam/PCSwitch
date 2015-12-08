package pcswitch.common.commands;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;


public abstract class CommandBase
{
	public static final byte CommandID_Invalid =				0x00;
	public static final byte CommandID_Ping = 					0x01;
	public static final byte CommandID_Ping_Rsp = 				0x41;
	public static final byte CommandID_Sleep = 					0x02;
	public static final byte CommandID_Sleep_Rsp = 				0x42;
	public static final byte CommandID_GetStatus_Req = 			0x03;
	public static final byte CommandID_GetStatus_Rsp = 			0x43;
	public static final byte CommandID_SetShutdownDelayReq = 	0x04;
	public static final byte CommandID_SetShutdownDelayRsp = 	0x44;
	
	public static final byte PacketVersion = 					0x01;
	
	private static int nextTransactionID = 0;
	
	private byte packetVersion;
	private byte commandID;
	private int transactionID;
	private int commandLength;
	
	private InetAddress peerAddress;
	private int			peerPort;
	
	public abstract void Visit(CommandVisitor visitor);
	
	protected CommandBase(byte commandID_){
		this(commandID_, -1);
		peerPort = 0;
	}
	
	protected CommandBase(byte commandID_, int transactionId_){
		if(transactionId_ == -1)
			transactionID = GetNextTransactionID();
		else
			transactionID = transactionId_;
		
		commandID = commandID_;
		commandLength = 0;
		packetVersion = PacketVersion;
		peerPort = 0;
	}
	public byte[] Serialize() throws IOException{
		ByteArrayOutputStream binaryOutput = new ByteArrayOutputStream();
		DataOutputStream output = new DataOutputStream(binaryOutput);
		output.writeByte(packetVersion);
		output.writeByte(commandID);
		output.writeInt(transactionID);
		
		ByteArrayOutputStream cmdSpecificBinaryOutput = new ByteArrayOutputStream();
		DataOutputStream cmdSpecificOutput = new DataOutputStream(cmdSpecificBinaryOutput);
		SerializeSpecific(cmdSpecificOutput);
		commandLength = cmdSpecificBinaryOutput.size();
		output.writeInt(commandLength);
		output.write(cmdSpecificBinaryOutput.toByteArray());
		return binaryOutput.toByteArray();
	}
	
	public static CommandBase Unserialize(byte[] bs){
		return Unserialize(bs, bs.length);
	}
	
	public static CommandBase Unserialize(byte[] bs, int length){
		CommandBase result = null;
		ByteArrayInputStream binaryInput = new ByteArrayInputStream(bs, 0, length);
		DataInputStream input = new DataInputStream(binaryInput);
		byte packetVersion;
		try {
			packetVersion = input.readByte();
			byte commandID = input.readByte();
			if(commandID == CommandID_Ping){
				result = new PingCommand();
			}
			else if(commandID == CommandID_Ping_Rsp){
				result = new PingCommandRsp();
			}
			else if(commandID == CommandID_GetStatus_Req){
				result = new GetServerStatusReq();
			}
			else if(commandID == CommandID_GetStatus_Rsp){
				result = new GetServerStatusRsp();
			}
			else if(commandID == CommandID_SetShutdownDelayReq){
				result = new SetShutdownDelayReq();
			}
			else if(commandID == CommandID_SetShutdownDelayRsp){
				result = new SetShutdownDelayRsp();
			}
				
			if(result != null)
			{
				result.packetVersion = packetVersion;
				result.transactionID = input.readInt();
				result.commandLength = input.readInt();
				result.UnserializeSpecific(input);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			result = null;
		} 
		return result;
	}
	protected abstract void UnserializeSpecific(DataInputStream input) throws IOException;
	protected abstract void SerializeSpecific(DataOutputStream output) throws IOException;
	
	protected int GetNextTransactionID(){
		return nextTransactionID++;
	}
	public int CommandID(){
		return commandID;
	}
	public void SetPeerPort(int port){
		this.peerPort = port;
	}
	public int GetPeerPort(){
		return peerPort;
	}
	public void SetPeer(InetAddress peer){
		this.peerAddress = peer;
	}
	public InetAddress GetPeer(){
		return peerAddress;
	}
	public int GetTranscationID(){
		return transactionID;
	}
	public String toString(){
		String result = "CmdID=" + (int)commandID + " TID=" + transactionID + " Peer=";
		if(peerAddress != null)
			result += peerAddress.getHostAddress() + ":" + peerPort;
		else
			result += "N/A";
		return result;
	}}
