package gr3go.pcswitch;

import java.net.InetAddress;
import java.util.Date;

import pcswitch.common.MACAddress;

public class PC {

    public enum Status {
        ON,
        OFF,
        ShuttingDown
    }

    protected InetAddress address;
    protected Status status;
    protected Date lastCommunication;
    protected int shutdownDelay;
    protected MACAddress macAddress = null;
    protected long dbId;

    public PC(MACAddress address) {
        macAddress = address;
        status = Status.OFF;
        lastCommunication = new Date();
        shutdownDelay = -1;
        dbId = -1;
    }

    public void MarkLastCommunication() {
        lastCommunication = new Date();
    }

    public Date LastCommunication() {
        return lastCommunication;
    }

    public String toString() {
        String result = "PC[IP=" + address.getHostAddress();
        result += " MAC=";
        if (macAddress == null) {
            result += "N/A";
        } else {
            result += macAddress.toString();
        }
        result += " DbId=" + dbId;
        result += " Status=";
        if (status == Status.ON) {
            result += "ON";
        } else if (status == Status.OFF) {
            result += "OFF";
        } else if (status == Status.ShuttingDown) {
            result += "ShuttingDown";
        } else {
            result += "Unknown(" + status + ")";
        }
        result += " ShutDownIn=";
        if (shutdownDelay > 0) {
            result += shutdownDelay + "s";
        } else {
            result += "OFF";
        }
        result += "]";
        return result;
    }

    public boolean SetAddress(InetAddress address_) {
        if (address != null) {
            if (address.equals(address_))
                return false;
        }
        address = address_;
        return true;
    }

    public InetAddress GetAddress() {
        return address;
    }

    public String GetAddressStr() {
        if (address != null) {
            return address.getHostAddress();
        } else {
            return new String();
        }
    }

    public Status GetStatus() {
        return status;
    }

    public boolean SetStatus(Status status) {
        if (this.status == status) {
            return false;
        }
        this.status = status;
        return true;
    }

    public boolean SetShutdownDelay(int shutdownDelay) {
        if (this.shutdownDelay == shutdownDelay) {
            return false;
        }
        this.shutdownDelay = shutdownDelay;
        return true;
    }

    public boolean IsShutdownConfigured() {
        return (shutdownDelay >= 0);
    }

    public int GetShutdownDelay() {
        return shutdownDelay;
    }

    public MACAddress GetMACAddress() {
        return macAddress;
    }

    public void SetDBId(long id) {
        dbId = id;
    }

    public long GetDBId() {
        return dbId;
    }
}
