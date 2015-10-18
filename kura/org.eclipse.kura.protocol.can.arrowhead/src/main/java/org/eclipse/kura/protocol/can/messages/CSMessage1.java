package org.eclipse.kura.protocol.can.messages;

import org.eclipse.kura.protocol.can.CanMessage;
import org.eclipse.kura.protocol.can.arrowhead.CanSocketTest;
import org.eclipse.kura.protocol.can.cs.data.CSDataSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSMessage1 {
	private static final Logger s_logger = LoggerFactory.getLogger(CanSocketTest.class);
	
	public static void parseCanMessage(CanMessage cm, boolean isBigEndian, CSDataSnapshot csReceivedData){
		byte[] b = null;
		b = cm.getData();
		if(b!=null && b.length == 8){
			StringBuilder sb = new StringBuilder("received : ");

			int powerOut;
			if (isBigEndian) {
				powerOut= MessageUtils.buildShort(b[0], b[1]);
			} else {
				powerOut= MessageUtils.buildShort(b[1], b[0]);
			}
			int minutesToRecharge= b[2];
			int secondsToRecharge= b[3];

			int energyOut;
			if (isBigEndian) {
				energyOut= MessageUtils.buildShort(b[4], b[5]);
			} else {
				energyOut= MessageUtils.buildShort(b[5], b[4]);
			}
			int powerPV;
			if (isBigEndian) {
				powerPV= MessageUtils.buildShort(b[6], b[7]);
			} else {
				powerPV= MessageUtils.buildShort(b[7], b[6]);
			}

			sb.append("Power out: " + powerOut + " W, ");
			sb.append("Minutes to recharge: " + minutesToRecharge + " minutes, ");
			sb.append("Seconds to recharge: " + secondsToRecharge + " seconds, ");
			sb.append("Energy out: " + energyOut + " Wh, ");
			sb.append("Power PV: " + powerPV + " W");

			//			for(int i=0; i<b.length; i++){
			//				sb.append(b[i]);
			//				sb.append(";");
			//			}
			sb.append(" on id = ");
			sb.append(cm.getCanId());			
			s_logger.debug(sb.toString());

			csReceivedData.setPowerOut(powerOut);
			csReceivedData.setTimeToRecharge(minutesToRecharge, secondsToRecharge);
			csReceivedData.setEnergy(energyOut);
			csReceivedData.setPowerPV(powerPV);
		}
	}

}
