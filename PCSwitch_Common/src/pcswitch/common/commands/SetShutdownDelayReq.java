package pcswitch.common.commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SetShutdownDelayReq extends CommandBase {

	int shutdownDelay; //0 = immediate shutdown; negative: cancel configured shutdown
	public SetShutdownDelayReq() {
		super(CommandID_SetShutdownDelayReq);
		shutdownDelay = 0;//immediate;
	}
	
	public SetShutdownDelayReq(int shutdownDelay) {
		super(CommandID_SetShutdownDelayReq);
		this.shutdownDelay = shutdownDelay;
	}
	
	public int GetShutdownDelay(){
		return shutdownDelay;
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

}
