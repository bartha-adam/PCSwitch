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

import pcswitch.common.MACAddress;
import pcswitch.common.commands.*;

public class PCManager extends CommandVisitor implements CommLinkListener, Runnable {
    Logger LOG = LoggerFactory.getLogger(PCManager.class);
    NetworkingUtils networkingUtils;
    private CommLink commLink;
    int remotePort = 12002;
    int localPort = 12001;

    Date lastDiscoverySent = null;

    Date lastMonitoringSent = null;

    static final int monitoringPeriod = 60000; //1min
    static final int serverPublishStatusPeriod = 10000; //10sec


    Vector<PC> remotePCs;
    PC selectedPC = null;
    PCManagerListener listener;
    Context context = null;
    PCManagerDataSource dataSource;
    private volatile boolean running = true;

    public PCManager(NetworkingUtils networkingUtils, PCManagerListener listener,
                     Context context) throws SocketException {
        this.networkingUtils = networkingUtils;
        this.listener = listener;
        this.context = context;

        dataSource = new PCManagerDataSource(context);
        dataSource.open();
        remotePCs = dataSource.GetPCs();
    }

    public void SetSelectedPC(PC pc) {
        if (selectedPC != null) {
            synchronized (selectedPC) {
                selectedPC = pc;
                lastMonitoringSent = null;
            }
        } else {
            selectedPC = pc;
        }
    }

    public PC GetSelectedPC() {
        synchronized (selectedPC) {
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
        if(EnsureCommLinkIsValid() == false) {
            return false;
        }
        int subnetId = networkingUtils.GetIPv4() & networkingUtils.GetNetMask();
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
        if (address == null) {
            return null;
        }
        PC result = null;
        try {
            Iterator<PC> it = remotePCs.iterator();
            while (it.hasNext()) {
                PC entry = (PC) it.next();
                if (entry.GetAddress().equals(address)) {
                    result = entry;
                    break;
                }
            }
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    public PC FindPC(String strIPv4) {
        if (strIPv4 == null) {
            return null;
        }
        PC result = null;
        try {
            Iterator<PC> it = remotePCs.iterator();
            while (it.hasNext()) {
                PC entry = (PC) it.next();
                if (entry.GetAddress().getHostAddress().equals(strIPv4)) {
                    result = entry;
                    break;
                }
            }
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    public PC FindPC(MACAddress macAddress) {
        if (macAddress == null) {
            return null;
        }
        PC result = null;
        try {
            Iterator<PC> it = remotePCs.iterator();
            while (it.hasNext()) {
                PC entry = (PC) it.next();
                if (entry.GetMACAddress().equals(macAddress)) {
                    result = entry;
                    break;
                }
            }
        } catch (Exception e) {
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
            boolean newInstance = false;
            //PC remotePC = FindPC(getServerStatusRsp.GetPeer());
            PC remotePC = FindPC(getServerStatusRsp.GetMAC());
            if (remotePC == null) {
                remotePC = new PC(getServerStatusRsp.GetMAC());
                remotePCs.add(remotePC);
                newInstance = true;
            }
            boolean changed = false;
            if (getServerStatusRsp.GetStatus() == GetServerStatusRsp.Status.ShutingDown.ordinal()) {
                changed |= remotePC.SetStatus(PC.Status.ShuttingDown);
            } else {
                changed |= remotePC.SetStatus(PC.Status.ON);
            }
            remotePC.MarkLastCommunication();
            changed |= remotePC.SetShutdownDelay(getServerStatusRsp.GetShutdownIn());
            changed |= remotePC.SetAddress(getServerStatusRsp.GetPeer());
            changed |= remotePC.SetName(getServerStatusRsp.GetName());
            if (newInstance) {
                LOG.info("Added " + remotePC.toString());
                NotifyPCAdded(remotePC, true);
            } else if (changed) {
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
        if (setShutdownDelayRsp.GetPeer() == null) {
            LOG.error("SetShutdownDelayRsp peer is null!");
            return;
        }
        PC remotePC = FindPC(setShutdownDelayRsp.GetPeer());
        if (remotePC == null) {
            LOG.error("SetShutdownDelayRsp peer not found " + setShutdownDelayRsp.GetPeer().getHostAddress());
            return;
        }
        boolean changed = remotePC.SetShutdownDelay(setShutdownDelayRsp.GetShutdownDelay());
        NotifyPCChanged(remotePC);
    }

    public void terminate() {
        running = false;
    }

    @Override
    public void run() {
        running = true;
        if(EnsureCommLinkIsValid() == false) {
            return;
        }
        // There may be some packets lost after enabling WiFi, so send multiple discovery request
        // to all.
        int numInitialDiscoveryRequests = 15;
        while (running) {
            try {
                Date now = new Date();
                if (numInitialDiscoveryRequests > 0) {
                    SendGetServerStatus_Broadcast();
                    lastMonitoringSent = now;
                    numInitialDiscoveryRequests--;
                }
                else {
                    if (lastMonitoringSent == null || (now.getTime() - lastMonitoringSent.getTime()) > monitoringPeriod) {
                        if (selectedPC != null) {
                            SendGetServerStatus(selectedPC);
                            lastMonitoringSent = now;
                        }
                    }
                }
                if (remotePCs == null) {
                    LOG.warn("remotePCs is null!");
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
            } catch (Exception e) {
                LOG.info("PCManager Periodic failed:" + e.toString());
            }
        }
        if (commLink != null) {
            try {
                commLink.terminate();
                commLink.join();
                commLink = null;
            } catch (InterruptedException ex) {
                LOG.info("Failed to stop commLink");
            }
        }

    }

    protected void NotifyPCAdded(PC pc, boolean notifyDb) {
        if (notifyDb) {
            dataSource.AddPC(pc);
        }
        if (listener != null) {
            listener.PCAdded(pc);
        } else {
            LOG.error("NotifyPCAdded listener is NULL!!");
        }
    }

    protected void NotifyPCChanged(PC pc) {
        if (listener != null) {
            listener.PCChanged(pc);
        } else {
            LOG.error("NotifyPCChanged listener is NULL!!");
        }
    }

    private boolean EnsureCommLinkIsValid() {
        try {
            if(commLink == null) {
                commLink = new CommLink(localPort, this);
                LOG.info("Create CommLink localPort=" + localPort);
            }
        } catch (SocketException e) {
            LOG.warn("Failed to create CommLink");
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
