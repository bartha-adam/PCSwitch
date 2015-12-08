package gr3go.pcswitch;


import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;

import pcswitch.common.commands.*;

public class PCManager extends CommandVisitor implements CommLinkListener, Runnable {
	Logger LOG = LoggerFactory.getLogger(PCManager.class);
	NetworkingUtils networkingUtils;
	CommLink	commLink;
	int 		remotePort= 12002;
	int 		localPort = 12001;
	
	int 		serverPublishStatusPeriod = 10000; //10sec
	Date		lastDiscoverySent = null;
	
	int			monitoringPeriod = 60000; //1min
	Date		lastMonitoringSent = null;
	
	Vector<PC>	remotePCs;
	 PC			selectedPC = null;
	PCManagerListener listener;
	Context context = null;
	PCManagerDataSource dataSource;
	
	public PCManager(NetworkingUtils networkingUtils, PCManagerListener listener,
                     Context context) throws SocketException {
		this.networkingUtils = networkingUtils;
		this.listener = listener;
		this.context = context;
        LOG.info("Constructor localPort=" + localPort);
		try {
			commLink = new CommLink(localPort, this);
            dataSource = new PCManagerDataSource(context);
            dataSource.open();
            remotePCs = dataSource.GetPCs();
            Iterator<PC> it = remotePCs.iterator();
		    while(it.hasNext()) {
		    	PC entry = (PC)it.next();
		    	NotifyPCAdded(entry, false);
		    }
		} catch (SocketException e) {
            LOG.info("Constructor exception:" + e.toString());
			e.printStackTrace();
            throw e;
		}
	}
		
	public void SetSelectedPC(PC pc) {
		if(selectedPC != null){
			synchronized(selectedPC) {
				selectedPC = pc;
			}
		} else {
            selectedPC = pc;
        }
	}
	
	public PC GetSelectedPC() {
		synchronized(selectedPC) {
			return selectedPC;
		}
	}
		
	public boolean DiscoverServers() {
		int subnetId = networkingUtils.GetIPv4() & networkingUtils.GetNetMask();
		int broadcastAddress = subnetId | ~networkingUtils.GetNetMask();
		
		//send broadcast msg
		try {
			InetAddress remoteInetAddress = InetAddress.getByAddress(
                    NetworkingUtils.IPv4To_ByteArray(broadcastAddress));
			PingCommand ping = new PingCommand();
			LOG.info("Sending ping to " + remoteInetAddress.toString());
			commLink.sendCommand(ping, remoteInetAddress, remotePort);
		} catch (UnknownHostException e1) {
			LOG.error("Failed to send ping to " + NetworkingUtils.Ipv4ToString(broadcastAddress));
			e1.printStackTrace();
		} catch (IOException e) {
			LOG.error("Failed to send ping to " + NetworkingUtils.Ipv4ToString(broadcastAddress));
			e.printStackTrace();
		}
		return true;
	}

