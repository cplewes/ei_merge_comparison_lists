package com.agfa.ris.client.lta.textarea.docks.dockables;

import com.agfa.hap.docks.Constraint;
import com.agfa.hap.docks.DockSystem;
import com.agfa.hap.docks.Dockable;
import com.agfa.hap.docks.impl.tabbed.TabbedDock;
import com.agfa.hap.ext.mvp.AppContext;
import com.agfa.hap.ext.mvp.event.Subscriber;
import com.agfa.hap.workspace.controller.IWorkspaceController;
import com.agfa.ris.client.domainmodel.person.Patient;
import com.agfa.ris.client.domainmodel.ris.SearchLocation;
import com.agfa.ris.client.lta.textarea.docks.SelectedTabChangedEvent;
import com.agfa.ris.client.lta.textarea.docks.dockables.ComparisonStudiesAddedDockable;
import com.agfa.ris.client.lta.textarea.docks.dockables.ComparisonStudiesDockable;
import com.agfa.ris.client.lta.textarea.docks.dockables.ComparisonTeachingFileListDockable;
import com.agfa.ris.client.lta.textarea.docks.dockables.Messages;
import com.agfa.ris.client.lta.textarea.event.TriggerAutoSearchForAddedComparisonEvent;
import com.agfa.ris.client.lta.textarea.event.TriggerAutoSearchForTceComparisonEvent;
import com.agfa.ris.client.lta.textarea.studylist.comparison.ComparisonAddedStudyListController;
import com.agfa.ris.client.lta.textarea.studylist.comparison.ComparisonStudySearchAreaController;
import com.agfa.ris.client.lta.textarea.studylist.comparison.ComparisonTeachingFileListController;
import com.agfa.ris.client.lta.textarea.studylist.comparison.SearchLocationConfigureContext;
import com.agfa.ris.client.platform.core.JRisPlatform;
import com.agfa.ris.common.features.Feature;
import com.agfa.ris.common.features.FeatureRouter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventObject;
import java.util.Properties;

