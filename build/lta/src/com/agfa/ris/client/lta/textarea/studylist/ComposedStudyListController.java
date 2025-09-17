package com.agfa.ris.client.lta.textarea.studylist;

import com.agfa.hap.base.datadriven.XObservable;
import com.agfa.hap.base.datadriven.XObserver;
import com.agfa.hap.base.types.Timestamp;
import com.agfa.hap.cyclelist.CycleListController;
import com.agfa.hap.cyclelist.data.CycleListData;
import com.agfa.hap.cyclelist.data.DisplayStrategy;
import com.agfa.hap.cyclelist.ui.generictable.CycleListGenericTableModel;
import com.agfa.hap.dataloading.Results;
import com.agfa.hap.dataloading.StateFullResults;
import com.agfa.hap.dataloading.Status;
import com.agfa.hap.dataloading.UpdateCoordinatorDesktopProvider;
import com.agfa.hap.dataloading.coordinator.CoordinatorTaskCallBack;
import com.agfa.hap.dataloading.item.AbstractLoadableItem;
import com.agfa.hap.dataloading.messaging.EdtTransaction;
import com.agfa.hap.dataloading.messaging.TransactionContext;
import com.agfa.hap.docks.Dockable;
import com.agfa.hap.docks.impl.tabbed.TabbedDock;
import com.agfa.hap.ext.Assert;
import com.agfa.hap.ext.mvp.AppContext;
import com.agfa.hap.ext.mvp.DefaultController;
import com.agfa.hap.ext.mvp.IController;
import com.agfa.hap.ext.mvp.event.GlobalBus;
import com.agfa.hap.ext.mvp.event.Subscriber;
import com.agfa.hap.ext.tableconfig.ConfigPropertyResult;
import com.agfa.hap.sdo.DataObject;
import com.agfa.hap.sdo.implementation.AbstractDataObject;
import com.agfa.hap.workspace.ui.HapOptionPane;
import com.agfa.pacs.client.context.ClinicalContextProviderFactory;
import com.agfa.pacs.data.export.tce.TeachingFile;
import com.agfa.pacs.data.export.tce.TeachingFileProperty;
import com.agfa.pacs.data.shared.lw.IPatientInfo;
import com.agfa.pacs.data.shared.lw.IStudyInfo;
import com.agfa.pacs.data.shared.lw.StudyAvailabilityState;
import com.agfa.pacs.notifier.IListener;
import com.agfa.ris.client.domainmodel.person.Patient;
import com.agfa.ris.client.domainmodel.ris.RequestedProcedure;
import com.agfa.ris.client.domainmodel.ris.SearchLocation;
import com.agfa.ris.client.domainmodel.ris.ServiceRequest;
import com.agfa.ris.client.lta.LtaMetaDataAdapterFactory;
import com.agfa.ris.client.lta.LtaTeachingFileItemMetaDataDecorator;
import com.agfa.ris.client.lta.reportseverity.IReportSeverityEditableObserver;
import com.agfa.ris.client.lta.reportseverity.IReportSeverityObservable;
import com.agfa.ris.client.lta.reportseverity.IReportSeverityObserver;
import com.agfa.ris.client.lta.textarea.actions.ITextAreaActionProvider;
import com.agfa.ris.client.lta.textarea.actions.TextAreaActionProviderFactory;
import com.agfa.ris.client.lta.textarea.controller.DiagnosticOverviewController;
import com.agfa.ris.client.lta.textarea.cycling.ExternalRequestedProcedureLoader;
import com.agfa.ris.client.lta.textarea.dataloading.CurrentLoadedItemModel;
import com.agfa.ris.client.lta.textarea.dataloading.RunOnEDTCoordinatorTaskCallBack;
import com.agfa.ris.client.lta.textarea.dataloading.handler.AcquisitionSplitMergeHandler;
import com.agfa.ris.client.lta.textarea.dataloading.handler.ReportingSplitMergeHandler;
import com.agfa.ris.client.lta.textarea.dataloading.handler.SplitMergeHandlerWrapper;
import com.agfa.ris.client.lta.textarea.dataloading.handler.SplitMergeNotPossibleHandler;
import com.agfa.ris.client.lta.textarea.dataloading.handler.factory.DataHandlers;
import com.agfa.ris.client.lta.textarea.docks.SelectedTabChangedEvent;
import com.agfa.ris.client.lta.textarea.docks.TextAreaDockSystem;
import com.agfa.ris.client.lta.textarea.docks.dockables.TextAreaDock;
import com.agfa.ris.client.lta.textarea.event.ActiveStudiesUpdatedEvent;
import com.agfa.ris.client.lta.textarea.event.Add2ComparisonStudiesEvent;
import com.agfa.ris.client.lta.textarea.event.Add2TeachingFilesEvent;
import com.agfa.ris.client.lta.textarea.event.RemoveFromListEvent;
import com.agfa.ris.client.lta.textarea.event.RemovedStudyEvent;
import com.agfa.ris.client.lta.textarea.event.StudySelectionEvent;
import com.agfa.ris.client.lta.textarea.event.BlendAddedStudiesEvent;
import com.agfa.ris.client.lta.textarea.event.UpdateStudyObjectListEvent;
import com.agfa.ris.client.lta.textarea.overview.SelectedStudyModel;
import com.agfa.ris.client.lta.textarea.reporting.ReportingContext;
import com.agfa.ris.client.lta.textarea.study.StudyDisplayUpdater;
import com.agfa.ris.client.lta.textarea.study.StudyModule;
import com.agfa.ris.client.lta.textarea.studylist.AbstractStudyListController;
import com.agfa.ris.client.lta.textarea.studylist.AbstractStudyListTablePopupMenu;
import com.agfa.ris.client.lta.textarea.studylist.ComposedStudyListModel;
import com.agfa.ris.client.lta.textarea.studylist.ImageAreaGateway;
import com.agfa.ris.client.lta.textarea.studylist.Messages;
import com.agfa.ris.client.lta.textarea.studylist.ShowReportForStudyEvent;
import com.agfa.ris.client.lta.textarea.studylist.StudyListActionListener;
import com.agfa.ris.client.lta.textarea.studylist.StudyListData;
import com.agfa.ris.client.lta.textarea.studylist.StudyListObject;
import com.agfa.ris.client.lta.textarea.studylist.active.ActiveStudyListController;
import com.agfa.ris.client.lta.textarea.studylist.comparison.ComparisonAddedStudyListController;
import com.agfa.ris.client.lta.textarea.studylist.comparison.ComparisonStudyListController;
import com.agfa.ris.client.lta.textarea.studylist.comparison.ComparisonTeachingFileListController;
import com.agfa.ris.client.lta.textarea.studylist.comparison.SearchLocationConfigureContext;
import com.agfa.ris.client.lta.textarea.studylist.renderer.StudyInViewHighlighter;
import com.agfa.ris.client.platform.desktop.DesktopController;
import com.agfa.ris.client.platform.error.HapOptionPaneWrapper;
import com.agfa.ris.client.workflow.model.task.TaskInstance;
import com.agfa.ris.client.workflow.services.SingleTaskInstanceCallback;
import com.agfa.ris.common.features.Feature;
import com.agfa.ris.common.features.FeatureRouter;
import com.agfa.ris.pacs.PacsImageDisplayUtils;
import com.agfa.ris.pacs.query.DicomToSdoMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IAdaptable;

