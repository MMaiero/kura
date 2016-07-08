package org.eclipse.kura.protocol.inemo.message;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

public class PingMessage extends INemoMessage {
	protected int messageId= 0x00;
	
	public String getStatus() throws KuraException {
		int[] data= getData();
		if (data[0] == 0x00) {
			return "Ok";
		} 
		throw new KuraException(KuraErrorCode.INVALID_MESSAGE_EXCEPTION);
	}

}
