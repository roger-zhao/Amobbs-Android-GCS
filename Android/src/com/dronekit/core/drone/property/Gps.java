package com.dronekit.core.drone.property;

import com.dronekit.core.helpers.coordinates.LatLong;

/**
 * Stores GPS information.
 */
public class Gps {

    public static final String LOCK_2D = "2D";
    public static final String LOCK_3D = "3D";
    public static final String LOCK_3D_DGPS = "3D+DGPS";
    public static final String LOCK_3D_RTK = "3D+RTK";
    public static final String NO_FIX = "NoFix";

    private final static int LOCK_2D_TYPE = 2;
    private final static int LOCK_3D_TYPE = 3;
    private final static int LOCK_3D_DGPS_TYPE = 4;
    private final static int LOCK_3D_RTK_TYPE = 5;

    private double mGpsEph;
    private int mSatCount;
    private int mFixType;
    private LatLong mPosition;

    public Gps() {
    }

    public Gps(LatLong position, double gpsEph, int satCount, int fixType) {
        mPosition = position;
        mGpsEph = gpsEph;
        mSatCount = satCount;
        mFixType = fixType;
    }

    public Gps(double latitude, double longitude, double gpsEph, int satCount, int fixType) {
        this(new LatLong(latitude, longitude), gpsEph, satCount, fixType);
    }

    public boolean isValid() {
        return mPosition != null;
    }

    public double getGpsEph() {
        return mGpsEph;
    }

    public void setGpsEph(double mGpsEph) {
        this.mGpsEph = mGpsEph;
    }

    public int getSatellitesCount() {
        return mSatCount;
    }

    public int getFixType() {
        return mFixType;
    }

    public void setFixType(int mFixType) {
        this.mFixType = mFixType;
    }

    public String getFixStatus() {
        switch (mFixType) {
            case LOCK_2D_TYPE:
                return LOCK_2D;

            case LOCK_3D_TYPE:
                return LOCK_3D;

            case LOCK_3D_DGPS_TYPE:
                return LOCK_3D_DGPS;

            case LOCK_3D_RTK_TYPE:
                return LOCK_3D_RTK;

            default:
                return NO_FIX;
        }
    }

    public LatLong getPosition() {
        return mPosition;
    }

    public void setPosition(LatLong mPosition) {
        this.mPosition = mPosition;
    }

    public void setSatCount(int mSatCount) {
        this.mSatCount = mSatCount;
    }

    /**
     * @return True if there's a 3D GPS lock, false otherwise.
     * @since 2.6.8
     */
    public boolean has3DLock() {
        return (mFixType == LOCK_3D_TYPE) || (mFixType == LOCK_3D_DGPS_TYPE) || (mFixType == LOCK_3D_RTK_TYPE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Gps)) return false;

        Gps gps = (Gps) o;

        if (mFixType != gps.mFixType) return false;
        if (Double.compare(gps.mGpsEph, mGpsEph) != 0) return false;
        if (mSatCount != gps.mSatCount) return false;
        return mPosition != null ? mPosition.equals(gps.mPosition) : gps.mPosition == null;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(mGpsEph);
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + mSatCount;
        result = 31 * result + mFixType;
        result = 31 * result + (mPosition != null ? mPosition.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Gps{" +
                "mGpsEph=" + mGpsEph +
                ", mSatCount=" + mSatCount +
                ", mFixType=" + mFixType +
                ", mPosition=" + mPosition +
                '}';
    }
}
