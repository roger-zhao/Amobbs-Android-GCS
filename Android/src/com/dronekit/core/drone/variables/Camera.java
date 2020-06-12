package com.dronekit.core.drone.variables;

import com.MAVLink.ardupilotmega.msg_camera_feedback;
import com.MAVLink.ardupilotmega.msg_mount_status;
import com.dronekit.core.drone.DroneVariable;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.property.Altitude;
import com.dronekit.core.drone.property.Attitude;
import com.dronekit.core.drone.property.Gps;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.survey.CameraInfo;
import com.dronekit.core.survey.FootPrint;
import com.evenbus.AttributeEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class Camera extends DroneVariable {

    private CameraInfo camera = new CameraInfo();
    private List<FootPrint> footprints = new ArrayList<FootPrint>();
    private double gimbal_pitch;

    public Camera(Drone myDrone) {
        super(myDrone);
    }

    public Camera(Drone myDrone, List<CameraInfo> availableCameraInfos) {
        super(myDrone);
    }

    public void newImageLocation(msg_camera_feedback msg) {
        footprints.add(new FootPrint(camera, msg));
        EventBus.getDefault().post(AttributeEvent.CAMERA_FOOTPRINTS_UPDATED);
    }

    public List<FootPrint> getFootprints() {
        return footprints;
    }

    public FootPrint getLastFootprint() {
        return footprints.get(footprints.size() - 1);
    }

    public CameraInfo getCamera() {
        return camera;
    }

    public FootPrint getCurrentFieldOfView() {
        final Altitude droneAltitude = myDrone.getAltitude();
        double altitude = droneAltitude.getAltitude();

        final Gps droneGps = myDrone.getVehicleGps();
        LatLong position = droneGps.getPosition();
        //double pitch = myDrone.getOrientation().getPitch() - gimbal_pitch;

        final Attitude attitude = myDrone.getAttitude();
        double pitch = attitude.getPitch();
        double roll = attitude.getRoll();
        double yaw = attitude.getYaw();
        return new FootPrint(camera, position, altitude, pitch, roll, yaw);
    }

    public void updateMountOrientation(msg_mount_status msg_mount_status) {
        gimbal_pitch = 90 - msg_mount_status.pointing_a / 100;
    }
}
