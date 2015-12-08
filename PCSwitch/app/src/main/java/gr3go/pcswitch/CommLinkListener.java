package gr3go.pcswitch;

import java.net.InetAddress;

import pcswitch.common.commands.CommandBase;

public interface CommLinkListener {
	public void CommandReceived(CommandBase cmd, InetAddress from);
}
