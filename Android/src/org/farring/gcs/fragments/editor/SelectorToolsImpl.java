package org.farring.gcs.fragments.editor;

import android.view.View;

import org.farring.gcs.activities.helpers.SuperUI;
import org.farring.gcs.proxy.mission.item.MissionItemProxy;

/**
 * Created by Fredia Huya-Kouadio on 8/25/15.
 */
class SelectorToolsImpl extends EditorToolsImpl implements View.OnClickListener {

    SelectorToolsImpl(EditorToolsFragment fragment) {
        super(fragment);
    }

    @Override
    public void onListItemClick(MissionItemProxy item) {
        if (missionProxy == null)
            return;

        if (missionProxy.selection.selectionContains(item)) {
            missionProxy.selection.removeItemFromSelection(item);
        } else {
            missionProxy.selection.addToSelection(item);
        }
    }

//    private void selectAll() {
//        if (missionProxy == null)
//            return;
//
//        missionProxy.selection.setSelectionTo(missionProxy.getItems());
//        EditorToolsFragment.EditorToolListener listener = editorToolsFragment.listener;
//        if (listener != null)
//            listener.zoomToFitSelected();
//    }

    @Override
    public EditorToolsFragment.EditorTools getEditorTools() {
        return EditorToolsFragment.EditorTools.SELECTOR;
    }

    @Override
    public void setup() {
//        EditorToolsFragment.EditorToolListener listener = editorToolsFragment.listener;
//        if (listener != null) {
//            listener.enableGestureDetection(false);
//            listener.skipMarkerClickEvents(false);
//        }
//
//        Toast.makeText(editorToolsFragment.getContext(), "Click on mission items to select them.", Toast.LENGTH_SHORT).show();
//
//        if (missionProxy != null) {
//            missionProxy.selection.clearSelection();
//            final List<MissionItemProxy> missionItems = missionProxy.getItems();
//            editorToolsFragment.selectAll.setEnabled(!missionItems.isEmpty());
//        }
        ( (SuperUI)editorToolsFragment.getActivity()).UploadMission();
    }

    @Override
    public void onClick(View v) {
//        selectAll();
    }
}
