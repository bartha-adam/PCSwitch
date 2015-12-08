package pcswitch.common.commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SetShutdownDelayRsp extends CommandBase {

	int shutdownDelay;
	public SetShutdownDelayRsp() {
		super(CommandID_SetShutdownDelayRsp);
		shutdownDelay = 0;
	}

	public SetShutdownDelayRsp(int transactionId_) {
		super(CommandID_SetShutdownDelayRsp, transactionId_);
		shutdownDelay = 0;
	}

	@Override
	public void Visit(CommandVisitor visitor) {
		visitor.Process(this);

	}

	@Override
	protected void UnserializeSpecific(DataInputStream input)
			throws IOException {
		shutdownDelay = input.readInt();

	}

	@Override
	protected void SerializeSpecific(DataOutputStream output)
			throws IOException {
		output.writeInt(shutdownDelay);
	}
	
	public int GetShutdownDelay(){
		return shutdownDelay;
	}
	
	public void SetShutdownDelay(int delay){
		this.shutdownDelay = delay;
	}
	

}
