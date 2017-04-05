package pcswitch.server;

import pcswitch.common.commands.*;

public class RequestProcessor extends CommandVisitor{

	CommandSender sender;
	Server server;
	public RequestProcessor(CommandSender sender, Server server) {
		this.sender = sender;
		this.server = server;
	}

	@Override
	public void Process(PingCommand pingReq) {
		PingCommandRsp pingRsp = new PingCommandRsp(pingReq.GetTranscationID());
		pingRsp.SetPeer(pingReq.GetPeer());
		pingRsp.SetPeerPort(pingReq.GetPeerPort());
		sender.sendCommand(pingRsp);
	}

	@Override
	public void Process(PingCommandRsp pingRsp) {
		//Should not receive this<string name="shutdownin">Shutdown:</string>
	}

	@Override
	public void Process(GetServerStatusReq getServerStatusReq) {
		GetServerStatusRsp getServerStatusRsp = new GetServerStatusRsp(getServerStatusReq.GetTranscationID());
		getServerStatusRsp.SetPeer(getServerStatusReq.GetPeer());
		getServerStatusRsp.SetPeerPort(getServerStatusReq.GetPeerPort());
		getServerStatusRsp.SetMAC(Utils.getMAC());
		getServerStatusRsp.SetName(Utils.getMachineName());
		getServerStatusRsp.SetShutdownIn(server.getShutdownDelay());
		if(server.shutingdown)
			getServerStatusRsp.SetStatus(GetServerStatusRsp.Status.ShutingDown.ordinal());
		else
			getServerStatusRsp.SetStatus(GetServerStatusRsp.Status.ON.ordinal());
		sender.sendCommand(getServerStatusRsp);
	}

	@Override
	public void Process(GetServerStatusRsp getServerStatusRsp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void Process(SetShutdownDelayReq setShutdownDelayReq) {
		SetShutdownDelayRsp setShutdownDelayRsp = new SetShutdownDelayRsp(setShutdownDelayReq.GetTranscationID());
		setShutdownDelayRsp.SetPeer(setShutdownDelayReq.GetPeer());
		setShutdownDelayRsp.SetPeerPort(setShutdownDelayReq.GetPeerPort());
		server.setShutdownDelay(setShutdownDelayReq.GetShutdownDelay());
		setShutdownDelayRsp.SetShutdownDelay(server.shutdownDelay);
		sender.sendCommand(setShutdownDelayRsp);
	}

	@Override
	public void Process(SetShutdownDelayRsp setShutdownDelayRsp) {
		// TODO Auto-generated method stub
		
	}

}
