package com.dronekit.core.model;

import com.dronekit.core.drone.autopilot.Drone;

/**
 * Parse received autopilot warning messages.
 */
public interface AutopilotWarningParser {

    String getDefaultWarning();

    String parseWarning(Drone drone, String warning);
}