public class ComparisonStudiesDock
extends TabbedDock
implements MouseListener {
    public static final String identifier = "comparison.studies.dock";
    public static final String ADDED_COMPARISON_ID = "comparison.studies.added";
    public static final String TCE_COMPARISON_ID = "comparison.tce";
    public static final String FIRST_QUERY = "firstQuery";
    private static final long serialVersionUID = -3327113509906962935L;
    private static final IWorkspaceController ws = (IWorkspaceController)JRisPlatform.getPlatform().getService(IWorkspaceController.class);
    private Integer currentSelectedTabIndex = 0;
    private ComparisonStudySearchAreaController controller;

    public ComparisonStudiesDock(int ordinal, DockSystem system) {
        super((String)(ordinal == 0 ? identifier : identifier + ordinal), system);
        this.updateTitle(this.isNewStudyListEnabled());
        AppContext.getCurrentContext().getGlobalEventBus().registerController(this);
        this.addMouseListener(this);
    }

    @Override
    public void addDockable(Dockable dockable, Constraint cons) {
        super.addDockable(dockable, cons);
        if (dockable instanceof ComparisonStudiesDockable || dockable instanceof ComparisonStudiesAddedDockable || dockable instanceof ComparisonTeachingFileListDockable) {
            dockable.getKey().addPropertyChangeListener("title", new PropertyChangeListener(){

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    ComparisonStudiesDock.this.getKey().setTitle(String.valueOf(evt.getNewValue()));
                }
            });
        }
    }

    private static boolean isNewAddedComparisonListEnabled() {
        return FeatureRouter.INSTANCE.isEnabled(Feature.NEW_STUDY_LIST) && FeatureRouter.INSTANCE.isEnabled(Feature.NEW_ADDED_COMPARISON_STUDY_LIST);
    }

    private static boolean isNewTeachingFileComparisonListEnabled() {
        return FeatureRouter.INSTANCE.isEnabled(Feature.NEW_STUDY_LIST) && FeatureRouter.INSTANCE.isEnabled(Feature.NEW_TEACHING_FILE_COMPARISON_STUDY_LIST);
    }

    private void triggerSearch(EventObject e) {
        if (e.getSource() instanceof ComparisonStudiesDock && ((ComparisonStudiesDock)e.getSource()).getSelectedDockable().getKey().getIdentifier().startsWith(ADDED_COMPARISON_ID) && ComparisonStudiesDock.isNewAddedComparisonListEnabled()) {
            AppContext.getCurrentContext().getGlobalEventBus().sendEvent(new TriggerAutoSearchForAddedComparisonEvent());
            return;
        }
        if (e.getSource() instanceof ComparisonStudiesDock && ((ComparisonStudiesDock)e.getSource()).getSelectedDockable().getKey().getIdentifier().startsWith(TCE_COMPARISON_ID) && ComparisonStudiesDock.isNewTeachingFileComparisonListEnabled()) {
            AppContext.getCurrentContext().getGlobalEventBus().sendEvent(new TriggerAutoSearchForTceComparisonEvent());
            return;
        }
        if (e.getSource() instanceof ComparisonStudiesDock) {
            ComparisonTeachingFileListDockable dockable;
            ComparisonTeachingFileListController listController;
            ComparisonStudiesDock dock = (ComparisonStudiesDock)e.getSource();
            if (dock.getSelectedDockable() instanceof ComparisonStudiesAddedDockable) {
                ComparisonStudiesAddedDockable dockable2 = (ComparisonStudiesAddedDockable)dock.getSelectedDockable();
                ComparisonAddedStudyListController listController2 = dockable2.getStudyListController();
                if (listController2 != null && listController2.isFirstTrigger()) {
                    final Patient patient = listController2.getPrimaryPatient();
                    SearchLocation searchLocation = SearchLocationConfigureContext.getInstance().getSearchLocation();
                    if (searchLocation != null) {
                        SearchLocationConfigureContext.getInstance().tryConnectLocation(searchLocation, true, new SearchLocationConfigureContext.SearchLocationConnectedCallback(){

                            @Override
                            public void call() {
                                ComparisonStudiesDock.this.getSearchAreaController().retrieveAllStudiesByPatient(patient, true);
                            }
                        });
                        listController2.setFirstTrigger(ComparisonAddedStudyListController.trigger.TRIGGERING);
                    } else {
                        Properties props = new Properties();
                        props.put(FIRST_QUERY, (Object)listController2.isFirstTrigger());
                        ws.navigate("comparison.study.searcharea", props);
                    }
                }
            } else if (dock.getSelectedDockable() instanceof ComparisonTeachingFileListDockable && (listController = (dockable = (ComparisonTeachingFileListDockable)dock.getSelectedDockable()).getStudyListController()) != null && listController.getModel().size() == 0) {
                ws.navigate("comparison.tce.searcharea");
            }
        }
    }

    private ComparisonStudySearchAreaController getSearchAreaController() {
        if (this.controller == null) {
            this.controller = new ComparisonStudySearchAreaController();
        }
        return this.controller;
    }

    @Subscriber(value={SelectedTabChangedEvent.class})
    public void onSelectedDockableChangedEvent() {
        this.currentSelectedTabIndex = 0;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (this.currentSelectedTabIndex.intValue() != this.getSelectedIndex() && e.getClickCount() == 1) {
            this.currentSelectedTabIndex = this.getSelectedIndex();
            this.triggerSearch(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    boolean isNewStudyListEnabled() {
        return FeatureRouter.INSTANCE.isEnabled(Feature.NEW_STUDY_LIST);
    }

    void updateTitle(boolean isAllStudiesEnabled) {
        this.getKey().setTitle(isAllStudiesEnabled ? Messages.ComparisonStudiesDockableAllStudies_Title : Messages.ComparisonStudiesDockable_Title);
        this.getKey().setName(isAllStudiesEnabled ? Messages.ComparisonStudiesDockableAllStudies_Name : Messages.ComparisonStudiesDockable_Name);
    }
}