	public boolean SendGetServerStatus_Broadcast() {
		int subnetId=networkingUtils.GetIPv4() & networkingUtils.GetNetMask();
		int broadcastAddress = subnetId | ~networkingUtils.GetNetMask();
		
		//send broadcast msg
		try {
			InetAddress remoteInetAddress = InetAddress.getByAddress(NetworkingUtils.IPv4To_ByteArray(broadcastAddress));
			GetServerStatusReq getServerStatus = new GetServerStatusReq();
			getServerStatus.SetPeer(remoteInetAddress);
			getServerStatus.SetPeerPort(remotePort);
			commLink.sendCommand(getServerStatus, remoteInetAddress, remotePort);
		} catch (UnknownHostException e1) {
			LOG.error("Failed to send GetServerStatus to " + NetworkingUtils.Ipv4ToString(broadcastAddress));
			e1.printStackTrace();
		} catch (IOException e) {
			LOG.error("Failed to send GetServerStatus to " + NetworkingUtils.Ipv4ToString(broadcastAddress));
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean SendGetServerStatus(PC pc) {
		//send msg
		InetAddress remoteInetAddress = pc.GetAddress();
		try {
			GetServerStatusReq getServerStatus = new GetServerStatusReq();
			getServerStatus.SetPeer(remoteInetAddress);
			getServerStatus.SetPeerPort(remotePort);
			commLink.sendCommand(getServerStatus, remoteInetAddress, remotePort);
		} catch (UnknownHostException e1) {
			LOG.error("Failed to send GetServerStatus to " + remoteInetAddress.getHostAddress());
			e1.printStackTrace();
		} catch (IOException e) {
			LOG.error("Failed to send GetServerStatus to " + remoteInetAddress.getHostAddress());
			e.printStackTrace();
		}
		return true;
	}
	
	@Override
	public void CommandReceived(CommandBase cmd, InetAddress from) {
		this.Visit(cmd);
	}
	
	public PC FindPC(InetAddress address) {
		if(address == null) {
            return null;
        }
		PC result = null;
		try {
			Iterator<PC> it = remotePCs.iterator();
		    while(it.hasNext()) {
		    	PC entry = (PC)it.next();
		    	if(entry.GetAddress().equals(address)) {
		    		result = entry;
		    		break;
		    	}
		    }
		}
		catch(Exception e) {
			result = null;
		}
		return result;
	}
	
	public PC FindPC(String strIPv4) {
		if(strIPv4 == null) {
            return null;
        }
		PC result = null;
		try {
			Iterator<PC> it = remotePCs.iterator();
		    while(it.hasNext()) {
		    	PC entry = (PC)it.next();
		    	if(entry.GetAddress().getHostAddress().equals(strIPv4)) {
		    		result = entry;
		    		break;
		    	}
		    }
		}
		catch(Exception e) {
			result = null;
		}
		return result;
	}
	
	public Vector<PC> GetRemotePCs() {
		return this.remotePCs;
	}
	
	public boolean SetShutdownDelayFor(PC remotePC, int delay) {
		InetAddress remoteInetAddress = remotePC.GetAddress();
		try {
			SetShutdownDelayReq req = new SetShutdownDelayReq(delay);
			LOG.info("Sending SetShutdownDelay to " + remoteInetAddress.toString() + " delay=" + delay);
			commLink.sendCommand(req, remoteInetAddress, remotePort);
		} catch (UnknownHostException e1) {
			LOG.error("Failed to send SetShutdownDelay to " + remoteInetAddress.getHostAddress());
			e1.printStackTrace();
		} catch (IOException e) {
			LOG.error("Failed to send SetShutdownDelay to " + remoteInetAddress.getHostAddress());
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void Process(PingCommand pingReq) {
		//Sent by the client
		//Nothing to do
	}

	@Override
	public void Process(PingCommandRsp pingRsp) {
        //TODO: why is this commented?
		/*synchronized (remotePCs) {
			PC remotePC = FindPC(pingRsp.GetPeer());
			if(remotePC == null){
				remotePC = new PC(pingRsp.GetPeer());
				remotePC.SetStatus(PC.Status.ON);
				remotePCs.add(remotePC);
				LOG.info("Found new " + remotePC.toString());
			}
			else
			{
				remotePC.SetStatus(PC.Status.ON);
				remotePC.MarkLastCommunication();
				LOG.info("Updated " + remotePC.toString());
			}
		}*/
	}


	@Override
	public void Process(GetServerStatusReq getServerStatusReq) {
		//Sent by the client
		//Nothing to do
	}


	@Override
	public void Process(GetServerStatusRsp getServerStatusRsp) {
		synchronized (remotePCs) {
			PC remotePC = FindPC(getServerStatusRsp.GetPeer());
			boolean newInstance = false;
			if(remotePC == null){
				remotePC = new PC(getServerStatusRsp.GetPeer());
				remotePCs.add(remotePC);
				newInstance = true;
			}
            //TODO inspect changed logic
			boolean changed = false;
			if(getServerStatusRsp.GetStatus() == GetServerStatusRsp.Status.ShutingDown.ordinal()) {
                changed |= remotePC.SetStatus(PC.Status.ShuttingDown);
            } else {
                changed |= remotePC.SetStatus(PC.Status.ON);
            }
			remotePC.MarkLastCommunication();
			changed |= remotePC.SetShutdownDelay(getServerStatusRsp.GetShutdownIn());
			changed |= remotePC.SetMACAddress(getServerStatusRsp.GetMAC());
			if(newInstance) {
				LOG.info("Added " + remotePC.toString());
				NotifyPCAdded(remotePC, true);
			} else if(changed) {
				LOG.info("Updated " + remotePC.toString());
				NotifyPCChanged(remotePC);
			}
		}
	}
	
	@Override
	public void Process(SetShutdownDelayReq setShutdownDelayReq) {
		//Sent by the client
		//Nothing to do
	}

	@Override
	public void Process(SetShutdownDelayRsp setShutdownDelayRsp) {
		if(setShutdownDelayRsp.GetPeer() == null) {
			LOG.error("SetShutdownDelayRsp peer is null!");
			return;
		}
		PC remotePC = FindPC(setShutdownDelayRsp.GetPeer());
		if(remotePC == null) {
			LOG.error("SetShutdownDelayRsp peer not found " + setShutdownDelayRsp.GetPeer().getHostAddress());
			return;
		}
		boolean changed = remotePC.SetShutdownDelay(setShutdownDelayRsp.GetShutdownDelay());
		NotifyPCChanged(remotePC);
	}

	@Override
	public void run() {
		while(true) {
			try {
				Date now = new Date();
				if(lastMonitoringSent == null || (now.getTime() - lastMonitoringSent.getTime()) > monitoringPeriod) {
					if(selectedPC != null) {
						SendGetServerStatus(selectedPC);
						lastMonitoringSent = now;
					}
				}
				if(remotePCs == null) {
                    LOG.warn("remotePCs is null!!");
                } else {
                    Iterator<PC> it = remotePCs.iterator();
                    while (it.hasNext()) {
                        PC remotePC = (PC) it.next();
                        int period = serverPublishStatusPeriod;
                        if (remotePC == selectedPC) {
                            if (monitoringPeriod < period)
                                period = monitoringPeriod;
                        }
                        if (now.getTime() - remotePC.LastCommunication().getTime() > period * 2) {
                            boolean changed = remotePC.SetStatus(PC.Status.OFF);
                            changed |= remotePC.SetShutdownDelay(-1);
                            if (changed) {
                                LOG.info("Updated " + remotePC.toString());
                                NotifyPCChanged(remotePC);
                            }
                        }
                    }
                }
			    Thread.sleep(1000);
			}
			catch(Exception e) {
				LOG.info("PCManager Periodic failed:" + e.toString());
			}
		}
		
	}
	protected void NotifyPCAdded(PC pc, boolean notifyDb)
	{
		if(notifyDb) {
            dataSource.AddPC(pc);
        }
		if(listener != null) {
            listener.PCAdded(pc);
        } else {
            LOG.error("NotifyPCAdded listener is NULL!!");
        }
	}
	
	protected void NotifyPCChanged(PC pc) {
		if(listener != null) {
            listener.PCChanged(pc);
        } else {
            LOG.error("NotifyPCChanged litener is NULL!!");
        }
	}
}
