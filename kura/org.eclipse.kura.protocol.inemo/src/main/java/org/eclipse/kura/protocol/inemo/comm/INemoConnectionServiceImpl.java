package org.eclipse.kura.protocol.inemo.comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.protocol.inemo.INemoTest;
import org.eclipse.kura.protocol.inemo.message.GetDurationMessage;
import org.eclipse.kura.protocol.inemo.message.GetEventInfoMessage;
import org.eclipse.kura.protocol.inemo.message.GetSnapshotMessage;
import org.eclipse.kura.protocol.inemo.message.GetThresholdMessage;
import org.eclipse.kura.protocol.inemo.message.INemoMessage;
import org.eclipse.kura.protocol.inemo.message.PingMessage;
import org.eclipse.kura.protocol.inemo.message.SetDurationMessage;
import org.eclipse.kura.protocol.inemo.message.SetThresholdMessage;
import org.eclipse.kura.protocol.inemo.message.SyncPollingMessage;
import org.eclipse.kura.protocol.inemo.message.TakeSnapshotMessage;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class INemoConnectionServiceImpl implements INemoConnectionService{
	private static final Logger s_logger = LoggerFactory.getLogger(INemoConnectionServiceImpl.class);

	private CommConnection m_connection;
	private InputStream m_iStream;
	private OutputStream m_oStream;

	@Override
	public void openConnection(SerialInterfaceParameters serialParams, ConnectionFactory connectionFactory) throws IOException {
		String device= serialParams.getDevice();
		int baudRate= serialParams.getBaudrate();
		int dataBits= serialParams.getDataBits();
		int stopBits= serialParams.getStopBits();
		int parity= serialParams.getParity();
		int timeout= serialParams.getTimeout();

		String uri = new CommURI.Builder(device).withBaudRate(baudRate).withDataBits(dataBits).withStopBits(stopBits).withParity(parity).withTimeout(timeout).build().toString();
		m_connection = (CommConnection) connectionFactory.createConnection(uri, 1, false);
		m_iStream = m_connection.openInputStream();
		m_oStream = m_connection.openOutputStream();
	}

	@Override
	public void closeConnection() throws IOException {
		if(m_iStream != null){
			m_iStream.close();
		}
		if(m_oStream != null){
			m_oStream.close();
		}
		m_iStream = null;
		m_oStream = null;

		if(m_connection != null){
			m_connection.close();
		}
		m_connection = null;
	}

	@Override
	public void sendMessage(byte[] message) throws KuraException, IOException {

		if (m_oStream != null) {
			m_oStream.write(message);
		}
	}

	@Override
	public void receiveMessage(INemoTest callback) throws KuraException, IOException {
		INemoMessage message= receiveMessage();
		callback.messageCallback(message);
	}
	
	@Override
	public INemoMessage receiveMessage() throws KuraException, IOException {
		boolean echo= false;

		if (m_iStream != null) {


			int c = -1;
			Vector<Integer> sb= new Vector<Integer>();

			while (m_iStream != null) {

				if (m_iStream.available() != 0) {
					c = m_iStream.read();
				} else {
					try {
						Thread.sleep(100);
						continue;
					} catch (InterruptedException e) {
						return null;
					}
				}

				if (echo && m_oStream != null) {
					m_oStream.write((char) c);
				}
				//s_logger.info("Received: {}", sb.toString());
				if (c == 0x76) {
					sb = new Vector<Integer>();
					sb.addElement(c);
				} else {
					sb.addElement(c);
				}

				if (sb.size() > 2) {
					int size= sb.elementAt(1);
					if (sb.size()-3 == size) {
						//parse body
						int messageID = sb.elementAt(2);
						int[] messageBody = new int[size - 1];
						int messageIndex= 3;
						for (int i= 0; i < size - 1 ; i++) {
							messageBody[i]= sb.elementAt(messageIndex);
							messageIndex++;
							
						}
						INemoMessage message= null;
						switch (messageID) {
							case 0x00:  message= new PingMessage();
										message.setLenght(size);
										message.setData(messageBody);
										break;
							case 0x04:  message= new SyncPollingMessage();
										message.setLenght(size);
										message.setData(messageBody);
										break;
							case 0x05:  message= new GetEventInfoMessage();
										message.setLenght(size);
										message.setData(messageBody);
										break;
							case 0x06:  message= new GetThresholdMessage();
										message.setLenght(size);
										message.setData(messageBody);
										break;
							case 0x07:  message= new SetThresholdMessage();
										message.setLenght(size);
										message.setData(messageBody);
										break;
							case 0x10:  message= new GetDurationMessage();
										message.setLenght(size);
										message.setData(messageBody);
										break;
							case 0x11:  message= new SetDurationMessage();
										message.setLenght(size);
										message.setData(messageBody);
										break;
							case 0x1E:  message= new TakeSnapshotMessage();
										message.setLenght(size);
										message.setData(messageBody);
										break;
							case 0x1F:  message= new GetSnapshotMessage();
										message.setLenght(size);
										message.setData(messageBody);
										break;
							default: 	break;
						}
						s_logger.info("Received: {}", sb.toString());
						return message;
					}
				}					
			}
		} 

		return null;
	}
}
