package pcswitch.common.commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;


public class PingCommand extends CommandBase {

	public PingCommand()
	{
		super(CommandID_Ping);
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
		return "PingReq[" + super.toString() + "]"; 
	}


}
