package org.farring.gcs.dialogs.openfile;

import com.dronekit.core.drone.property.Parameter;

import org.farring.gcs.utils.file.IO.ParameterReader;

import java.util.List;

public abstract class OpenParameterDialog extends OpenFileDialog {
    public abstract void parameterFileLoaded(List<Parameter> parameters);

    @Override
    protected FileReader createReader() {
        return new ParameterReader();
    }

    @Override
    protected void onDataLoaded(FileReader reader) {
        parameterFileLoaded(((ParameterReader) reader).getParameters());
    }
}