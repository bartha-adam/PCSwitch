package pcswitch.server;
import pcswitch.common.commands.*;

public interface CommandSender {

	public void sendCommand(CommandBase command);
}
