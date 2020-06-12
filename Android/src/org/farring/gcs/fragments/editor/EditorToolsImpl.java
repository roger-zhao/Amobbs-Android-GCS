package org.farring.gcs.fragments.editor;

import android.os.Bundle;

import com.dronekit.core.helpers.coordinates.LatLong;

import org.farring.gcs.proxy.mission.MissionProxy;
import org.farring.gcs.proxy.mission.MissionSelection;
import org.farring.gcs.proxy.mission.item.MissionItemProxy;

import java.util.List;

/**
 * Created by Fredia Huya-Kouadio on 8/25/15.
 */
public abstract class EditorToolsImpl implements MissionSelection.OnSelectionUpdateListener {

    protected final EditorToolsFragment editorToolsFragment;
    protected MissionProxy missionProxy;

    EditorToolsImpl(EditorToolsFragment fragment) {
        this.editorToolsFragment = fragment;
    }

    void setMissionProxy(MissionProxy missionProxy) {
        this.missionProxy = missionProxy;
    }

    void onSaveInstanceState(Bundle outState) {
    }

    void onRestoreInstanceState(Bundle savedState) {
    }

    public void onMapClick(LatLong point) {
        if (missionProxy == null) return;

        // If an mission item is selected, unselect it.
        missionProxy.selection.clearSelection();
    }

    public void onListItemClick(MissionItemProxy item) {
        if (missionProxy == null)
            return;

        if (missionProxy.selection.selectionContains(item)) {
            missionProxy.selection.clearSelection();
        } else {
            editorToolsFragment.setTool(EditorToolsFragment.EditorTools.NONE);
            missionProxy.selection.setSelectionTo(item);
        }
    }

    public void onPathFinished(List<LatLong> path) {
    }

    @Override
    public void onSelectionUpdate(List<MissionItemProxy> selected) {

    }

    public abstract EditorToolsFragment.EditorTools getEditorTools();

    public abstract void setup();
}
