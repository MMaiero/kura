package com.eurotech.example.modbus.slave;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ModbusProtocolSlaveImpl implements ModbusProtocol{

	private static final Logger s_logger = LoggerFactory.getLogger(ModbusProtocolSlaveImpl.class);

	@Override
	public boolean[] readCoils(int unitAddr, int dataAddress, int count)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean[] readDiscreteInputs(int unitAddr, int dataAddress, int count)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeSingleCoil(int unitAddr, int dataAddress, boolean data)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeMultipleCoils(int unitAddr, int dataAddress, boolean[] data)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub

	}

	@Override
	public byte[] readHoldingRegisters(int unitAddr, int dataAddress, int count, ModbusSlavePreferences msp)
			throws ModbusProtocolException {
		s_logger.info("Received readHoldingRegisters request from Master");
		int[] data=new int[15];
		data[0] = msp.getPowerOut(); //Power Out [W]
		data[1] = msp.getTimeToRecharge(); //Time to recharge [s]
		data[2] = msp.getEnergyOut(); //Energy Out [Wh]
		data[3] = msp.getPowerPV(); //Power PV Out [W]
		
		int faultFlag= msp.isFaultFlag() ? 1 : 0;
		int rechargeAvailable= msp.isRechargeAvailable() ? 1 : 0;
		int rechargeInProgress= msp.isRechargeInProgress() ? 1 : 0;
		int pvSystemActive= msp.isPvSystemActive() ? 1 : 0;
		int auxChargerActive= msp.isAuxChargerActive() ? 1 : 0;
		int storageBatteryContractorStatus= msp.isStorageBatteryContactorStatus() ? 1 : 0;
		int converterContractorStatus= msp.isConverterContactorStatus() ? 1 : 0;
		data[4] = faultFlag 
				+ (rechargeAvailable << 1) 
				+ (rechargeInProgress << 2) 
				+ (pvSystemActive << 3) 
				+ (auxChargerActive << 4) 
				+ (storageBatteryContractorStatus << 5) 
				+ (converterContractorStatus << 6); //Status flag
		
		data[5] = msp.getFaultString1(); //Fault String 1
		data[6] = msp.getFaultString2(); //Fault String 2
		data[7] = msp.getIgbtTemp(); //IGBT_temp [°C]
		data[8] = msp.getStorageTemp(); //Storage temp [°C]
		data[9] = msp.getStorageBatterySoc(); //Storage battery SOC [%]
		data[10] = msp.getvOut(); //V_Out [V]
		data[11] = msp.getStorageBatteryV(); //Storage_Battery_V [V]
		data[12] = msp.getPvSystemV(); //PV_System_V [V]
		data[13] = msp.getiOut(); //I_Out [A]
		data[14] = msp.getStorageBatteryI(); //Storage_Battery_I [A]
		
		s_logger.info("Preparing answer to Master: ");
		s_logger.info("Power Out [W]: {}", data[0]);
		s_logger.info("Time to recharge [s]: {}", data[1]);
		s_logger.info("Energy Out [Wh]: {}", data[2]);
		s_logger.info("Power PV Out [W]: {}", data[3]);
		
		s_logger.info("Status flag -> Fault_flag: {}", faultFlag);
		s_logger.info("Status flag -> Recharge_Available: {}", rechargeAvailable);
		s_logger.info("Status flag -> Recharge_In_Progress: {}", rechargeInProgress);
		s_logger.info("Status flag -> PV_System_Active: {}", pvSystemActive);
		s_logger.info("Status flag -> Aux_Charger_Active: {}", auxChargerActive);
		s_logger.info("Status flag -> Storage_Battery_Contactor_Status: {}", storageBatteryContractorStatus);
		s_logger.info("Status flag -> Converter_Contactor_Status: {}", converterContractorStatus);
		
		s_logger.info("Fault String 1: {}", data[5]);
		s_logger.info("Fault String 2: {}", data[6]);
		s_logger.info("IGBT_temp [°C]: {}", data[7]);
		s_logger.info("Storage temp [°C]: {}", data[8]);
		s_logger.info("Storage battery SOC [%]: {}", data[9]);
		s_logger.info("V_Out [V]: {}", data[10]);
		s_logger.info("Storage_Battery_V [V]: {}", data[11]);
		s_logger.info("PV_System_V [V]: {}", data[12]);
		s_logger.info("I_Out [A]: {}", data[13]);
		s_logger.info("Storage_Battery_I [A]: {}", data[14]);
		
		byte[] cmd = new byte[2*count + 3];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.READ_HOLDING_REGS;
		cmd[2] = (byte) (2*count);
		
		cmd[3] = (byte) ((data[0] >> 8) & 0xFF); 
		cmd[4] = (byte) ((data[0]) & 0xFF); 
		
		cmd[5] = (byte) ((data[1] >> 8) & 0xFF); 
		cmd[6] = (byte) ((data[1]) & 0xFF);
		
		cmd[7] = (byte) ((data[2] >> 8) & 0xFF);
		cmd[8] = (byte) ((data[2]) & 0xFF);
		cmd[9] = (byte) ((data[3] >> 8) & 0xFF);
		cmd[10] = (byte) ((data[3]) & 0xFF);
		
		cmd[11] = (byte) ((data[4] >> 8) & 0xFF);
		cmd[12] = (byte) ((data[4]) & 0xFF);
		
		cmd[13] = (byte) ((data[5] >> 8) & 0xFF);
		cmd[14] = (byte) ((data[5]) & 0xFF);
		cmd[15] = (byte) ((data[6] >> 8) & 0xFF);
		cmd[16] = (byte) ((data[6]) & 0xFF);
		cmd[17] = (byte) ((data[7] >> 8) & 0xFF);
		cmd[18] = (byte) ((data[7]) & 0xFF);
		cmd[19] = (byte) ((data[8] >> 8) & 0xFF);
		cmd[20] = (byte) ((data[8]) & 0xFF);
		cmd[21] = (byte) ((data[9] >> 8) & 0xFF);
		cmd[22] = (byte) ((data[9]) & 0xFF);
		cmd[23] = (byte) ((data[10] >> 8) & 0xFF);
		cmd[24] = (byte) ((data[10]) & 0xFF);
		cmd[25] = (byte) ((data[11] >> 8) & 0xFF);
		cmd[26] = (byte) ((data[11]) & 0xFF);
		cmd[27] = (byte) ((data[12] >> 8) & 0xFF);
		cmd[28] = (byte) ((data[12]) & 0xFF);
		cmd[29] = (byte) ((data[13] >> 8) & 0xFF);
		cmd[30] = (byte) ((data[13]) & 0xFF);
		cmd[31] = (byte) ((data[14] >> 8) & 0xFF);
		cmd[32] = (byte) ((data[14]) & 0xFF);
		
		return cmd;
	}

	@Override
	public int[] readInputRegisters(int unitAddr, int dataAddress, int count)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeSingleRegister(int unitAddr, int dataAddress, int data)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean[] readExceptionStatus(int unitAddr)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModbusCommEvent getCommEventCounter(int unitAddr)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModbusCommEvent getCommEventLog(int unitAddr)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] writeMultipleRegister(int unitAddr, int dataAddress, byte[] data, int numRegisters)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub
		
		int startRecharge= data[0] & 0x01;
		int isBooked = (data[0] >> 1) & 0x01;
		int irradiation = (data[0] >> 2) & 0x03;
		int bookingHours = (data[3] & 0xFF);
		int bookingMinutes= (data[2] & 0xFF);
		int bookingMonth= (data[4] & 0xFF);
		int bookingDay= (data[5] & 0xFF);
		int bookingYear= buildShort(data[6], data[7]);
		
		int currentHours = (data[9] & 0xFF);
		int currentMinutes= (data[8] & 0xFF);
		int currentMonth= (data[10] & 0xFF);
		int currentDay= (data[11] & 0xFF);
		int currentYear= buildShort(data[12], data[13]);
		
		s_logger.info("Received from Master-> startRecharge: " + startRecharge +
				" isBooked: " + isBooked + 
				" irradiation: " + irradiation + 
				" bookingHours: " + bookingHours +
				" bookingMinutes: " + bookingMinutes +
				" bookingMonth: " + bookingMonth +
				" bookingDay: " + bookingDay +
				" bookingYear: " + bookingYear + 
				" currentHours: " + currentHours +
				" currentMinutes: " + currentMinutes +
				" currentMonth: " + currentMonth +
				" currentDay: " + currentDay +
				" currentYear: " + currentYear);
		
		byte[] cmd = new byte[6];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.PRESET_MULTIPLE_REGS;
		cmd[2] = (byte) ((dataAddress >> 8) & 0xFF);
		cmd[3] = (byte) (dataAddress & 0xFF);
		
		cmd[4] = (byte) ((numRegisters >> 8) & 0xFF);
		cmd[5] = (byte) (numRegisters & 0xFF);
		
		return cmd;
	}
	
	public int buildShort(byte high, byte low){
		return ((0xFF & (int) high) << 8) + ((0xFF & (int) low));
	}
}
