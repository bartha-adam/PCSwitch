package pcswitch.common.commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;


public class PingCommandRsp extends CommandBase {

	public PingCommandRsp() {
		super(CommandID_Ping_Rsp);
	}
	public PingCommandRsp(int transactionID) {
		super(CommandID_Ping_Rsp, transactionID);
	}

	@Override
	public void Visit(CommandVisitor visitor) {
		visitor.Process(this);
	}
	@Override
	protected void UnserializeSpecific(DataInputStream input) {
		// TODO Auto-generated method stub
		
	}
	@Override
	protected void SerializeSpecific(DataOutputStream output) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String toString(){
		return "PingRsp[" + super.toString() + "]";
	}
}
