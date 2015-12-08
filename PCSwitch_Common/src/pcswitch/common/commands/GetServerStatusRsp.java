package pcswitch.common.commands;

import pcswitch.common.MACAddress;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class GetServerStatusRsp extends CommandBase {

	public enum Status
	{
		ON,
		ShutingDown
	}
	
	MACAddress macAddress;
	int shutdownDelay; //seconds; negative -> not set
	int status;
	
	public GetServerStatusRsp() {
		super(CommandID_GetStatus_Rsp);
		macAddress = new MACAddress();
	}

	public GetServerStatusRsp(int transactionId_) {
		super(CommandID_GetStatus_Rsp, transactionId_);
		macAddress = new MACAddress();
	}

	@Override
	public void Visit(CommandVisitor visitor) {
		visitor.Process(this);
	}

	@Override
	protected void UnserializeSpecific(DataInputStream input) throws IOException {
		input.read(macAddress.bytes, 0, MACAddress.MAC_SIZE);
		shutdownDelay = input.readInt();
		status = input.readInt();
	}

	@Override
	protected void SerializeSpecific(DataOutputStream output) throws IOException {
		if(macAddress != null)
		{
			output.write(macAddress.bytes, 0, MACAddress.MAC_SIZE);
		}
		else{
			for(int i = 0; i < MACAddress.MAC_SIZE; ++i)
				output.writeByte(0);
		}
		output.writeInt(shutdownDelay);
		output.writeInt(status);
	}
	
	public void SetMAC(MACAddress MAC){
		this.macAddress = MAC;
	}
	
	public MACAddress GetMAC(){
		return macAddress;
	}
	
	public void SetShutdownIn(int sec){
		shutdownDelay = sec;
	}
	
	public int GetShutdownIn(){
		return shutdownDelay;
	}
	
	public void SetStatus(int status){
		this.status = status;
	}
	
	public int GetStatus(){
		return this.status;
	}
	
	public String toString()
	{
		String result = "GetServerStatusRsp[";
		result += super.toString();
		result += " MAC=" + macAddress.toString();
		result += " ShutdownDelay=";
		if(shutdownDelay > 0)
			result += shutdownDelay + "s";
		else
			result += "OFF";
		result += " Status=" + status;
		result += "]";
		return result;
	}
	

}
