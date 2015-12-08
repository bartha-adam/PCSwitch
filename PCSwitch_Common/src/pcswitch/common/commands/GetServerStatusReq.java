package pcswitch.common.commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class GetServerStatusReq extends CommandBase {

	public GetServerStatusReq() {
		super(CommandID_GetStatus_Req);
	}

	@Override
	public void Visit(CommandVisitor visitor) {
		visitor.Process(this);
	}

	@Override
	protected void UnserializeSpecific(DataInputStream input) {
		
	}

	@Override
	protected void SerializeSpecific(DataOutputStream output) {
		
	}
	
	@Override
	public String toString(){
		return "GetServerStatusReq[" + super.toString() + "]";
	}

}
