package com.dronekit.core.drone;

import com.dronekit.core.drone.profiles.VehicleProfile;
import com.dronekit.core.drone.variables.StreamRates.Rates;
import com.dronekit.core.firmware.FirmwareType;

public interface Preferences {

    VehicleProfile loadVehicleProfile(FirmwareType firmwareType);

    Rates getRates();
}
