package org.eclipse.kura.protocol.can.messages;

import org.eclipse.kura.protocol.can.CanMessage;
import org.eclipse.kura.protocol.can.arrowhead.ArrowheadCanSocketImpl;
import org.eclipse.kura.protocol.can.cs.data.PublicCSDataSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages statically 0x100 messages received from a Charging Station
 * The output resulting from parsing will consist of the following values in
 * csReceivedData object:
 * <li>Power_Out</li>
 * <li>Time_to_Recharge_Estimated</li>
 * <li>Energy_Out</li>
 * <li>Power_PV</li>
 *
 */
public class CSMessage0x100 {
    private static final Logger s_logger = LoggerFactory.getLogger(ArrowheadCanSocketImpl.class);

    private CSMessage0x100() {
    }

    public static void parseCanMessage(CanMessage cm, boolean isBigEndian, PublicCSDataSnapshot publicCSReceivedData) {
        byte[] b = cm.getData();
        if (b != null && b.length == 8) {
            StringBuilder sb = new StringBuilder("received 0x100: ");

            int powerOut;
            if (isBigEndian) {
                powerOut = MessageUtils.buildShort(b[0], b[1]);
            } else {
                powerOut = MessageUtils.buildShort(b[1], b[0]);
            }
            int minutesToRecharge = b[2];
            int secondsToRecharge = b[3];

            int energyOut;
            if (isBigEndian) {
                energyOut = MessageUtils.buildShort(b[4], b[5]);
            } else {
                energyOut = MessageUtils.buildShort(b[5], b[4]);
            }
            int powerPV;
            if (isBigEndian) {
                powerPV = MessageUtils.buildShort(b[6], b[7]);
            } else {
                powerPV = MessageUtils.buildShort(b[7], b[6]);
            }

            sb.append("Power out: " + powerOut + " W, ");
            sb.append("Minutes to recharge: " + minutesToRecharge + " minutes, ");
            sb.append("Seconds to recharge: " + secondsToRecharge + " seconds, ");
            sb.append("Energy out: " + energyOut + " Wh, ");
            sb.append("Power PV: " + powerPV + " W");
            
            sb.append(" on id = ");
            sb.append(cm.getCanId());
            s_logger.debug(sb.toString());

            publicCSReceivedData.setPowerOut(powerOut);
            publicCSReceivedData.setTimeToRecharge(minutesToRecharge, secondsToRecharge);
            publicCSReceivedData.setEnergy(energyOut);
            publicCSReceivedData.setPowerPV(powerPV);
        }
    }

}
