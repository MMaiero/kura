package org.eclipse.kura.protocol.can.messages;

import org.eclipse.kura.protocol.can.arrowhead.CanSocketTest;
import org.eclipse.kura.protocol.can.recharge.CurrentDateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GWMessage3 {
	private static final Logger s_logger = LoggerFactory.getLogger(CanSocketTest.class);

	public static byte[] createMessage(int id, CurrentDateInfo currentDateInfo, boolean isBigEndian){
		StringBuilder sb = new StringBuilder("Trying to create can frame message 3 with value = ");
		byte bCurrentDate[] = new byte[4];
		int currentDateDay= currentDateInfo.getCurrentDateDay();
		int currentDateMonth= currentDateInfo.getCurrentDateMonth();
		int currentDateYear= currentDateInfo.getCurrentDateYear();

		bCurrentDate[0]= (byte) currentDateDay; //Current date: day
		bCurrentDate[1]= (byte) currentDateMonth; //Current date: month

		if (isBigEndian) {
			bCurrentDate[2]= (byte) ((currentDateYear >> 8) & 0xFF); //Current date: year
			bCurrentDate[3]= (byte) (currentDateYear & 0xFF); //Current date: year
		} else {
			bCurrentDate[2]= (byte) (currentDateYear & 0xFF); //Current date: year
			bCurrentDate[3]= (byte) ((currentDateYear >> 8) & 0xFF); //Current date: year
		}

		sb.append("Current date: day " +     bCurrentDate[0] + ", ");
		sb.append("Current date: month " +   bCurrentDate[1] + ", ");
		if (isBigEndian) {
			sb.append("Current date: year " +    MessageUtils.buildShort(bCurrentDate[2], bCurrentDate[3]));
		} else {
			sb.append("Current date: year " +    MessageUtils.buildShort(bCurrentDate[3], bCurrentDate[2]));
		}

		sb.append(" and id = ");
		sb.append(id);
		s_logger.debug(sb.toString());
		
		return bCurrentDate;
	}

}
