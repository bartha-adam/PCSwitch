package pcswitch.common;

import java.security.InvalidParameterException;
import java.util.Arrays;

public class MACAddress {

	public static final int MAC_SIZE =6;
	public byte[] bytes;
	
	public MACAddress() {
		bytes = new byte[MAC_SIZE];
		for(int i = 0; i < MAC_SIZE; i++)
			bytes[i] = 0;
	}
	
	public MACAddress(byte[] MAC) {
		bytes = Arrays.copyOf(MAC, MAC_SIZE);
	}
	
	//Format xx-xx-xx-xx-xx-xx
	public MACAddress(String str) {
		bytes = new byte[MAC_SIZE];
		String[] tokens = str.split("-");
		if(tokens.length != MAC_SIZE)
			throw new InvalidParameterException();
		for(int i = 0; i < MAC_SIZE; i++){
			String token = tokens[i];
			if(token.length() != 2)
				throw new InvalidParameterException();
			//bytes[i] = Byte.parseByte(token, 16);
			bytes[i] = (byte) ((Character.digit(token.charAt(0), 16) << 4)
					          + Character.digit(token.charAt(1), 16));
		}
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			sb.append(String.format("%02X%s", bytes[i], (i < bytes.length - 1) ? "-" : ""));		
		}
		return sb.toString();
	}
	
	public boolean equals(Object obj) {
        if (obj instanceof MACAddress){
        	MACAddress other = (MACAddress)obj;
        	for(int i = 0; i < MAC_SIZE; i++)
        		if(other.bytes[i] != this.bytes[i])
        			return false;
        	return true;
        }
        else
            return false;
    }
	

}
