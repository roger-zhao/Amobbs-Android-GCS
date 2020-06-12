package org.farring.gcs.graphic.map;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import com.dronekit.core.MAVLink.command.doCmd.MavLinkDoCmds;
import com.dronekit.core.drone.autopilot.Drone;
import com.dronekit.core.drone.commandListener.ICommandListener;
import com.dronekit.core.drone.property.Home;
import com.dronekit.core.helpers.coordinates.LatLong;
import com.dronekit.core.helpers.coordinates.LatLongAlt;
import com.orhanobut.logger.Logger;

import org.farring.gcs.R;
import org.farring.gcs.maps.MarkerInfo;

public class GraphicHome extends MarkerInfo.SimpleMarkerInfo {

    private final Drone drone;
    private final Context context;

    public GraphicHome(Drone drone, Context context) {
        this.drone = drone;
        this.context = context;
    }

    @Override
    public float getAnchorU() {
        return 0.5f;
    }

    public boolean isValid() {
        Home droneHome = drone.getVehicleHome();
        return droneHome != null && droneHome.isValid();
    }

    @Override
    public float getAnchorV() {
        return 0.5f;
    }

    @Override
    public Bitmap getIcon(Resources res) {
        return BitmapFactory.decodeResource(res, R.drawable.ic_wp_home);
    }

    @Override
    public LatLong getPosition() {
        Home droneHome = drone.getVehicleHome();
        if (droneHome == null) return null;

        return droneHome.getCoordinate();
    }

    @Override
    public void setPosition(LatLong position) {
        //Move the home location
        final Home currentHome = drone.getVehicleHome();
        final LatLongAlt homeCoord = currentHome.getCoordinate();
        final double homeAlt = homeCoord == null ? 0 : homeCoord.getAltitude();

        final LatLongAlt newHome = new LatLongAlt(position, homeAlt);
        MavLinkDoCmds.setVehicleHome(drone, newHome, new ICommandListener() {
            @Override
            public void onSuccess() {
                Logger.i("Updated home location to %s", newHome);
                Toast.makeText(context, "Updated home location.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int i) {
                Logger.e("Unable to update home location: %d", i);
            }

            @Override
            public void onTimeout() {
                Logger.w("Home location update timed out.");
            }
        });
    }

    @Override
    public String getSnippet() {
        Home droneHome = drone.getVehicleHome();
        return "Home " + (droneHome == null ? "N/A" : droneHome.getCoordinate().getAltitude());
    }

    @Override
    public String getTitle() {
        return "Home";
    }

    @Override
    public boolean isVisible() {
        return isValid();
    }

    @Override
    public boolean isDraggable() {
        return true;
    }
}
