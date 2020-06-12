package com.dronekit.core.drone.property;

import com.MAVLink.enums.EKF_STATUS_FLAGS;

import java.util.BitSet;

public class EkfStatus {

    private static final String TAG = EkfStatus.class.getSimpleName();
    private static final int FLAGS_BIT_COUNT = 16;
    private final BitSet flags;
    private float velocityVariance;
    private float horizontalPositionVariance;
    private float verticalPositionVariance;
    private float compassVariance;
    private float terrainAltitudeVariance;

    public EkfStatus() {
        this.flags = new BitSet(FLAGS_BIT_COUNT);
    }

    public EkfStatus(int flags, float compassVariance,
                     float horizontalPositionVariance, float terrainAltitudeVariance,
                     float velocityVariance, float verticalPositionVariance) {
        this();
        this.compassVariance = compassVariance;
        this.horizontalPositionVariance = horizontalPositionVariance;
        this.terrainAltitudeVariance = terrainAltitudeVariance;
        this.velocityVariance = velocityVariance;
        this.verticalPositionVariance = verticalPositionVariance;

        fromShortToBitSet(flags);
    }

    private void fromShortToBitSet(int flags) {
        final EkfFlags[] ekfFlags = EkfFlags.values();
        final int ekfFlagsCount = ekfFlags.length;

        for (int i = 0; i < ekfFlagsCount; i++) {
            this.flags.set(i, (flags & ekfFlags[i].value) != 0);
        }
    }

    public void setEkfStatusFlag(int flags) {
        fromShortToBitSet(flags);
    }

    public float getTerrainAltitudeVariance() {
        return terrainAltitudeVariance;
    }

    public void setTerrainAltitudeVariance(float terrainAltitudeVariance) {
        this.terrainAltitudeVariance = terrainAltitudeVariance;
    }

    public float getVelocityVariance() {
        return velocityVariance;
    }

    public void setVelocityVariance(float velocityVariance) {
        this.velocityVariance = velocityVariance;
    }

    public float getVerticalPositionVariance() {
        return verticalPositionVariance;
    }

    public void setVerticalPositionVariance(float verticalPositionVariance) {
        this.verticalPositionVariance = verticalPositionVariance;
    }

    public float getHorizontalPositionVariance() {
        return horizontalPositionVariance;
    }

    public void setHorizontalPositionVariance(float horizontalPositionVariance) {
        this.horizontalPositionVariance = horizontalPositionVariance;
    }

    public float getCompassVariance() {
        return compassVariance;
    }

    public void setCompassVariance(float compassVariance) {
        this.compassVariance = compassVariance;
    }

    public boolean isEkfFlagSet(EkfFlags flag) {
        return flags.get(flag.ordinal());
    }

    /**
     * Returns true if the horizontal absolute position is ok, and home position is set.
     *
     * @param armed
     * @return
     */
    public boolean isPositionOk(boolean armed) {
        if (armed) {
            return this.flags.get(EkfFlags.EKF_POS_HORIZ_ABS.ordinal())
                    && !this.flags.get(EkfFlags.EKF_CONST_POS_MODE.ordinal());
        } else {
            return this.flags.get(EkfFlags.EKF_POS_HORIZ_ABS.ordinal())
                    || this.flags.get(EkfFlags.EKF_PRED_POS_HORIZ_ABS.ordinal());
        }
    }

    public enum EkfFlags {
        EKF_ATTITUDE(EKF_STATUS_FLAGS.EKF_ATTITUDE),
        EKF_VELOCITY_HORIZ(EKF_STATUS_FLAGS.EKF_VELOCITY_HORIZ),
        EKF_VELOCITY_VERT(EKF_STATUS_FLAGS.EKF_VELOCITY_VERT),
        EKF_POS_HORIZ_REL(EKF_STATUS_FLAGS.EKF_POS_HORIZ_REL),
        EKF_POS_HORIZ_ABS(EKF_STATUS_FLAGS.EKF_POS_HORIZ_ABS),
        EKF_POS_VERT_ABS(EKF_STATUS_FLAGS.EKF_POS_VERT_ABS),
        EKF_POS_VERT_AGL(EKF_STATUS_FLAGS.EKF_POS_VERT_AGL),
        EKF_CONST_POS_MODE(EKF_STATUS_FLAGS.EKF_CONST_POS_MODE),
        EKF_PRED_POS_HORIZ_REL(EKF_STATUS_FLAGS.EKF_PRED_POS_HORIZ_REL),
        EKF_PRED_POS_HORIZ_ABS(EKF_STATUS_FLAGS.EKF_PRED_POS_HORIZ_ABS);

        final int value;

        EkfFlags(int value) {
            this.value = value;
        }
    }
}