public class ComposedStudyListController
extends DefaultController<IController>
implements IReportSeverityEditableObserver {
    private static final String LOCAL = "local";
    private static final Logger LOGGER = Logger.getLogger(ComposedStudyListController.class);
    private static final String IMAGEAREA_SUFFIX = "_ImageArea";
    private static final GlobalBus globalEventBus = AppContext.getCurrentContext().getGlobalEventBus();
    private final Comparator<StudyListObject> sortor = Comparator.comparing(StudyListObject::getOrderAccessionNumber).thenComparing(StudyListObject::getStudyUID);
    private final ComposedStudyListModel model;
    private SplitMergeHandlerWrapper splitMergeHandler;
    private PropertyChangeListener isSplitMergePossibleListener;
    private final List<ActiveStudyListController> actives;
    private final List<ComparisonStudyListController> comparisons;
    private final List<ComparisonAddedStudyListController> comparisonAddedList;
    private final List<ComparisonTeachingFileListController> tceList;
    private final List<RequestedProcedure> additionalComparisons = new ArrayList<RequestedProcedure>();
    private final Set<RequestedProcedure> addedComparisonStudies = Sets.newHashSet();
    private final Set<RequestedProcedure> teachingFileStudies = Sets.newHashSet();
    private final SelectedStudyModel selectedStudyModel;
    private final List<StudyListActionListener> studyListActionListeners = new ArrayList<StudyListActionListener>();
    private final List<StudyListObject> moveToActiveStudyList = new ArrayList<StudyListObject>();
    private IReportSeverityObservable reportSeverityObservable;
    private boolean additionalComparisonsLoaded = false;
    private DisplayStrategy displayStrategy;
    private ImageAreaGateway gateway;
    private PopupMenuListener popupListener;
    private StudyInViewHighlighter studyHighlighter;

    public ComposedStudyListController(SelectedStudyModel selectedStudyModel) {
        this.selectedStudyModel = selectedStudyModel;
        this.model = new ComposedStudyListModel();
        this.actives = new ArrayList<ActiveStudyListController>();
        this.comparisons = new ArrayList<ComparisonStudyListController>();
        this.comparisonAddedList = Lists.newArrayList();
        this.tceList = Lists.newArrayList();
        this.init(selectedStudyModel);
    }

    void init(SelectedStudyModel selectedStudyModel) {
        globalEventBus.registerController(this);
        selectedStudyModel.selectedStudy().addObserver(new XObserver(){

            protected void update(XObservable observable, Object oldValue, int event) {
                RequestedProcedure study = ComposedStudyListController.this.selectedStudyModel.getSelectedStudy();
                ComposedStudyListController.this.updateSelectedStudyInTables(study, ComposedStudyListController.this.selectedStudyModel.getStudyModule());
                ComposedStudyListController.this.repaint();
            }
        });
        this.isSplitMergePossibleListener = evt -> {
            SplitMergeHandlerWrapper sm = (SplitMergeHandlerWrapper)evt.getSource();
            for (AbstractStudyListController c : this.getChildren()) {
                for (StudyListObject studyObject : c.getModel()) {
                    studyObject.setSplitMergePossible((Boolean)evt.getNewValue());
                    studyObject.setCanMerge(sm.canMerge(studyObject.getRequestedProcedure()));
                    studyObject.setCanShow(sm.canShow(studyObject.getRequestedProcedure()));
                    studyObject.setCanSplit(sm.canSplit(studyObject.getRequestedProcedure()));
                }
            }
            this.refresh();
        };
        this.studyHighlighter = new StudyInViewHighlighter(selectedStudyModel);
        this.addActive(0);
        this.getActiveStudiesController().addActivePropertyChangeListener(evt -> {
            StudyListObject obj = (StudyListObject)evt.getSource();
            if (!obj.isActive() && !obj.isLinked()) {
                this.removeActiveStudy(obj, null);
                this.updateCycleList();
            } else if (obj.isActive() && obj.isLinked()) {
                this.addActiveStudy(obj, true, null);
                this.updateCycleList();
            }
        });
        this.addComparison(0);
        this.getComparisonStudiesController().addActivePropertyChangeListener(evt -> {
            StudyListObject obj = (StudyListObject)evt.getSource();
            if (obj.isActive()) {
                this.addActiveStudy(obj, false, null);
                this.updateCycleList();
            }
        });
        this.addComparisonAdded(1);
        this.addTeachingFile(2);
        PropertyChangeListener repaintListener = evt -> this.repaint();
        selectedStudyModel.addPropertyChangeListener("associatedStudies", repaintListener);
        selectedStudyModel.addPropertyChangeListener("studyForContextMenu", repaintListener);
    }

    protected void repaint() {
        for (AbstractStudyListController c : this.getChildren()) {
            c.getView().getScrollpaneStudies().repaint();
        }
    }

    protected void refresh() {
        for (AbstractStudyListController c : this.getChildren()) {
            c.refresh();
        }
    }

    protected List<AbstractStudyListController> getChildren() {
        ArrayList<AbstractStudyListController> result = new ArrayList<AbstractStudyListController>();
        result.addAll(this.actives);
        result.addAll(this.comparisons);
        result.addAll(this.comparisonAddedList);
        result.addAll(this.tceList);
        return result;
    }

    private ImageAreaGateway getGateway() {
        if (this.gateway == null) {
            this.gateway = new ImageAreaGateway();
        }
        return this.gateway;
    }

    private PopupMenuListener getPopupListener() {
        if (this.popupListener == null) {
            this.popupListener = new PopupMenuListener(){

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                    ComposedStudyListController.this.selectedStudyModel.setStudyForContextMenu(null);
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    ComposedStudyListController.this.selectedStudyModel.setStudyForContextMenu(null);
                }

                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    AbstractStudyListTablePopupMenu popupMenu = (AbstractStudyListTablePopupMenu)e.getSource();
                    ComposedStudyListController.this.selectedStudyModel.setStudyForContextMenu(popupMenu.getStudyListObject().getRequestedProcedure());
                    ComposedStudyListController.this.repaint();
                }
            };
        }
        return this.popupListener;
    }

    private StudyInViewHighlighter getHighlighter() {
        return this.studyHighlighter;
    }

    private void enableTableConfigSyncing(AbstractStudyListController c) {
        c.getView().getTblStudies().addPropertyChangeListener("tableconfigOpen", evt -> {
            Object newValue = evt.getNewValue();
            if (Boolean.FALSE.equals(newValue) || newValue instanceof ConfigPropertyResult && !((ConfigPropertyResult)newValue).getValue()) {
                this.syncTableConfig(c);
            }
        });
    }

    private void syncTableConfig(AbstractStudyListController c) {
        boolean isActive = c instanceof ActiveStudyListController;
        ArrayList<AbstractStudyListController> others = new ArrayList<AbstractStudyListController>();
        if (isActive) {
            for (ActiveStudyListController activeStudyListController : this.actives) {
                if (this.isNotAnImageAreaStudyListController(activeStudyListController) || activeStudyListController.equals(c)) continue;
                others.add(activeStudyListController);
            }
        } else {
            ArrayList<AbstractStudyListController> allComparisons = Lists.newArrayList();
            allComparisons.addAll(this.comparisons);
            allComparisons.addAll(this.comparisonAddedList);
            allComparisons.addAll(this.tceList);
            for (AbstractStudyListController comparison : allComparisons) {
                if (this.isNotAnImageAreaStudyListController(comparison) || comparison.equals(c)) continue;
                others.add(comparison);
            }
        }
        LOGGER.info("Applying tableconfig to " + others.size() + " other image area(s).");
        for (AbstractStudyListController abstractStudyListController : others) {
            abstractStudyListController.reloadTableConfig(abstractStudyListController.getIdentifier());
        }
    }

    private boolean isNotAnImageAreaStudyListController(AbstractStudyListController c) {
        return !c.getIdentifier().endsWith(IMAGEAREA_SUFFIX);
    }

    public Set<RequestedProcedure> getAddedComparisonStudies() {
        return this.addedComparisonStudies;
    }

    public Set<RequestedProcedure> getTeachingFileStudies() {
        return this.teachingFileStudies;
    }

    public void setPrimaryPatient(Patient patient) {
        for (ComparisonAddedStudyListController cc : this.comparisonAddedList) {
            cc.setPrimaryPatient(patient);
        }
    }

    public void clearSelection(StudyModule currentStudyModule) {
        if (currentStudyModule != StudyModule.Active) {
            for (ActiveStudyListController activeStudyListController : this.actives) {
                activeStudyListController.getView().getTblStudies().clearSelection();
            }
        }
        if (currentStudyModule != StudyModule.Comparison) {
            for (ComparisonStudyListController comparisonStudyListController : this.comparisons) {
                comparisonStudyListController.getView().getTblStudies().clearSelection();
            }
        }
        if (currentStudyModule != StudyModule.Added) {
            for (ComparisonAddedStudyListController comparisonAddedStudyListController : this.comparisonAddedList) {
                comparisonAddedStudyListController.getView().getTblStudies().clearSelection();
            }
        }
        if (currentStudyModule != StudyModule.Tce) {
            for (ComparisonTeachingFileListController comparisonTeachingFileListController : this.tceList) {
                comparisonTeachingFileListController.getView().getTblStudies().clearSelection();
            }
        }
    }

    private void extConfigStudyListController(AbstractStudyListController controller, StudyModule studyModule) {
        controller.initModel();
        controller.setPopupMenuListener(this.getPopupListener());
        controller.getView().getTblStudies().getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || controller.isCheckBoxSelected()) {
                return;
            }
            int row = controller.getView().getTblStudies().getSelectedRow();
            if (row == -1) {
                return;
            }
            Rectangle cellRect = controller.getView().getTblStudies().getCellRect(row, 0, false);
            controller.getView().getTblStudies().scrollRectToVisible(cellRect);
            this.setCheckboxesEnabled(false);
            row = controller.getView().getTblStudies().convertRowIndexToModel(row);
            controller.setSelectionUnderway(true);
            this.selectedStudyModel.setStudyModule(studyModule);
            this.selectedStudyModel.setSelectedStudy(((StudyListObject)controller.getModel().get(row)).getRequestedProcedure());
            controller.setSelectionUnderway(false);
            this.clearSelection(studyModule);
            this.setCheckboxesEnabled(true);
        });
        controller.getView().getTblStudies().addHighlighter(this.getHighlighter());
        if (controller.isForImageArea()) {
            this.enableTableConfigSyncing(controller);
        }
    }

    public ActiveStudyListController addActive(int ordinal) {
        boolean isForImageArea;
        Assert.isTrue(ordinal >= 0, "ordinal must be >= 0");
        Object identifier = "ActiveStudiesListTable";
        boolean bl = isForImageArea = ordinal > 0;
        if (isForImageArea) {
            identifier = (String)identifier + IMAGEAREA_SUFFIX;
        }
        ActiveStudyListController c = new ActiveStudyListController((String)identifier, this.selectedStudyModel, this.getGateway(), isForImageArea);
        this.extConfigStudyListController(c, StudyModule.Active);
        this.actives.add(c);
        if (this.reportSeverityObservable != null) {
            this.reportSeverityObservable.add(c);
        }
        return c;
    }

    public ComparisonAddedStudyListController getComparisonAddedStudyListController() {
        return this.comparisonAddedList.get(0);
    }

    public ComparisonTeachingFileListController getTeachingFileListController() {
        return this.tceList.get(0);
    }

    public ComparisonStudyListController addComparison(int ordinal) {
        boolean isForImageArea;
        Assert.isTrue(ordinal >= 0, "ordinal must be >= 0");
        Object identifier = "ComparisonStudiesListTable";
        boolean bl = isForImageArea = ordinal > 0;
        if (isForImageArea) {
            identifier = (String)identifier + IMAGEAREA_SUFFIX;
        }
        ComparisonStudyListController c = new ComparisonStudyListController((String)identifier, this.selectedStudyModel, this.getGateway(), isForImageArea);
        this.extConfigStudyListController(c, StudyModule.Comparison);
        this.comparisons.add(c);
        return c;
    }

    public ComparisonAddedStudyListController addComparisonAdded(int ordinal) {
        boolean isForImageArea;
        Assert.isTrue(ordinal >= 1, "ordinal must be >= 1");
        Object identifier = "AddedComparisonStudiesListTable";
        boolean bl = isForImageArea = ordinal > 1;
        if (isForImageArea) {
            identifier = (String)identifier + IMAGEAREA_SUFFIX;
        }
        ComparisonAddedStudyListController c = new ComparisonAddedStudyListController((String)identifier, this.getGateway(), isForImageArea);
        this.extConfigStudyListController(c, StudyModule.Added);
        this.comparisonAddedList.add(c);
        return c;
    }

    public ComparisonTeachingFileListController addTeachingFile(int ordinal) {
        boolean isForImageArea;
        Assert.isTrue(ordinal >= 2, "ordinal must be >= 2");
        Object identifier = "TeachingFileListTable";
        boolean bl = isForImageArea = ordinal > 2;
        if (isForImageArea) {
            identifier = (String)identifier + IMAGEAREA_SUFFIX;
        }
        ComparisonTeachingFileListController c = new ComparisonTeachingFileListController((String)identifier, this.getGateway(), isForImageArea);
        this.extConfigStudyListController(c, StudyModule.Tce);
        this.tceList.add(c);
        return c;
    }

    public ComposedStudyListModel getModel() {
        return this.model;
    }

    public AbstractStudyListController getActiveStudiesController() {
        return this.actives.get(0);
    }

    public ComparisonStudyListController getComparisonStudiesController() {
        return this.comparisons.get(0);
    }

    @Override
    public void clear() {
        for (AbstractStudyListController c : this.getChildren()) {
            c.clear();
        }
        this.addedComparisonStudies.clear();
        this.teachingFileStudies.clear();
        this.selectedStudyModel.setStudyModule(StudyModule.Active);
        this.model.clear();
        this.additionalComparisons.clear();
        if (this.splitMergeHandler != null) {
            this.splitMergeHandler.removePropertyChangeListener("isSplitMergePossible", this.isSplitMergePossibleListener);
        }
    }

    public void display(IAdaptable domainModel) {
        StudyListData studyListData = (StudyListData)domainModel.getAdapter(StudyListData.class);
        List<RequestedProcedure> activeStudies = studyListData.getActiveStudies();
        List<RequestedProcedure> comparisonStudies = this.filterByProcedureStatus(studyListData.getRelevantStudies());
        this.additionalComparisons.removeIf(s -> this.containsStudy((Collection<RequestedProcedure>)activeStudies, (RequestedProcedure)s));
        SplitMergeHandlerWrapper handlerWrapper = studyListData.getSplitMergeHandler();
        boolean isTask = studyListData.isTask();
        if (!this.additionalComparisonsLoaded) {
            comparisonStudies.addAll(this.filterByProcedureStatus(this.additionalComparisons));
            this.setDockableComparisonsLoaded(false);
            this.comparisons.forEach(ComparisonStudyListController::triggerTabTitleUpdate);
        } else {
            comparisonStudies.addAll(this.additionalComparisons);
            this.setDockableComparisonsLoaded(true);
            this.comparisons.forEach(ComparisonStudyListController::triggerTabTitleUpdate);
        }
        this.setComparisonObservers();
        this.model.fillModel(activeStudies, comparisonStudies);
        this.model.setLocalStudies(studyListData.isLocal());
        ReportingContext.setAllPatientProcedures(studyListData.getAllStudies());
        this.performUpdates(activeStudies, comparisonStudies, handlerWrapper, isTask);
        this.selectedComparisonDockable(StudyModule.Comparison);
        for (ComparisonAddedStudyListController c : this.comparisonAddedList) {
            c.setFirstTrigger(ComparisonAddedStudyListController.trigger.NOT_TRIGGERED_YET);
        }
        AppContext.getCurrentContext().getGlobalEventBus().sendEvent(new ActiveStudiesUpdatedEvent());
    }

    private void setComparisonObservers() {
        for (final RequestedProcedure cmp : this.getModel().getComparisonStudies()) {
            XObserver displayStatusObserver = new XObserver(){

                protected void update(XObservable observable, Object oldValue, int event) {
                    for (StudyListObject obj : ComposedStudyListController.this.getComparisonStudiesController().getModel()) {
                        if (!obj.getRequestedProcedure().isSameObject(cmp)) continue;
                        obj.getRequestedProcedure().setDisplayStatus(cmp.getDisplayStatus());
                    }
                }
            };
            cmp.displayStatus().addObserver(displayStatusObserver);
        }
    }

    private boolean containsStudy(Collection<RequestedProcedure> collection, RequestedProcedure study) {
        return collection.stream().map(RequestedProcedure::getPrimaryKey).anyMatch(pk -> pk.equals(study.getPrimaryKey()));
    }

    public void mergeComparisons(List<RequestedProcedure> comparisons, boolean isTask) {
        this.setAdditionalComparisonsLoaded(true);
        List<RequestedProcedure> comparisonsToMerge = this.filterByProcedureStatus(comparisons);
        this.setDockableComparisonsLoaded(true);
        this.comparisons.forEach(ComparisonStudyListController::triggerTabTitleUpdate);
        if (this.model.getActiveStudies().isEmpty()) {
            this.additionalComparisons.addAll(comparisonsToMerge.stream().filter(comp -> this.additionalComparisons.stream().noneMatch(existingComp -> existingComp.getPrimaryKey().equals(comp.getPrimaryKey()))).collect(Collectors.toList()));
            return;
        }
        this.additionalComparisons.clear();
        List comparisonsToRetain = this.model.getComparisonStudies().stream().filter(existingComparison -> comparisonsToMerge.stream().noneMatch(toMerge -> toMerge.getPrimaryKey().equals(existingComparison.getPrimaryKey()))).collect(Collectors.toList());
        List comparisonsToRetire = this.model.getComparisonStudies().stream().filter(existingComparison -> comparisonsToMerge.stream().anyMatch(toMerge -> toMerge.getPrimaryKey().equals(existingComparison.getPrimaryKey()))).collect(Collectors.toList());
        ArrayList<RequestedProcedure> newComparisons = new ArrayList<RequestedProcedure>(comparisonsToMerge);
        newComparisons.addAll(comparisonsToRetain);
        this.model.refillModel(new ArrayList<RequestedProcedure>(this.model.getActiveStudies()), newComparisons);
        Stream<RequestedProcedure> currentStudiesStream = Stream.concat(this.model.getActiveStudies().stream(), this.model.getComparisonStudies().stream());
        List<RequestedProcedure> allStudies = (List<RequestedProcedure>) Stream.concat(currentStudiesStream, comparisonsToRetire.stream()).distinct().collect(Collectors.toList());
        ReportingContext.setAllPatientProcedures(allStudies);
        this.performUpdates(this.model.getActiveStudies(), new HashSet<RequestedProcedure>(this.model.getActiveStudies()), new HashSet<RequestedProcedure>(comparisonsToRetire), this.model.getComparisonStudies(), this.splitMergeHandler, isTask);
        this.selectedComparisonDockable(StudyModule.Comparison);
    }

    private void performUpdates(List<RequestedProcedure> activeStudies, List<RequestedProcedure> comparisonStudies, SplitMergeHandlerWrapper handlerWrapper, boolean isTask) {
        this.performUpdates(activeStudies, Collections.emptySet(), Collections.emptySet(), comparisonStudies, handlerWrapper, isTask);
    }

    private void performUpdates(List<RequestedProcedure> activeStudies, Set<RequestedProcedure> oldActives, Set<RequestedProcedure> nonRelevantComparisons, List<RequestedProcedure> comparisonStudies, SplitMergeHandlerWrapper handlerWrapper, boolean isTask) {
        this.updateActiveList(activeStudies, nonRelevantComparisons, comparisonStudies, handlerWrapper, isTask, oldActives);
        this.updateComparisonList(activeStudies, comparisonStudies, handlerWrapper, isTask);
        this.setCheckboxesEnabled(true);
        this.updateSelectedStudyInTables(this.selectedStudyModel.getSelectedStudy(), this.selectedStudyModel.getStudyModule());
        this.updateDisplayStatus();
    }

    private void setDockableComparisonsLoaded(boolean value) {
        this.comparisons.forEach(cmp -> cmp.setDockableComparisonsLoaded(value));
    }

    public void setSplitMergeHandler(SplitMergeHandlerWrapper splitMergeHandler) {
        if (this.splitMergeHandler != null) {
            this.splitMergeHandler.getPropertyChangeListeners("isSplitMergePossible");
            this.splitMergeHandler.removePropertyChangeListener("isSplitMergePossible", this.isSplitMergePossibleListener);
        }
        this.splitMergeHandler = splitMergeHandler;
        splitMergeHandler.addPropertyChangeListener("isSplitMergePossible", this.isSplitMergePossibleListener);
    }

    public void setReadOnly() {
        if (this.splitMergeHandler != null) {
            this.splitMergeHandler.setSplitMergeHandler(new SplitMergeNotPossibleHandler());
        }
    }

    public void setCheckboxesEnabled(boolean enabled) {
        for (AbstractStudyListController c : this.getChildren()) {
            c.setCheckboxEnabled(enabled);
        }
    }

    private void updateActiveList(List<RequestedProcedure> actives, List<RequestedProcedure> comparisons, SplitMergeHandlerWrapper splitMergeHandler, Set<RequestedProcedure> previousActives) {
        this.updateActiveList(actives, Collections.emptySet(), comparisons, splitMergeHandler, true, previousActives);
    }

    private void updateActiveList(List<RequestedProcedure> actives, Set<RequestedProcedure> nonRelevantComparisons, List<RequestedProcedure> comparisons, SplitMergeHandlerWrapper splitMergeHandler, boolean isTask, Set<RequestedProcedure> previousActives) {
        Object newActives;
        ArrayList<StudyListObject> activeStudies = new ArrayList<StudyListObject>();
        int nbActive = actives.size();
        for (RequestedProcedure activeRequestedProcedure : actives) {
            StudyListObject activeStudy = this.createActiveStudy(activeRequestedProcedure, splitMergeHandler, nbActive > 1, isTask);
            this.getActiveStudiesController().getModel().stream().filter(object -> object.getRequestedProcedure().isSameObject(activeRequestedProcedure)).findAny().ifPresent(object -> activeStudy.setManualClicked(object.isManualClicked()));
            activeStudy.setSplitMergePossible(splitMergeHandler.isSplitMergePossible());
            this.notifyAddToStudyListeners(activeRequestedProcedure);
            activeStudies.add(activeStudy);
        }
        List<RequestedProcedure> newActivesList = actives.stream().filter(rp -> !previousActives.contains(rp) && !nonRelevantComparisons.contains(rp)).collect(Collectors.toList());
        newActives = newActivesList;
        if (!(!this.getActiveStudiesController().getImageAreaGateway().isImageViewerAvailable() || UpdateCoordinatorDesktopProvider.getInstance().hasRequests(CycleListController.SHOW) || DisplayStrategy.OpenWithTextOnly.equals(this.displayStrategy) && !this.atLeastOneIsDisplayedInIA(actives) || newActivesList.isEmpty())) {
            this.getActiveStudiesController().getImageAreaGateway().addStudiesToSidebar((Collection<RequestedProcedure>)newActives, true, this.model.areLocalStudies());
        }
        for (RequestedProcedure comparisonRequestedProcedure : comparisons) {
            Set<ServiceRequest> activeServiceRequests = this.getActiveServiceRequests(actives);
            if (!AbstractDataObject.contains(new ArrayList<ServiceRequest>(activeServiceRequests), comparisonRequestedProcedure.getServiceRequest())) continue;
            StudyListObject obj = this.createComparisonStudy(comparisonRequestedProcedure, splitMergeHandler, isTask);
            this.getActiveStudiesController().getModel().stream().filter(object -> object.getRequestedProcedure().isSameObject(comparisonRequestedProcedure)).findAny().ifPresent(object -> obj.setManualClicked(object.isManualClicked()));
            obj.setLinked(true);
            obj.setSplitMergePossible(splitMergeHandler.isSplitMergePossible());
            activeStudies.add(obj);
        }
        activeStudies.sort(this.sortor);
        for (ActiveStudyListController activeStudyListController : this.actives) {
            if (splitMergeHandler.getWrappedHandler() instanceof ReportingSplitMergeHandler) {
                activeStudyListController.getSelectedStudyModel().setMergeType(SelectedStudyModel.MergeType.REPORTING);
            } else if (splitMergeHandler.getWrappedHandler() instanceof AcquisitionSplitMergeHandler) {
                activeStudyListController.getSelectedStudyModel().setMergeType(SelectedStudyModel.MergeType.ACQUISITION);
            }
            activeStudyListController.display(activeStudies);
            this.reportSeverityObservable.add(activeStudyListController);
        }
        this.reportSeverityObservable.notifyReportSeverityUpdated();
        Iterator<RequestedProcedure> activesRequestedProcedureIterator = actives.iterator();
        if (activesRequestedProcedureIterator.hasNext()) {
            RequestedProcedure activeRequestedProcedure;
            activeRequestedProcedure = activesRequestedProcedureIterator.next();
            Patient patient = activeRequestedProcedure.getPatient();
            this.setPrimaryPatient(patient);
        }
    }

    private boolean atLeastOneIsDisplayedInIA(List<RequestedProcedure> studies) {
        return studies.stream().anyMatch(rp -> PacsImageDisplayUtils.getStatusByStudyUID(rp.getStudyUID()) != null);
    }

    private void updateComparisonList(Collection<RequestedProcedure> actives, Collection<RequestedProcedure> comparisons, SplitMergeHandlerWrapper splitMergeHandler, boolean isTask) {
        ArrayList<StudyListObject> studies = new ArrayList<StudyListObject>();
        for (RequestedProcedure study : comparisons) {
            Set<ServiceRequest> activeServiceRequests = this.getActiveServiceRequests(actives);
            if (AbstractDataObject.contains(new ArrayList<ServiceRequest>(activeServiceRequests), study.getServiceRequest())) continue;
            StudyListObject obj = this.createComparisonStudy(study, splitMergeHandler, isTask);
            this.moveToActiveStudyList.stream().filter(object -> object.getRequestedProcedure().isSameObject(study)).findAny().ifPresent(object -> obj.setManualClicked(object.isManualClicked()));
            studies.add(obj);
        }
        studies.sort(this.sortor);
        for (ComparisonStudyListController c : this.comparisons) {
            if (splitMergeHandler.getWrappedHandler() instanceof ReportingSplitMergeHandler) {
                c.getSelectedStudyModel().setMergeType(SelectedStudyModel.MergeType.REPORTING);
            } else if (splitMergeHandler.getWrappedHandler() instanceof AcquisitionSplitMergeHandler) {
                c.getSelectedStudyModel().setMergeType(SelectedStudyModel.MergeType.ACQUISITION);
            }
            c.display(studies);
        }
    }

    private List<RequestedProcedure> filterByProcedureStatus(List<RequestedProcedure> list) {
        List<RequestedProcedure> associatedStudies;
        ArrayList<RequestedProcedure> current = new ArrayList<RequestedProcedure>();
        RequestedProcedure selectedStudy = this.getSelectedStudyModel().getSelectedStudy();
        if (selectedStudy != null) {
            current.add(selectedStudy);
        }
        if ((associatedStudies = this.getSelectedStudyModel().getAssociatedStudies()) != null) {
            current.addAll(associatedStudies);
        }
        return list.stream().filter(study -> {
            boolean excluded = !AbstractDataObject.contains(current, study) && study.isCancelled() && !study.hasReports() && !study.hasImages();
            return !excluded;
        }).collect(Collectors.toList());
    }

    private List<StudyListObject> getStudyListObjects(Collection<RequestedProcedure> requestedProcedures, boolean isLocal) {
        ArrayList<StudyListObject> objects = new ArrayList<StudyListObject>(requestedProcedures.size());
        for (RequestedProcedure requestedProcedure : requestedProcedures) {
            StudyListObject object = this.createStudyListObject(requestedProcedure, isLocal, StudyModule.Added);
            objects.add(object);
        }
        return objects;
    }

    private boolean isNewAddedComparisonListEnabled() {
        return FeatureRouter.INSTANCE.isEnabled(Feature.NEW_STUDY_LIST) && FeatureRouter.INSTANCE.isEnabled(Feature.NEW_ADDED_COMPARISON_STUDY_LIST);
    }

    @Subscriber(value={Add2ComparisonStudiesEvent.class})
    public void updateComparisonAddedList(Add2ComparisonStudiesEvent event) {
        String selectedStudyUID;
        if (this.isNewAddedComparisonListEnabled()) {
            return;
        }
        final boolean isLocal = event.isLocal();
        final List sortList = event.getSelectedStudies().stream().filter(rp -> this.canBeAdded((RequestedProcedure)rp, isLocal)).sorted(ComposedStudyListController.getRequestedProcedureComparator()).collect(Collectors.toList());
        if (sortList.isEmpty()) {
            if (event.isNeedCompareImages()) {
                event.addResult(false);
                this.showWarnDialog();
            }
            return;
        }
        if (ComparisonAddedStudyListController.trigger.TRIGGERING == this.getComparisonAddedStudyListController().getTriggerState()) {
            selectedStudyUID = "";
            this.getComparisonAddedStudyListController().setFirstTrigger(ComparisonAddedStudyListController.trigger.TRIGGERED);
        } else {
            selectedStudyUID = sortList.iterator().hasNext() ? ((RequestedProcedure)sortList.iterator().next()).getStudyUID() : "";
        }
        boolean isExternal = ((RequestedProcedure)sortList.iterator().next()).isExternal();
        ArrayList<CoordinatorTaskCallBack> callbacks = new ArrayList<CoordinatorTaskCallBack>();
        if (event.isNeedCompareImages()) {
            if (isExternal) {
                callbacks.add(new RunOnEDTCoordinatorTaskCallBack(){

                    @Override
                    public void onCoordinatorTaskEndedExecuteOnEDT(AbstractLoadableItem item, Status status) {
                        Results result = (Results)item.getData("IA_PACS_PatientStudy");
                        ExternalRequestedProcedureLoader.updateRequestedProcedure((List)result.getValue(), sortList);
                        Map<String, RequestedProcedure> toBeProcessed = (Map<String, RequestedProcedure>) sortList.stream().collect(Collectors.toMap(RequestedProcedure::getStudyUID, Function.identity()));
                        List<IPatientInfo> patients = (List<IPatientInfo>)result.getValue();
                        if (patients != null && !patients.isEmpty()) {
                            block0: for (IPatientInfo patientInfo : patients) {
                                if (patientInfo.isDisposed()) continue;
                                IStudyInfo studyInfo = patientInfo.getStudy();
                                for (RequestedProcedure study : (List<RequestedProcedure>) sortList) {
                                    if (!study.getStudyUID().equals(studyInfo.getStudyUID()) || ComposedStudyListController.LOCAL.equals(study.getAeCode())) continue;
                                    toBeProcessed.remove(study.getStudyUID());
                                    class StudyAvailableListener
                                    implements IListener<StudyAvailabilityState> {
                                        private RequestedProcedure requestedProcedure;
                                        private final IStudyInfo studyInfo;
                                        final /* synthetic */ String val$selectedStudyUID;
                                        final /* synthetic */ boolean val$isLocal;

                                        public StudyAvailableListener(RequestedProcedure requestedProcedure, IStudyInfo studyInfo, String selectedStudyUID, boolean isLocal) {
                                            this.val$selectedStudyUID = selectedStudyUID;
                                            this.val$isLocal = isLocal;
                                            this.requestedProcedure = requestedProcedure;
                                            this.studyInfo = studyInfo;
                                        }

                                        public void notifyListener(StudyAvailabilityState state) {
                                            SwingUtilities.invokeLater(() -> {
                                                SearchLocation searchLocation = this.requestedProcedure.getSearchLocation();
                                                this.requestedProcedure = new DicomToSdoMapper().createRequestedProcedure(this.studyInfo, true);
                                                this.requestedProcedure.setSearchLocation(searchLocation);
                                                this.requestedProcedure.setLocation(searchLocation.getLocationName());
                                                ComposedStudyListController.this.addAddedComparison(this.requestedProcedure, this.val$selectedStudyUID, this.val$isLocal);
                                            });
                                            ClinicalContextProviderFactory.getProvider().deregisterStudyAvailableListener((IStudyInfo)this.requestedProcedure.getAttributes(), this);
                                        }
                                    }
                                    StudyAvailableListener studyAvailabilityListener = new StudyAvailableListener(study, studyInfo, study.getStudyUID(), false);
                                    ClinicalContextProviderFactory.getProvider().registerStudyAvailableListener(studyInfo, studyAvailabilityListener);
                                    continue block0;
                                }
                            }
                        }
                        if (!toBeProcessed.isEmpty()) {
                            for (RequestedProcedure requestedProcedure : toBeProcessed.values()) {
                                ComposedStudyListController.this.addAddedComparison(requestedProcedure, selectedStudyUID, isLocal);
                            }
                        }
                    }
                });
            } else {
                callbacks.add(new RunOnEDTCoordinatorTaskCallBack(){

                    @Override
                    protected void onCoordinatorTaskEndedExecuteOnEDT(AbstractLoadableItem item, Status status) {
                        for (RequestedProcedure requestedProcedure : (List<RequestedProcedure>) sortList) {
                            ComposedStudyListController.this.addAddedComparison(requestedProcedure, selectedStudyUID, isLocal);
                        }
                    }
                });
            }
            this.getGateway().compareStudies(new ArrayList<RequestedProcedure>(sortList), this.model.areLocalStudies(), false, null, callbacks);
        } else {
            for (RequestedProcedure requestedProcedure : (List<RequestedProcedure>) sortList) {
                this.addAddedComparison(requestedProcedure, selectedStudyUID, isLocal);
            }
        }
        LOGGER.info("updateComparisonAddedList - selectedStudy by study UID[" + selectedStudyUID + "]");
    }

    static Comparator<RequestedProcedure> getRequestedProcedureComparator() {
        return Comparator.comparing(RequestedProcedure::getDateTime, Comparator.nullsFirst(Collections.reverseOrder()));
    }

    private void addAddedComparison(RequestedProcedure requestedProcedure, String selectedStudyUID, boolean isLocal) {
        if (LOCAL.equals(requestedProcedure.getAeCode())) {
            for (RequestedProcedure comparison : this.model.getComparisonStudies()) {
                if (!comparison.getStudyUID().equals(requestedProcedure.getStudyUID())) continue;
                requestedProcedure = (RequestedProcedure)comparison.deepCopy();
                requestedProcedure.setAeTitle(LOCAL);
                break;
            }
        }
        this.addedComparisonStudies.add(requestedProcedure);
        StudyDisplayUpdater.getInstance().addStudy(requestedProcedure);
        for (ComparisonAddedStudyListController c : this.comparisonAddedList) {
            c.display(this.getStudyListObjects(this.addedComparisonStudies, isLocal));
        }
        if (StringUtils.isNotEmpty(selectedStudyUID) && CurrentLoadedItemModel.getInstance().getLoadedItem() != null) {
            this.selectedStudy(selectedStudyUID, StudyModule.Added, this.comparisonAddedList);
        }

        // NEW: also blend Added studies into the main Comparison list WITHOUT filtering
        // This keeps Added visible for users who open it, but no click is needed to see items in Comparison.
        // Use direct addition to additionalComparisons to avoid filterByProcedureStatus() that excludes external studies
        if (!this.containsStudy(this.additionalComparisons, requestedProcedure) &&
            !this.containsStudy(this.model.getComparisonStudies(), requestedProcedure)) {
            this.additionalComparisons.add(requestedProcedure);

            // Update the main comparison model to show the blended study
            List<RequestedProcedure> newComparisons = new ArrayList<>(this.model.getComparisonStudies());
            newComparisons.add(requestedProcedure);
            this.model.refillModel(new ArrayList<>(this.model.getActiveStudies()), newComparisons);

            // Update ReportingContext with all studies
            Stream<RequestedProcedure> currentStudiesStream = Stream.concat(this.model.getActiveStudies().stream(), this.model.getComparisonStudies().stream());
            List<RequestedProcedure> allStudies = currentStudiesStream.collect(Collectors.toList());
            ReportingContext.setAllPatientProcedures(allStudies);
        }
    }

    private void showWarnDialog() {
        HapOptionPaneWrapper instance = new HapOptionPaneWrapper(null);
        ArrayList<HapOptionPaneWrapper.HapOptionPaneAction> actions = new ArrayList<HapOptionPaneWrapper.HapOptionPaneAction>();
        actions.add(instance.closeAction);
        instance.getModel().setActions((List)actions);
        instance.getModel().setMessageType(HapOptionPane.MessageType.WARNING_MESSAGE);
        instance.getModel().setTitle(Messages.ComposedStudyListController_3);
        instance.getModel().setMainInstruction(Messages.ComposedStudyListController_4);
        instance.popup();
    }

    private boolean hasImages(RequestedProcedure rp) {
        if (rp.isExternal()) {
            return rp.getPacsStudyDetails() != null && rp.getPacsStudyDetails().getNumberOfInstances() > 0L;
        }
        return rp.getPacsStudyDetails() != null && rp.getPacsStudyDetails().getNumberOfImages() > 0L;
    }

    private boolean canBeAdded(RequestedProcedure rp, boolean isLocal) {
        boolean includeDownloadedStudies = SearchLocationConfigureContext.getInstance().getIncludeDownloadedStudies();
        if (includeDownloadedStudies && !isLocal) {
            return !this.isSameWithActiveStudy(rp) && !this.isSameComparisonAddedStudy(rp);
        }
        return !this.isSameWithActiveStudy(rp) && !this.isSameComparisonStudy(rp);
    }

    private boolean isSameWithActiveStudy(RequestedProcedure addedStudy) {
        for (RequestedProcedure activeStudy : this.model.getActiveStudies()) {
            if (!activeStudy.getStudyUID().equals(addedStudy.getStudyUID())) continue;
            return true;
        }
        return false;
    }

    private boolean isSameComparisonStudy(RequestedProcedure addedStudy) {
        for (RequestedProcedure comparison : this.model.getComparisonStudies()) {
            if (!comparison.getStudyUID().equals(addedStudy.getStudyUID())) continue;
            if (this.hasImages(comparison)) {
                return true;
            }
            if (this.hasImages(comparison) || this.hasImages(addedStudy)) continue;
            return true;
        }
        return this.isSameComparisonAddedStudy(addedStudy);
    }

    private boolean isSameComparisonAddedStudy(RequestedProcedure addedStudy) {
        for (RequestedProcedure comparison : this.addedComparisonStudies) {
            if (!comparison.getStudyUID().equals(addedStudy.getStudyUID())) continue;
            return true;
        }
        return false;
    }

    @Subscriber(value={Add2TeachingFilesEvent.class})
    public void updateTeachingFileList(Add2TeachingFilesEvent event) {
        String selectedStudyUID;
        if (this.isNewAddedComparisonListEnabled()) {
            return;
        }
        final ArrayList<RequestedProcedure> sortList = Lists.newArrayList(event.getSelectedStudies());
        sortList.sort(new CreationDateComparator());
        String string = selectedStudyUID = sortList.iterator().hasNext() ? ((RequestedProcedure)sortList.iterator().next()).getStudyUID() : "";
        if (event.isCompareNeeded()) {
            ArrayList<CoordinatorTaskCallBack> callbacks = new ArrayList<CoordinatorTaskCallBack>();
            callbacks.add(new RunOnEDTCoordinatorTaskCallBack(){

                @Override
                public void onCoordinatorTaskEndedExecuteOnEDT(AbstractLoadableItem item, Status status) {
                    item.getMetaData().createMetaDataAdapter(new LtaMetaDataAdapterFactory<Void>(){

                        @Override
                        public Void createForTeachingFileLoadableItem(LtaTeachingFileItemMetaDataDecorator metaData) {
                            block0: for (Map.Entry<TeachingFile<TeachingFile.ObjectReference>, IPatientInfo> next : metaData.getTeachingFiles().entrySet()) {
                                for (RequestedProcedure study : (List<RequestedProcedure>) sortList) {
                                    if (!next.getKey().getClientProperty(TeachingFile.ClientProperty.ATFI_STUID).equals(study.getStudyUID()) || next.getValue() == null) continue;
                                    ExternalRequestedProcedureLoader.updateRequestedProcedure(next.getValue(), study);
                                    study.getPacsStudyDetails().setNumberOfInstances((Long)next.getKey().getClientProperty(TeachingFile.ClientProperty.NUMBER_OF_IMAGES));
                                    study.setTeachingFile(next.getKey());
                                    continue block0;
                                }
                            }
                            ComposedStudyListController.this.addAddedTeachingFile(sortList, selectedStudyUID);
                            return null;
                        }
                    });
                }
            });
            this.getGateway().compareStudies(new ArrayList<RequestedProcedure>(sortList), this.model.areLocalStudies(), false, null, callbacks);
        } else {
            this.addAddedTeachingFile(sortList, selectedStudyUID);
        }
    }

    /*
     * Exception decompiling
     */
    private void addAddedTeachingFile(List<RequestedProcedure> sortList, String selectedStudyUID) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * java.lang.NullPointerException: Cannot invoke "Object.getClass()" because "unbound" is null
         *     at org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder.doBind(GenericTypeBinder.java:142)
         *     at org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder.extractBindings(GenericTypeBinder.java:135)
         *     at org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType.improveGenericType(InferredJavaType.java:1105)
         *     at org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType.useAsWithoutCasting(InferredJavaType.java:1083)
         *     at org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype.tightenArgs(MethodPrototype.java:584)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs.createStatement(Op02WithProcessedDataAndRefs.java:1199)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs.access$100(Op02WithProcessedDataAndRefs.java:57)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs$11.call(Op02WithProcessedDataAndRefs.java:2080)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs$11.call(Op02WithProcessedDataAndRefs.java:2077)
         *     at org.benf.cfr.reader.util.graph.AbstractGraphVisitorFI.process(AbstractGraphVisitorFI.java:60)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs.convertToOp03List(Op02WithProcessedDataAndRefs.java:2089)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:469)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at software.coley.recaf.services.decompile.cfr.CfrDecompiler.decompileInternal(CfrDecompiler.java:61)
         *     at software.coley.recaf.services.decompile.AbstractJvmDecompiler.decompile(AbstractJvmDecompiler.java:49)
         *     at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
         *     at java.base/java.lang.reflect.Method.invoke(Method.java:565)
         *     at org.jboss.weld.bean.proxy.AbstractBeanInstance.invoke(AbstractBeanInstance.java:39)
         *     at org.jboss.weld.bean.proxy.ProxyMethodHandler.invoke(ProxyMethodHandler.java:109)
         *     at software.coley.recaf.services.decompile.Decompiler$JvmDecompiler$1269202896$Proxy$_$$_WeldClientProxy.decompile(Unknown Source)
         *     at software.coley.recaf.services.decompile.DecompilerManager.lambda$decompile$2(DecompilerManager.java:156)
         *     at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.run(CompletableFuture.java:1814)
         *     at software.coley.recaf.util.threading.ThreadUtil.lambda$wrap$2(ThreadUtil.java:233)
         *     at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1095)
         *     at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:619)
         *     at java.base/java.lang.Thread.run(Thread.java:1447)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    public RequestedProcedure getRequestedProcedureById(Long requestedProcedureId) {
        for (StudyListObject so : this.getActiveStudiesController().getModel()) {
            if (!requestedProcedureId.equals(this.safeGetPrimaryKey(so.getRequestedProcedure()))) continue;
            return so.getRequestedProcedure();
        }
        for (StudyListObject so : this.getComparisonStudiesController().getModel()) {
            if (!requestedProcedureId.equals(this.safeGetPrimaryKey(so.getRequestedProcedure()))) continue;
            return so.getRequestedProcedure();
        }
        for (RequestedProcedure procedure : this.getAddedComparisonStudies()) {
            if (!requestedProcedureId.equals(this.safeGetPrimaryKey(procedure))) continue;
            return procedure;
        }
        for (RequestedProcedure procedure : this.getTeachingFileStudies()) {
            if (!requestedProcedureId.equals(this.safeGetPrimaryKey(procedure))) continue;
            return procedure;
        }
        return null;
    }

    private Long safeGetPrimaryKey(RequestedProcedure requestedProcedure) {
        if (requestedProcedure == null) {
            return null;
        }
        if (requestedProcedure.isExternal()) {
            return null;
        }
        return requestedProcedure.getPrimaryKey();
    }

    public List<RequestedProcedure> getStudies() {
        return Stream.concat(Stream.concat(this.getActiveStudiesController().getStudies().stream(), this.getComparisonStudiesController().getStudies().stream()), Stream.concat(this.getAddedComparisonStudies().stream(), this.getTeachingFileStudies().stream())).collect(Collectors.toList());
    }

    @Subscriber(value={RemoveFromListEvent.class})
    public void removeComparisonAddedFromList(RemoveFromListEvent event) {
        this.getGateway().removeFromSidebar(event.getProcedure(), false);
        if (event.getStudyModule() == StudyModule.Added) {
            this.removeComparisonAddedFromList(event.getProcedure());
        } else if (event.getStudyModule() == StudyModule.Tce) {
            this.removeTeachingFileFromList(event.getProcedure());
        }
    }

    @Subscriber(value={UpdateStudyObjectListEvent.class})
    private void updateStudyInList(UpdateStudyObjectListEvent event) {
        event.getProcedure().ifPresent(this::updateRequestedProcedure);
    }

    private void updateRequestedProcedure(RequestedProcedure requestedProcedure) {
        for (AbstractStudyListController abstractStudyListController : this.comparisonAddedList) {
            ArrayList<StudyListObject> newModel = new ArrayList<StudyListObject>();
            for (StudyListObject obj : abstractStudyListController.getModel()) {
                if (requestedProcedure.getStudyUID().equals(obj.getRequestedProcedure().getStudyUID())) {
                    obj = new StudyListObject(requestedProcedure, obj.isActive(), obj.isLocal());
                    this.getGateway().addStudiesToSidebar(Collections.singletonList(requestedProcedure), obj.isActive(), obj.isLocal());
                }
                newModel.add(obj);
            }
            abstractStudyListController.setModel(newModel);
        }
    }

    private void removeComparisonAddedFromList(RequestedProcedure procedure) {
        String selectedStudyUid = procedure.getStudyUID();
        this.addedComparisonStudies.stream().filter(rp -> rp.getStudyUID().equals(selectedStudyUid)).findAny().ifPresent(this.addedComparisonStudies::remove);
        for (ComparisonAddedStudyListController c : this.comparisonAddedList) {
            ArrayList<StudyListObject> newModel = new ArrayList<StudyListObject>();
            for (StudyListObject obj : c.getModel()) {
                if (selectedStudyUid.equals(obj.getRequestedProcedure().getStudyUID())) continue;
                newModel.add(obj);
            }
            c.setModel(newModel);
        }
    }

    private void removeTeachingFileFromList(RequestedProcedure procedure) {
        String selectedStudyUid = procedure.getStudyUID();
        this.teachingFileStudies.remove(procedure);
        for (ComparisonTeachingFileListController c : this.tceList) {
            ArrayList<StudyListObject> newModel = new ArrayList<StudyListObject>();
            for (StudyListObject obj : c.getModel()) {
                if (selectedStudyUid.equals(obj.getRequestedProcedure().getStudyUID())) continue;
                newModel.add(obj);
            }
            c.setModel(newModel);
        }
    }

    private StudyListObject createStudyListObject(RequestedProcedure study, boolean isLocal, StudyModule module) {
        StudyListObject studyListObject = new StudyListObject(study, false, isLocal);
        studyListObject.setActiveEnabled(false);
        studyListObject.setTask(false);
        if (this.splitMergeHandler != null) {
            studyListObject.setSplitMergePossible(this.splitMergeHandler.isSplitMergePossible());
            studyListObject.setCanMerge(this.splitMergeHandler.canMerge(study));
            studyListObject.setCanShow(this.splitMergeHandler.canShow(study));
            studyListObject.setCanSplit(this.splitMergeHandler.canSplit(study));
        }
        studyListObject.setStudyModule(module);
        return studyListObject;
    }

    private void updateCycleList() {
        if (CycleListController.getInstance().getItemDisplayed().isRoot()) {
            CycleListData data = DataHandlers.getCycleListData(CurrentLoadedItemModel.getInstance().getLoadedItem());
            CycleListController.getInstance().getItemDisplayed().registerData("CycleListData", new StateFullResults(Status.SUCCESS, data));
            CycleListGenericTableModel.getInstance().fireTableDataChanged();
            CycleListController.getInstance().updateCycleListLabel();
        }
    }

    public void removeActiveStudy(final StudyListObject studyListObject, final SingleTaskInstanceCallback additionalCallback) {
        LOGGER.info("Removing Study from report - Study uid=" + studyListObject.getRequestedProcedure().getStudyUID());
        this.startProcessingStudy(studyListObject, true);
        ITextAreaActionProvider textAreaActionProvider = TextAreaActionProviderFactory.getFactory().getTextAreaActionProvider();
        textAreaActionProvider.setEnabled(false);
        TransactionContext ctx = new TransactionContext(CycleListController.MODIFY, true);
        EdtTransaction removeTxn = new EdtTransaction(ctx){

            protected void processOnEdt() {
                block6: {
                    final RequestedProcedure study = studyListObject.getRequestedProcedure();
                    SingleTaskInstanceCallback callback = new SingleTaskInstanceCallback(){

                        @Override
                        public void onException(Throwable e) {
                            try {
                                try {
                                    if (additionalCallback != null) {
                                        additionalCallback.onException(e);
                                    }
                                }
                                catch (Exception e2) {
                                    LOGGER.error("Error removing active study", e2);
                                }
                                LOGGER.error("Error removing active study", e);
                                ComposedStudyListController.this.stopProcessingStudy(study);
                                studyListObject.setActive(true);
                                ComposedStudyListController.this.enableTopToolBar(study);
                            }
                            finally {
                                // Transaction cleanup handled by framework
                            }
                        }

                        /*
                         * WARNING - Removed try catching itself - possible behaviour change.
                         */
                        public void onEnd(TaskInstance targetTask, List<TaskInstance> involvedTasks) {
                            try {
                                try {
                                    if (additionalCallback != null) {
                                        additionalCallback.onEnd(targetTask, involvedTasks);
                                    }
                                }
                                catch (Exception e2) {
                                    LOGGER.error(e2);
                                }
                                ComposedStudyListController.this.enableTopToolBar(study);
                            }
                            finally {
                                // Transaction cleanup handled by framework
                            }
                        }
                    };
                    try {
                        if (ComposedStudyListController.this.splitMergeHandler.splitRequestedProcedure(study, callback)) break block6;
                        try {
                            ComposedStudyListController.this.enableTopToolBar(study);
                            ComposedStudyListController.this.stopProcessingStudy(study);
                            studyListObject.setActive(true);
                            if (additionalCallback != null) {
                                additionalCallback.onException(new IllegalArgumentException("unable to unlink studies"));
                            }
                        }
                        finally {
                            // Transaction cleanup handled by framework
                        }
                    }
                    catch (Exception e) {
                        // Transaction cleanup handled by framework
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        UpdateCoordinatorDesktopProvider.getInstance().update(removeTxn);
        this.moveToActiveStudyList.add(studyListObject);
    }

    public void addActiveStudy(final StudyListObject studyListObject, boolean fromActiveTable, final SingleTaskInstanceCallback additionalCallback) {
        LOGGER.info("Adding Study to report - Study uid=" + studyListObject.getRequestedProcedure().getStudyUID());
        this.startProcessingStudy(studyListObject, fromActiveTable);
        ITextAreaActionProvider textAreaActionProvider = TextAreaActionProviderFactory.getFactory().getTextAreaActionProvider();
        textAreaActionProvider.setEnabled(false);
        TransactionContext ctx = new TransactionContext(CycleListController.MODIFY, true);
        EdtTransaction addTxn = new EdtTransaction(ctx){

            protected void processOnEdt() {
                block6: {
                    final RequestedProcedure study = studyListObject.getRequestedProcedure();
                    SingleTaskInstanceCallback callback = new SingleTaskInstanceCallback(){

                        @Override
                        public void onException(Throwable e) {
                            try {
                                try {
                                    if (additionalCallback != null) {
                                        additionalCallback.onException(e);
                                    }
                                }
                                catch (Exception e2) {
                                    LOGGER.error("Error adding active study", e2);
                                }
                                LOGGER.error("Error adding active study", e);
                                ComposedStudyListController.this.enableTopToolBar(study);
                                ComposedStudyListController.this.stopProcessingStudy(study);
                                studyListObject.setActive(false);
                            }
                            finally {
                                // Transaction cleanup handled by framework
                            }
                        }

                        /*
                         * WARNING - Removed try catching itself - possible behaviour change.
                         */
                        public void onEnd(TaskInstance targetTask, List<TaskInstance> involvedTasks) {
                            try {
                                try {
                                    if (additionalCallback != null) {
                                        additionalCallback.onEnd(targetTask, involvedTasks);
                                    }
                                }
                                catch (Exception e2) {
                                    LOGGER.error(e2);
                                }
                                ComposedStudyListController.this.enableTopToolBar(study);
                                LOGGER.info("ITaskInstanceCallback end");
                                ComposedStudyListController.this.selectedStudyModel.setStudyModule(StudyModule.Active);
                                ComposedStudyListController.this.selectedStudyModel.setSelectedStudy(study);
                                TextAreaDockSystem.getInstance().applyTabHierarchy();
                                this.removeFromAddedList(studyListObject);
                            }
                            finally {
                                // Transaction cleanup handled by framework
                            }
                        }

                        private void removeFromAddedList(StudyListObject studyListObject) {
                            if (studyListObject.isActive()) {
                                ComposedStudyListController.this.removeComparisonAddedFromList(studyListObject.getRequestedProcedure());
                            }
                        }
                    };
                    try {
                        if (ComposedStudyListController.this.splitMergeHandler.mergeRequestedProcedure(study, callback)) break block6;
                        try {
                            LOGGER.info("Merge failed for study : " + study.getStudyUID());
                            ComposedStudyListController.this.enableTopToolBar(study);
                            ComposedStudyListController.this.stopProcessingStudy(study);
                            studyListObject.setActive(false);
                            if (additionalCallback != null) {
                                additionalCallback.onException(new IllegalArgumentException("unable to link studies"));
                            }
                        }
                        finally {
                            // Transaction cleanup handled by framework
                        }
                    }
                    catch (Exception e) {
                        // Transaction cleanup handled by framework
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        UpdateCoordinatorDesktopProvider.getInstance().update(addTxn);
    }

    private void notifyAddToStudyListeners(RequestedProcedure rp) {
        for (StudyListActionListener l : new ArrayList<StudyListActionListener>(this.studyListActionListeners)) {
            l.addStudy(rp.getStudyUID());
        }
    }

    private void notifyRemoveToStudyListeners(RequestedProcedure rp) {
        for (StudyListActionListener l : new ArrayList<StudyListActionListener>(this.studyListActionListeners)) {
            l.removeStudy(rp.getStudyUID());
        }
    }

    private Set<ServiceRequest> getActiveServiceRequests(Collection<RequestedProcedure> activeStudies) {
        HashSet<ServiceRequest> serviceRequests = new HashSet<ServiceRequest>();
        for (RequestedProcedure requestedProcedure : activeStudies) {
            serviceRequests.add(requestedProcedure.getServiceRequest());
        }
        return serviceRequests;
    }

    private StudyListObject createStudyListObject(RequestedProcedure study, SplitMergeHandlerWrapper splitMergeHandler, boolean isActive, boolean isActiveEnabled, boolean isTask) {
        StudyListObject studyListObject = new StudyListObject(study, isActive, this.model.areLocalStudies());
        studyListObject.setActiveEnabled(isActiveEnabled);
        studyListObject.setTask(isTask);
        studyListObject.setSplitMergePossible(splitMergeHandler.isSplitMergePossible());
        studyListObject.setCanMerge(splitMergeHandler.canMerge(study));
        studyListObject.setCanShow(splitMergeHandler.canShow(study));
        studyListObject.setCanSplit(splitMergeHandler.canSplit(study));
        return studyListObject;
    }

    private StudyListObject createActiveStudy(RequestedProcedure study, SplitMergeHandlerWrapper splitMergeHandler, boolean isActiveEnabled, boolean isTask) {
        StudyListObject studyListObject = this.createStudyListObject(study, splitMergeHandler, true, isActiveEnabled, isTask);
        studyListObject.setStudyModule(StudyModule.Active);
        return studyListObject;
    }

    private StudyListObject createComparisonStudy(RequestedProcedure study, SplitMergeHandlerWrapper splitMergeHandler, boolean isTask) {
        StudyListObject studyListObject = this.createStudyListObject(study, splitMergeHandler, false, true, isTask);
        studyListObject.setStudyModule(StudyModule.Comparison);
        return studyListObject;
    }

    private TabbedDock getTabbedDock() {
        return (TabbedDock)TextAreaDock.comparisonStudies.getDock();
    }

    public void selectedComparisonDockable(StudyModule studyModule) {
        if (studyModule == StudyModule.Added) {
            this.selectedDockable("comparison.studies.added");
        } else if (studyModule == StudyModule.Tce) {
            this.selectedDockable("comparison.tce");
        } else {
            AppContext.getCurrentContext().getGlobalEventBus().sendEvent(new SelectedTabChangedEvent());
            this.selectedDockable("comparison.studies");
        }
    }

    private void selectedDockable(String identifier) {
        for (Dockable dockable : this.getTabbedDock().getDockables()) {
            if (!dockable.getKey().getIdentifier().equals(identifier)) continue;
            this.getTabbedDock().setSelectedDockable(dockable);
        }
    }

    public boolean selectStudyByUID(String studyUID, StudyModule studyModule) {
        if (studyUID == null) {
            return false;
        }
        if (studyModule != null) {
            switch (studyModule) {
                case Active: {
                    return this.selectedStudy(studyUID, studyModule, this.actives);
                }
                case Comparison: {
                    return this.selectedStudy(studyUID, studyModule, this.comparisons);
                }
                case Added: {
                    return this.selectedStudy(studyUID, studyModule, this.comparisonAddedList);
                }
                case Tce: {
                    return this.selectedStudy(studyUID, studyModule, this.tceList);
                }
            }
        }
        return this.selectedStudy(studyUID, StudyModule.Active, this.actives) || this.selectedStudy(studyUID, StudyModule.Comparison, this.comparisons) || this.selectedStudy(studyUID, StudyModule.Added, this.comparisonAddedList) || this.selectedStudy(studyUID, StudyModule.Tce, this.tceList);
    }

    private boolean selectedStudy(String studyUID, StudyModule currentModule, List<? extends AbstractStudyListController> controllers) {
        LOGGER.info("selectedStudy for studyUID[" + studyUID + "] on " + currentModule.name());
        for (StudyListObject obj : controllers.get(0).getModel()) {
            if (!studyUID.equals(obj.getRequestedProcedure().getStudyUID())) continue;
            this.clearSelection(currentModule);
            int index = controllers.get(0).getModel().indexOf((Object)obj);
            index = controllers.get(0).getView().getTblStudies().convertRowIndexToView(index);
            int currentSelectedRow = controllers.get(0).getView().getTblStudies().getSelectedRow();
            if (currentSelectedRow == index) {
                if (this.selectedStudyModel.getSelectedStudy() != null && !studyUID.equals(this.selectedStudyModel.getSelectedStudy().getStudyUID())) {
                    this.clearSelection(null);
                } else {
                    return true;
                }
            }
            for (AbstractStudyListController abstractStudyListController : controllers) {
                if (abstractStudyListController.isSelectionUnderway() || (index = abstractStudyListController.getModel().indexOf((Object)obj)) == -1) continue;
                index = abstractStudyListController.getView().getTblStudies().convertRowIndexToView(index);
                Point viewPosition = abstractStudyListController.getView().getScrollpaneStudies().getViewport().getViewPosition();
                abstractStudyListController.getView().getTblStudies().getSelectionModel().setSelectionInterval(index, index);
                abstractStudyListController.getView().getScrollpaneStudies().getViewport().setViewPosition(viewPosition);
            }
            return true;
        }
        return false;
    }

    public void onLinkedRequestedProceduresChanged(List<RequestedProcedure> requestedProcedures, Set<RequestedProcedure> oldRequestedProcedures) {
        ArrayList<RequestedProcedure> oldActives = !oldRequestedProcedures.isEmpty() ? new ArrayList<RequestedProcedure>(oldRequestedProcedures) : new ArrayList<RequestedProcedure>(this.model.getActiveStudies());
        ImmutableSet<RequestedProcedure> previousActives = ImmutableSet.copyOf(oldActives);
        ArrayList<RequestedProcedure> oldComparisons = new ArrayList<RequestedProcedure>(this.model.getComparisonStudies());
        this.model.updateModel(requestedProcedures);
        ComposedStudyListController.removeAll(oldActives, this.model.getActiveStudies());
        ComposedStudyListController.removeAll(oldComparisons, this.model.getComparisonStudies());
        for (RequestedProcedure requestedProcedure : oldActives) {
            this.stopProcessingStudy(requestedProcedure);
            this.notifyRemoveToStudyListeners(requestedProcedure);
        }
        for (RequestedProcedure requestedProcedure : oldComparisons) {
            this.stopProcessingStudy(requestedProcedure);
        }
        this.updateActiveList(this.model.getActiveStudies(), this.model.getComparisonStudies(), this.splitMergeHandler, previousActives);
        this.updateComparisonList(this.model.getActiveStudies(), this.model.getComparisonStudies(), this.splitMergeHandler, true);
    }

    public static <T extends DataObject> void removeAll(List<T> oldList, List<T> newList) {
        ArrayList<DataObject> result = new ArrayList<DataObject>();
        for (DataObject newObject : newList) {
            for (DataObject oldObject : oldList) {
                if (!oldObject.isSameObject(newObject)) continue;
                result.add(oldObject);
            }
        }
        oldList.removeAll(result);
    }

    private void startProcessingStudy(StudyListObject studyListObj, boolean fromActiveTable) {
        if (fromActiveTable) {
            for (ActiveStudyListController c : this.actives) {
                c.setProcessingStudy(studyListObj);
            }
        } else {
            for (ComparisonStudyListController c : this.comparisons) {
                c.setProcessingStudy(studyListObj);
            }
        }
    }

    private void enableTopToolBar(RequestedProcedure study) {
        for (AbstractStudyListController c : this.getChildren()) {
            if (c.isLastProcessing(study)) continue;
            return;
        }
        ITextAreaActionProvider textAreaActionProvider = TextAreaActionProviderFactory.getFactory().getTextAreaActionProvider();
        textAreaActionProvider.setEnabled(true);
    }

    private void stopProcessingStudy(RequestedProcedure study) {
        LOGGER.info("Stop study processing : " + study.getStudyUID() + " - " + study.getIdentity());
        for (AbstractStudyListController c : this.getChildren()) {
            c.stopProcessingStudy(study.getIdentity());
            if (!(c instanceof IReportSeverityObserver)) continue;
            this.reportSeverityObservable.remove((IReportSeverityObserver)((Object)c));
        }
    }

    public SelectedStudyModel getSelectedStudyModel() {
        return this.selectedStudyModel;
    }

    public void updateSelectedStudyInTables(RequestedProcedure study, StudyModule studyModule) {
        if (study == null) {
            for (AbstractStudyListController c : this.getChildren()) {
                if (this.selectedStudyModel.getStudyModule() == StudyModule.Added && c instanceof ComparisonAddedStudyListController || this.selectedStudyModel.getStudyModule() == StudyModule.Tce && c instanceof ComparisonTeachingFileListController) continue;
                c.getView().getTblStudies().getSelectionModel().clearSelection();
            }
        } else {
            this.selectStudyByUID(study.getStudyUID(), studyModule);
        }
    }

    @Subscriber(value={RemovedStudyEvent.class})
    public void removeStudy(RemovedStudyEvent event) {
        this.removeDeletedStudy(event.getProcedure());
    }

    private void removeDeletedStudy(RequestedProcedure requestedProcedure) {
        String studyUid;
        List<RequestedProcedure> comparisonsToRetain = this.model.getComparisonStudies().stream().filter(existingComparison -> !existingComparison.getPrimaryKey().equals(requestedProcedure.getPrimaryKey()) && !existingComparison.getStudyUID().equals(requestedProcedure.getStudyUID())).collect(Collectors.toList());
        if (comparisonsToRetain.size() != this.model.getComparisonStudies().size()) {
            this.model.refillModel(new ArrayList<RequestedProcedure>(this.model.getActiveStudies()), comparisonsToRetain);
        }
        if (!StringUtils.isEmpty(studyUid = requestedProcedure.getStudyUID())) {
            for (AbstractStudyListController c : this.getChildren()) {
                ArrayList<StudyListObject> newModel = new ArrayList<StudyListObject>();
                for (StudyListObject obj : c.getModel()) {
                    if (studyUid.equals(obj.getRequestedProcedure().getStudyUID())) continue;
                    newModel.add(obj);
                }
                c.setModel(newModel);
            }
        }
    }

    public void addStudyListActionListener(StudyListActionListener listener) {
        this.studyListActionListeners.add(listener);
    }

    @Subscriber(value={StudySelectionEvent.class})
    public void setSelectedStudies(StudySelectionEvent event) {
        RequestedProcedure rp;
        String studyUID = event.getStudyUid();
        RequestedProcedure foundRp = null;
        for (RequestedProcedure rp2 : this.getModel().getActiveStudies()) {
            if (!rp2.getStudyUID().equals(studyUID)) continue;
            foundRp = rp2;
            this.getEventBus().sendEvent(new ShowReportForStudyEvent(rp2.getStudyUID()));
            break;
        }
        if (foundRp == null) {
            for (RequestedProcedure rp2 : this.getModel().getComparisonStudies()) {
                if (!rp2.getStudyUID().equals(studyUID)) continue;
                foundRp = rp2;
                this.updateSelectedStudyInTables(foundRp, StudyModule.Comparison);
                break;
            }
        }
        if (foundRp == null) {
            for (StudyListObject studyListObject : this.getComparisonAddedStudyListController().getModel()) {
                rp = studyListObject.getRequestedProcedure();
                if (!rp.getStudyUID().equals(studyUID)) continue;
                foundRp = rp;
                this.updateSelectedStudyInTables(foundRp, StudyModule.Added);
                break;
            }
        }
        if (foundRp == null) {
            for (StudyListObject studyListObject : this.getTeachingFileListController().getModel()) {
                rp = studyListObject.getRequestedProcedure();
                if (!rp.getStudyUID().equals(studyUID)) continue;
                foundRp = rp;
                this.updateSelectedStudyInTables(foundRp, StudyModule.Tce);
                break;
            }
        }
    }

    // NEW: Event subscriber for blending Added studies into main Comparison list
    @Subscriber(value={BlendAddedStudiesEvent.class})
    public void handleBlendAddedStudies(BlendAddedStudiesEvent event) {
        // Blend each study from the Added list into the main comparison list without filtering
        for (RequestedProcedure study : event.getStudies()) {
            if (!this.containsStudy(this.additionalComparisons, study) &&
                !this.containsStudy(this.model.getComparisonStudies(), study)) {

                this.additionalComparisons.add(study);

                // Update the main comparison model to show the blended study
                List<RequestedProcedure> newComparisons = new ArrayList<>(this.model.getComparisonStudies());
                newComparisons.add(study);
                this.model.refillModel(new ArrayList<>(this.model.getActiveStudies()), newComparisons);

                // Update ReportingContext with all studies
                Stream<RequestedProcedure> currentStudiesStream = Stream.concat(this.model.getActiveStudies().stream(), this.model.getComparisonStudies().stream());
                List<RequestedProcedure> allStudies = currentStudiesStream.collect(Collectors.toList());
                ReportingContext.setAllPatientProcedures(allStudies);
            }
        }
    }

    public JComponent getView() {
        return null;
    }

    public void updateDisplayStatus() {
        LOGGER.info("Updating display status of active studies");
        this.updateDisplayStatus(this.model.getActiveStudies());
        LOGGER.info("Updating display status of comparison studies");
        this.updateDisplayStatus(this.model.getComparisonStudies());
        this.updateDisplayStatus(this.addedComparisonStudies);
        this.updateDisplayStatus(this.teachingFileStudies);
    }

    private void updateDisplayStatus(Collection<RequestedProcedure> requestedProcedures) {
        DiagnosticOverviewController diagnosticOverviewController = (DiagnosticOverviewController)((Object)DesktopController.getInstance().getComponent("diagnostic.overview"));
        for (RequestedProcedure requestedProcedure : requestedProcedures) {
            StudyDisplayUpdater.getInstance().updateDisplayStatus(requestedProcedure);
            diagnosticOverviewController.getModel().getActiveStudies().stream().filter(rp -> rp.getStudyUID().equals(requestedProcedure.getStudyUID())).findAny().ifPresent(rp -> requestedProcedure.setDisplayStatus(rp.getDisplayStatus()));
        }
    }

    public void setReportSeverityObservable(IReportSeverityObservable reportSeverityObservable) {
        this.reportSeverityObservable = reportSeverityObservable;
        for (ActiveStudyListController activeStudyListController : this.actives) {
            reportSeverityObservable.add(activeStudyListController);
        }
    }

    public void addReportSeverityObserver(IReportSeverityObserver reportSeverityObserver) {
        for (ActiveStudyListController active : this.actives) {
            active.add(reportSeverityObserver);
        }
    }

    @Override
    public void updateReportSeverityEditable(boolean isEditable) {
        for (ActiveStudyListController active : this.actives) {
            active.updateReportSeverityEditable(isEditable);
        }
    }

    public void calcCanShowForStudyList() {
        if (this.model.getActiveStudies() != null && !this.model.getActiveStudies().isEmpty()) {
            this.getActiveStudiesController().getModel().forEach(obj -> obj.calcCanShow(this.splitMergeHandler));
            this.getComparisonStudiesController().getModel().forEach(obj -> obj.calcCanShow(this.splitMergeHandler));
        }
    }

    public void setAdditionalComparisonsLoaded(boolean value) {
        this.additionalComparisonsLoaded = value;
    }

    public void setDisplayStrategy(DisplayStrategy displayStrategy) {
        this.displayStrategy = displayStrategy;
    }

    public DisplayStrategy getDisplayStrategy() {
        return this.displayStrategy;
    }

    static class CreationDateComparator
    implements Comparator<RequestedProcedure> {
        CreationDateComparator() {
        }

        @Override
        public int compare(RequestedProcedure o1, RequestedProcedure o2) {
            Timestamp date2 = new Timestamp((Date)o2.getTeachingFile().getValue(TeachingFileProperty.CreatedOn));
            Timestamp date1 = new Timestamp((Date)o1.getTeachingFile().getValue(TeachingFileProperty.CreatedOn));
            return date2.compareTo(date1);
        }
    }
}
