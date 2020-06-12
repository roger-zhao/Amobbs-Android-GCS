package com.dronekit.core.drone.commandListener;

/**
 * Basic command listener implementation.
 * Overrides the methods as needed to receive the command execution status notification.
 */
public class SimpleCommandListener implements ICommandListener {
    @Override
    public void onSuccess() {

    }

    @Override
    public void onError(int executionError) {

    }

    @Override
    public void onTimeout() {

    }
}
