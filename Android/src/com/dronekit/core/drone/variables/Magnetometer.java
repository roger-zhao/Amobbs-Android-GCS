package com.dronekit.core.drone.variables;

import com.MAVLink.common.msg_raw_imu;
import com.MAVLink.common.msg_scaled_imu2;
import com.dronekit.core.drone.DroneVariable;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.property.Parameter;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.EventBus;

public class Magnetometer extends DroneVariable {

    private int mag1_x;
    private int mag1_y;
    private int mag1_z;

    private int mag2_x;
    private int mag2_y;
    private int mag2_z;

    public Magnetometer(Drone myDrone) {
        super(myDrone);
    }

    public void newMag1Data(msg_raw_imu msg_imu1) {
        mag1_x = msg_imu1.xmag;
        mag1_y = msg_imu1.ymag;
        mag1_z = msg_imu1.zmag;

        EventBus.getDefault().post(AttributeEvent.UPDATE_MAGNETOMETER_NO1);
    }

    public void newMag2Data(msg_scaled_imu2 msg_imu2) {
        mag2_x = msg_imu2.xmag;
        mag2_y = msg_imu2.ymag;
        mag2_z = msg_imu2.zmag;

        EventBus.getDefault().post(AttributeEvent.UPDATE_MAGNETOMETER_NO2);
    }

    // 磁罗盘1的偏差数据
    public int[] getCompass1Offsets() {
        Parameter paramX = myDrone.getParameterManager().getParameter("COMPASS_OFS_X");
        Parameter paramY = myDrone.getParameterManager().getParameter("COMPASS_OFS_Y");
        Parameter paramZ = myDrone.getParameterManager().getParameter("COMPASS_OFS_Z");
        if (paramX == null || paramY == null || paramZ == null) {
            return null;
        }
        return new int[]{(int) paramX.getValue(), (int) paramY.getValue(), (int) paramZ.getValue()};
    }

    // 磁罗盘2的偏差数据
    public int[] getCompass2Offsets() {
        Parameter paramX = myDrone.getParameterManager().getParameter("COMPASS_OFS2_X");
        Parameter paramY = myDrone.getParameterManager().getParameter("COMPASS_OFS2_Y");
        Parameter paramZ = myDrone.getParameterManager().getParameter("COMPASS_OFS2_Z");
        if (paramX == null || paramY == null || paramZ == null) {
            return null;
        }
        return new int[]{(int) paramX.getValue(), (int) paramY.getValue(), (int) paramZ.getValue()};
    }

    // 发送偏差数据到【1号】磁罗盘
    public void sendMag1Offsets(double x, double y, double z) throws Exception {
        Parameter mag1OffsetX = myDrone.getParameterManager().getParameter("COMPASS_OFS_X");
        Parameter mag1OffsetY = myDrone.getParameterManager().getParameter("COMPASS_OFS_Y");
        Parameter mag1OffsetZ = myDrone.getParameterManager().getParameter("COMPASS_OFS_Z");

        if (mag1OffsetX == null || mag1OffsetY == null || mag1OffsetZ == null) {
            throw new Exception("参数列表仍未加载完成~！");
        }

        mag1OffsetX.setValue(x);
        mag1OffsetY.setValue(y);
        mag1OffsetZ.setValue(z);

        myDrone.getParameterManager().sendParameter(mag1OffsetX); //TODO should probably do a check after sending the parameters
        myDrone.getParameterManager().sendParameter(mag1OffsetY);
        myDrone.getParameterManager().sendParameter(mag1OffsetZ);
    }

    // 发送偏差数据到【2号】磁罗盘
    public void sendMag2Offsets(double x, double y, double z) throws Exception {
        Parameter mag2OffsetX = myDrone.getParameterManager().getParameter("COMPASS_OFS2_X");
        Parameter mag2OffsetY = myDrone.getParameterManager().getParameter("COMPASS_OFS2_Y");
        Parameter mag2OffsetZ = myDrone.getParameterManager().getParameter("COMPASS_OFS2_Z");

        if (mag2OffsetX == null || mag2OffsetY == null || mag2OffsetZ == null) {
            throw new Exception("参数列表仍未加载完成~！");
        }

        mag2OffsetX.setValue(x);
        mag2OffsetY.setValue(y);
        mag2OffsetZ.setValue(z);

        myDrone.getParameterManager().sendParameter(mag2OffsetX); //TODO should probably do a check after sending the parameters
        myDrone.getParameterManager().sendParameter(mag2OffsetY);
        myDrone.getParameterManager().sendParameter(mag2OffsetZ);
    }

    public int getMag1_x() {
        return mag1_x;
    }

    public int getMag1_y() {
        return mag1_y;
    }

    public int getMag1_z() {
        return mag1_z;
    }

    public int getMag2_x() {
        return mag2_x;
    }

    public int getMag2_y() {
        return mag2_y;
    }

    public int getMag2_z() {
        return mag2_z;
    }
}
