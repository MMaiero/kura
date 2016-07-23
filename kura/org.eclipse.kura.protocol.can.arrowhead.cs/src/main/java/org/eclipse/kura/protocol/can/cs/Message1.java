package org.eclipse.kura.protocol.can.cs;

public class Message1 {
    public static final String POWER_OUT                = "power.out";
    public static final String TIME_TO_RECHARGE_MINUTES = "time.to.recharge.minutes";
    public static final String TIME_TO_RECHARGE_SECONDS = "time.to.recharge.seconds";
    public static final String ENERGY_OUT               = "energy.out";
    public static final String POWER_PV                 = "power.pv";

    private int powerOut;
    private int timeToRechargeMinutes;
    private int timeToRechargeSeconds;
    private int energyOut;
    private int powerPV;

    public int getPowerOut() {
        return powerOut;
    }

    public void setPowerOut(int powerOut) {
        this.powerOut = powerOut;
    }

    public int getTimeToRechargeMinutes() {
        return timeToRechargeMinutes;
    }

    public void setTimeToRechargeMinutes(int timeToRechargeMinutes) {
        this.timeToRechargeMinutes = timeToRechargeMinutes;
    }

    public int getTimeToRechargeSeconds() {
        return timeToRechargeSeconds;
    }

    public void setTimeToRechargeSeconds(int timeToRechargeSeconds) {
        this.timeToRechargeSeconds = timeToRechargeSeconds;
    }

    public int getEnergyOut() {
        return energyOut;
    }

    public void setEnergyOut(int energyOut) {
        this.energyOut = energyOut;
    }

    public int getPowerPV() {
        return powerPV;
    }

    public void setPowerPV(int powerPV) {
        this.powerPV = powerPV;
    }
}
