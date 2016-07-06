package org.eclipse.kura.protocol.inemo.message;

public class SetDurationRequestMessage extends SetDurationMessage{

	public SetDurationRequestMessage() {
		super();
		this.setLenght(0x03);
		int[] data= new int[3];
		data[0]= super.messageId;
		data[1]= 0x00;
		data[2]= 0x06;
		this.setData(data);
	}
}
