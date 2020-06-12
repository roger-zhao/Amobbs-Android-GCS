package com.dronekit.api;

import android.text.TextUtils;

import com.dronekit.core.MAVLink.MavLinkCommands;
import com.dronekit.core.MAVLink.command.doCmd.MavLinkDoCmds;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.commandListener.ICommandListener;
import com.dronekit.core.drone.variables.ApmModes;
import com.dronekit.core.drone.variables.Type;
import com.dronekit.core.error.CommandExecutionError;

/**
 * Created by Linjieqiang on 2016/1/27.
 */
public class CommonApiUtils {

    public static void postSuccessEvent(ICommandListener listener) {
        if (listener != null) {
            listener.onSuccess();
        }
    }

    public static void postErrorEvent(int errorCode, ICommandListener listener) {
        if (listener != null) {
            listener.onError(errorCode);
        }
    }

    public static void postTimeoutEvent(ICommandListener listener) {
        if (listener != null) {
            listener.onTimeout();
        }
    }

    /**
     * Check if the kill switch feature is supported on the given drone
     *
     * @param drone
     * @return true if it's supported, false otherwise.
     */
    public static boolean isKillSwitchSupported(Drone drone) {
        if (drone == null)
            return false;

        if (!Type.isCopter(drone.getType().getType()))
            return false;

        String firmwareVersion = drone.getFirmwareVersion();
        if (TextUtils.isEmpty(firmwareVersion))
            return false;

        return !(!firmwareVersion.startsWith("APM:Copter V3.3")
                && !firmwareVersion.startsWith("APM:Copter V3.4")
                && !firmwareVersion.startsWith("Solo"));
    }


    public static void arm(Drone drone, boolean arm, ICommandListener listener) {
        arm(drone, arm, false, listener);
    }

    public static void arm(final Drone drone, final boolean arm, final boolean emergencyDisarm, final ICommandListener listener) {
        if (!arm && emergencyDisarm) {
            if (Type.isCopter(drone.getType().getType()) && !isKillSwitchSupported(drone)) {
                drone.getState().changeFlightMode(ApmModes.ROTOR_STABILIZE, new ICommandListener() {
                    @Override
                    public void onSuccess() {
                        MavLinkCommands.sendArmMessage(drone, arm, emergencyDisarm, listener);
                    }

                    @Override
                    public void onError(int executionError) {
                        if (listener != null) {
                            listener.onError(executionError);
                        }
                    }

                    @Override
                    public void onTimeout() {
                        if (listener != null) {
                            listener.onTimeout();
                        }
                    }
                });

                return;
            }
        }

        MavLinkCommands.sendArmMessage(drone, arm, emergencyDisarm, listener);
    }

    public static void doMotorTest(final Drone drone, final int motor_num, final int throttle, final int duration, final ICommandListener listener) {
         MavLinkCommands.sendMotorTestMessage(drone, motor_num,  throttle, duration,listener);
    }
    public static void startMission(final Drone drone, final boolean forceModeChange, boolean forceArm, final ICommandListener listener) {
        if (drone == null) {
            return;
        }

        final Runnable sendCommandRunnable = new Runnable() {
            @Override
            public void run() {
                MavLinkCommands.startMission(drone, listener);
            }
        };

        final Runnable modeCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (drone.getState().getMode() != ApmModes.ROTOR_AUTO) {
                    if (forceModeChange) {
                        drone.getState().changeFlightMode(ApmModes.ROTOR_AUTO, new ICommandListener() {
                            @Override
                            public void onSuccess() {
                                sendCommandRunnable.run();
                            }

                            @Override
                            public void onError(int executionError) {
                                listener.onError(executionError);
                            }

                            @Override
                            public void onTimeout() {
                                listener.onTimeout();
                            }
                        });
                    } else {
                        listener.onError(CommandExecutionError.COMMAND_FAILED);
                    }
                } else {
                    sendCommandRunnable.run();
                }
            }
        };

        if (!drone.getState().isArmed()) {
            if (forceArm) {
                arm(drone, true, new ICommandListener() {
                    @Override
                    public void onSuccess() {
                        modeCheckRunnable.run();
                    }

                    @Override
                    public void onError(int executionError) {
                        listener.onError(executionError);
                    }

                    @Override
                    public void onTimeout() {
                        listener.onTimeout();
                    }
                });
            } else {
                listener.onError(CommandExecutionError.COMMAND_FAILED);
            }
            return;
        }

        modeCheckRunnable.run();
    }

    public static void gotoWaypoint(Drone drone, int waypoint, ICommandListener listener) {
        if (drone == null)
            return;
        if (waypoint < 0) {
            listener.onError(CommandExecutionError.COMMAND_FAILED);
            return;
        }
        MavLinkDoCmds.gotoWaypoint(drone, waypoint, listener);
    }
}


