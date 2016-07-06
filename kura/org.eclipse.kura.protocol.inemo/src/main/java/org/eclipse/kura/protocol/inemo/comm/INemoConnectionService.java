package org.eclipse.kura.protocol.inemo.comm;

import java.io.IOException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.protocol.inemo.INemoTest;
import org.osgi.service.io.ConnectionFactory;

public interface INemoConnectionService {

	public void openConnection(SerialInterfaceParameters serialParams,  ConnectionFactory connectionFactory) throws IOException;
	
	public void closeConnection() throws IOException;
	
	public void sendMessage(byte[] message) throws KuraException, IOException;
	
	public void receiveMessage(INemoTest callback) throws KuraException, IOException;
}