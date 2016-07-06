package org.eclipse.kura.protocol.inemo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.protocol.inemo.comm.INemoConnectionService;
import org.eclipse.kura.protocol.inemo.comm.INemoConnectionServiceImpl;
import org.eclipse.kura.protocol.inemo.comm.SerialInterfaceParameters;
import org.eclipse.kura.protocol.inemo.message.INemoMessage;
import org.eclipse.kura.protocol.inemo.message.PingMessage;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class INemoTest implements ConfigurableComponent, CloudClientListener {
	private static final Logger s_logger = LoggerFactory.getLogger(INemoTest.class);

	// Cloud Application identifier
	private static final String APP_ID = "INEMO_TEST";

	// Publishing Property Names
	private static final String   PUBLISH_TOPIC_PROP_NAME  = "publish.semanticTopic";
	private static final String   PUBLISH_QOS_PROP_NAME    = "publish.qos";
	private static final String   PUBLISH_RETAIN_PROP_NAME = "publish.retain";

	private static final String   SERIAL_DEVICE_PROP_NAME= "serial.device";
	private static final String   SERIAL_BAUDRATE_PROP_NAME= "serial.baudrate";
	private static final String   SERIAL_DATA_BITS_PROP_NAME= "serial.data-bits";
	private static final String   SERIAL_PARITY_PROP_NAME= "serial.parity";
	private static final String   SERIAL_STOP_BITS_PROP_NAME= "serial.stop-bits";

	private static final String   SERIAL_ECHO_PROP_NAME= "serial.echo";
	private static final String   SERIAL_CLOUD_ECHO_PROP_NAME= "serial.cloud-echo";


	private CloudService m_cloudService;
	private CloudClient m_cloudClient;

	private INemoConnectionService m_connService;

	private ConnectionFactory m_connectionFactory;


	private ScheduledExecutorService m_writerWorker;
	private ScheduledExecutorService m_listenerWorker;
	private Future<?>           m_writerHandle;
	private Future<?>           m_listenerHandle;

	private Map<String, Object> m_properties;

	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------

	public INemoTest(){
		super();
		m_writerWorker = Executors.newSingleThreadScheduledExecutor();
		m_listenerWorker = Executors.newSingleThreadScheduledExecutor();
	}

	public void setCloudService(CloudService cloudService) {
		m_cloudService = cloudService;
	}

	public void unsetCloudService(CloudService cloudService) {
		m_cloudService = null;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.m_connectionFactory = connectionFactory;
	}

	public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
		this.m_connectionFactory = null;
	}

	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext, Map<String,Object> properties) {
		s_logger.info("Activating iNemo test...");

		m_properties = new HashMap<String, Object>();

		// get the mqtt client for this application
		try  {

			// Acquire a Cloud Application Client for this Application 
			s_logger.info("Getting CloudApplicationClient for {}...", APP_ID);
			m_cloudClient = m_cloudService.newCloudClient(APP_ID);
			m_cloudClient.addCloudClientListener(this);

			// Don't subscribe because these are handled by the default 
			// subscriptions and we don't want to get messages twice			
			doUpdate(properties);
		}
		catch (Exception e) {
			s_logger.error("Error during component activation", e);
			throw new ComponentException(e);
		}
		s_logger.info("Activating INEMO test... Done.");
	}

	protected void deactivate(ComponentContext componentContext){
		s_logger.info("Deactivating INEMO test...");

		m_listenerHandle.cancel(true);
		m_writerHandle.cancel(true);

		// shutting down the worker and cleaning up the properties
		m_writerWorker.shutdownNow();
		m_listenerWorker.shutdownNow();

		// Releasing the CloudApplicationClient
		s_logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
		m_cloudClient.release();

		closePort();

		s_logger.info("Deactivating INEMO test... Done.");
	}	

	public void updated(Map<String,Object> properties){
		s_logger.info("Updated INEMO test...");

		// try to kick off a new job
		doUpdate(properties);
		s_logger.info("Updated INEMO test... Done.");
	}


	// ----------------------------------------------------------------
	//
	//   Cloud Application Callback Methods
	//
	// ----------------------------------------------------------------

	@Override
	public void onControlMessageArrived(String deviceId, String appTopic,
			KuraPayload msg, int qos, boolean retain) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessageArrived(String deviceId, String appTopic,
			KuraPayload msg, int qos, boolean retain) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnectionLost() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnectionEstablished() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessageConfirmed(int messageId, String appTopic) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessagePublished(int messageId, String appTopic) {
		// TODO Auto-generated method stub

	}

	// ----------------------------------------------------------------
	//
	//   Private Methods
	//
	// ----------------------------------------------------------------

	/**
	 * Called after a new set of properties has been configured on the service
	 */
	private void doUpdate(Map<String, Object> properties) {
		try {

			for (String s : properties.keySet()) {
				s_logger.info("Update - "+s+": "+properties.get(s));
			}

			// cancel a current worker handle if one if active
			if (m_listenerHandle != null) {
				m_listenerHandle.cancel(true);
			}
			if (m_writerHandle != null) {
				m_writerHandle.cancel(true);
			}

			String topic = (String) m_properties.get(PUBLISH_TOPIC_PROP_NAME);
			if (topic != null) {
				try {
					m_cloudClient.unsubscribe(topic);
				} catch (KuraException e) {
					s_logger.error("Unsubscribe failed", e);
				}
			}

			m_connService= new INemoConnectionServiceImpl();
			closePort();

			m_properties.clear();
			m_properties.putAll(properties);

			openPort();

			Boolean cloudEcho = (Boolean) m_properties.get(SERIAL_CLOUD_ECHO_PROP_NAME);
			if (cloudEcho) {
				try {
					m_cloudClient.subscribe(topic, 0);
				} catch (KuraException e) {
					s_logger.error("Subscribe failed", e);
				}
			}

			m_writerHandle = m_writerWorker.submit(new Runnable() {		
				@Override
				public void run() {
					boolean exec= true;
					while (exec) {
						doSerial();
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							exec = false;
						}
					}
				}
			});
			
			final INemoTest callbackHandler= this;
			m_listenerHandle = m_listenerWorker.submit(new Runnable() {		
				@Override
				public void run() {
					boolean exec= true;
					while (exec) {
						try {
							m_connService.receiveMessage(callbackHandler);
						} catch (KuraException e) {
							s_logger.warn("Exception!!!!!!!");
						} catch (IOException e) {
							s_logger.warn("Exception!!!!!!!");
						}
					}
				}
			});
		} catch (Throwable t) {
			s_logger.error("Unexpected Throwable", t);
		}
	}

	private void openPort() {
		String port = (String) m_properties.get(SERIAL_DEVICE_PROP_NAME);

		if (port == null) {
			s_logger.info("Port name not configured");
			return;
		}

		int baudRate = Integer.valueOf((String) m_properties.get(SERIAL_BAUDRATE_PROP_NAME));
		int dataBits = Integer.valueOf((String) m_properties.get(SERIAL_DATA_BITS_PROP_NAME)); 
		int stopBits = Integer.valueOf((String) m_properties.get(SERIAL_STOP_BITS_PROP_NAME));

		String sParity = (String) m_properties.get(SERIAL_PARITY_PROP_NAME);

		int parity = CommURI.PARITY_NONE;
		if (sParity.equals("none")) {
			parity = CommURI.PARITY_NONE;
		} else if (sParity.equals("odd")) {
			parity = CommURI.PARITY_ODD;	
		} else if (sParity.equals("even")) {
			parity = CommURI.PARITY_EVEN;
		}

		int timeout= 2000;

		SerialInterfaceParameters sip= new SerialInterfaceParameters();
		sip.setDevice(port);
		sip.setBaudrate(baudRate);
		sip.setDataBits(dataBits);
		sip.setStopBits(stopBits);
		sip.setParity(parity);
		sip.setTimeout(timeout);

		try {
			m_connService.openConnection(sip, m_connectionFactory);

			s_logger.info(port+" open");
		} catch (IOException e) {
			s_logger.error("Failed to open port", e);
			cleanupPort();
		}
	}

	private void cleanupPort() {
		try {
			s_logger.info("Closing connection...");
			m_connService.closeConnection();
			s_logger.info("Closed connection");
		} catch (IOException e) {
			s_logger.error("Cannot close connection", e);
		}
	}

	private void closePort() {
		cleanupPort();
	}

	private void doSerial() {				
		// fetch the publishing configuration from the publishing properties
//		String  topic  = (String) m_properties.get(PUBLISH_TOPIC_PROP_NAME);
//		Integer qos    = (Integer) m_properties.get(PUBLISH_QOS_PROP_NAME);
//		Boolean retain = (Boolean) m_properties.get(PUBLISH_RETAIN_PROP_NAME);
//
//		Boolean echo = (Boolean) m_properties.get(SERIAL_ECHO_PROP_NAME);
		
		int header= 0x76;
		int length= 0x01;
		int payload= 0x00;
		int checksum= (header + length + payload) & 0xFF;
		
		byte bMessage[] = new byte[4];
		bMessage[0]= (byte) header;
		bMessage[1]= (byte) length;
		bMessage[2]= (byte) payload;
		bMessage[3]= (byte) checksum;
		
		try {
			m_connService.sendMessage(bMessage);
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void messageCallback(INemoMessage message) {
		if (message instanceof PingMessage) {
			PingMessage pingMessage= (PingMessage) message;
			s_logger.info("Received: {}", pingMessage.getStatus());
		}
	}
}