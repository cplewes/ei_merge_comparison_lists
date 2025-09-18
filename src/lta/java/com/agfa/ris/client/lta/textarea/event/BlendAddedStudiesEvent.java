package com.agfa.ris.client.lta.textarea.event;

import com.agfa.ris.client.domainmodel.ris.RequestedProcedure;
import com.agfa.hap.ext.mvp.event.IEvent;
import java.util.List;

/**
 * Event to signal that Added studies should be blended into the main Comparison list
 * without filtering, ensuring all studies from external archives are visible.
 */
public class BlendAddedStudiesEvent implements IEvent {
    private final List<RequestedProcedure> studies;
    private final boolean isLocal;

    public BlendAddedStudiesEvent(List<RequestedProcedure> studies, boolean isLocal) {
        this.studies = studies;
        this.isLocal = isLocal;
    }

    public List<RequestedProcedure> getStudies() {
        return studies;
    }

    public boolean isLocal() {
        return isLocal;
    }
}