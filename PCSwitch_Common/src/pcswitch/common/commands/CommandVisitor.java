package pcswitch.common.commands;


public abstract class CommandVisitor {

	public void Visit(CommandBase command)
	{
		command.Visit(this);
	}
	public abstract void Process(PingCommand pingReq);
	public abstract void Process(PingCommandRsp pingRsp);
	public abstract void Process(GetServerStatusReq getServerStatusReq);
	public abstract void Process(GetServerStatusRsp getServerStatusRsp);
	public abstract void Process(SetShutdownDelayReq setShutdownDelayReq);
	public abstract void Process(SetShutdownDelayRsp setShutdownDelayRsp);
}
