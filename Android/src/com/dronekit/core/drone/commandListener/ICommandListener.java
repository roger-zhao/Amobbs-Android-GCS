package com.dronekit.core.drone.commandListener;

/**
 * Created by LinJieqiang on 2016/1/22.
 */
public interface ICommandListener {

    /**
     * Called when the command was executed successfully.
     */
    void onSuccess();

    /**
     * Called when the command execution failed.
     */
    void onError(int executionError);

    /**
     * Called when the command execution times out.
     */
    void onTimeout();
}
