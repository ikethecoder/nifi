package org.apache.nifi.atlas.provenance;

import org.apache.atlas.typesystem.Referenceable;
import org.apache.commons.lang.StringUtils;
import org.apache.nifi.atlas.NiFIAtlasHook;
import org.apache.nifi.atlas.NiFiFlow;
import org.apache.nifi.atlas.NiFiFlowPath;
import org.apache.nifi.controller.status.ProcessorStatus;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.provenance.ProvenanceEventRecord;
import org.apache.nifi.provenance.ProvenanceEventType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.nifi.atlas.NiFiTypes.*;
import static org.apache.nifi.atlas.NiFiTypes.ATTR_PROCESSORS;

public class ByFileLineageStrategy implements LineageEventProcessor {

    ComponentLog logger;
    NiFIAtlasHook nifiAtlasHook;

    public ByFileLineageStrategy (ComponentLog logger, NiFIAtlasHook atlasHook) {
        this.logger = logger;
        this.nifiAtlasHook = atlasHook;
    }

    private ComponentLog getLogger() {
        return logger;
    }

    public void processEvent (ProvenanceEventRecord event, NiFiFlow nifiFlow, AnalysisContext analysisContext) {
        try {

            if (event.getEventType() == ProvenanceEventType.CLONE) {

                // final NiFiFlowPath flowPath = nifiFlow.findPath(event.getComponentId());

                String flowPathName = "undefined"; //flowPath.getName()

                if (true) {

                    String qname = event.getFlowFileUuid();
                    String cid = event.getComponentId();
                    ProcessorStatus pr = nifiFlow.getProcessors().get(cid);
                    // Create a new
                    final Referenceable flowPathRef = prepareFlowPathWithTraits (event);

                    flowPathRef.set(ATTR_NAME, (pr == null ? "UNKNOWN" : pr.getName()));
                    flowPathRef.set(ATTR_DESCRIPTION, event.getAttribute("filename") + " : " + flowPathName);
                    flowPathRef.set(ATTR_QUALIFIED_NAME, qname);
                    //flowPathRef.set(ATTR_NIFI_FLOW, flowRef);
                    flowPathRef.set(ATTR_URL, nifiFlow.getUrl());
                    addProcessorToFlowPath (flowPathRef, pr);

                    Collection<Referenceable> outputs = getNifiDataRefs(event);

                    flowPathRef.set(ATTR_OUTPUTS, outputs);

                    nifiAtlasHook.addCreateReferenceable(outputs, flowPathRef);
                }

                if (true) {
                    ProcessorStatus pr = nifiFlow.getProcessors().get(event.getComponentId());

                    Collection<Referenceable> inputs = getNifiDataRefs(event);

                    // Create a new flow path for the Child UUID
                    for ( String childUuid : event.getChildUuids()) {
                        final Referenceable flowPathRef = prepareFlowPathWithTraits (event);
                        flowPathRef.set(ATTR_NAME, (pr == null ? "UNKNOWN" : pr.getName()));
                        flowPathRef.set(ATTR_DESCRIPTION, event.getAttribute("filename") + " : " + flowPathName + " (CLONED)");
                        flowPathRef.set(ATTR_QUALIFIED_NAME, childUuid);
                        //f.set(ATTR_NIFI_FLOW, flowRef);
                        flowPathRef.set(ATTR_URL, nifiFlow.getUrl());
                        addProcessorToFlowPath(flowPathRef, pr);

                        flowPathRef.set(ATTR_INPUTS, inputs);
                        nifiAtlasHook.addCreateReferenceable(inputs, flowPathRef);
                    }

                }

            } else {

                final NiFiProvenanceEventAnalyzer analyzer = NiFiProvenanceEventAnalyzerFactory.getAnalyzer(event.getComponentType(), event.getTransitUri(), event.getEventType());
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Analyzer {} is found for event: {}", new Object[]{analyzer, event});
                }
                if (analyzer == null) {
                    getLogger().warn("No analyzer for {}", new Object[]{event.getComponentType()});
                    return;
                }
                final DataSetRefs refs = analyzer.analyze(analysisContext, event);
                if (refs == null || (refs.isEmpty())) {
                    return;
                }

                // TODO: need special logic for remote ports as it may be connected to multiple flow paths.
                final Set<NiFiFlowPath> flowPaths = refs.getComponentIds().stream()
                        .map(componentId -> {
                            final NiFiFlowPath flowPath = nifiFlow.findPath(componentId);
                            if (flowPath == null) {
                                getLogger().warn("FlowPath for {} was not found.", new Object[]{event.getComponentId()});
                            }
                            return flowPath;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());


                // create reference to NiFi flow path.
                for (NiFiFlowPath flowPath : flowPaths) {
                    // TODO: make the reference to NiFiFlow optional?
                    final Referenceable flowRef = new Referenceable(TYPE_NIFI_FLOW);
                    flowRef.set(ATTR_QUALIFIED_NAME, nifiFlow.getId().getUniqueAttributes().get(ATTR_QUALIFIED_NAME));

                    String name = flowPath.getName();
                    String qname = flowPath.getId();

                    if (true) {
                        //qname = event.getAttribute("NIFI_PROCESS_ID");
                        qname = event.getFlowFileUuid();
                        name = "For DataFlowId " + qname;
                    }


                    String cid = refs.getComponentIds().iterator().next();
                    ProcessorStatus pr = nifiFlow.getProcessors().get(cid);

                    final Referenceable flowPathRef = prepareFlowPathWithTraits(event);
                    flowPathRef.set(ATTR_NAME, (pr == null ? "UNKNOWN" : pr.getName()));
                    flowPathRef.set(ATTR_DESCRIPTION, event.getAttribute("filename") + " : " + flowPath.getName());
                    flowPathRef.set(ATTR_QUALIFIED_NAME, qname);
                    //flowPathRef.set(ATTR_NIFI_FLOW, flowRef);
                    flowPathRef.set(ATTR_URL, nifiFlow.getUrl());
                    addProcessorToFlowPath(flowPathRef, pr);

                    Collection<Referenceable> addedFlowPath = new ArrayList<>();
                    addedFlowPath.add(flowPathRef);
                    flowRef.set(ATTR_FLOW_PATHS, addedFlowPath);

                    nifiAtlasHook.addDataSetRefs(refs, flowPathRef, (event.getEventType() == ProvenanceEventType.RECEIVE));
                    //nifiAtlasHook.addUpdateReferenceable(flowRef);
                }
            }
        } catch (Exception e) {
            // If something went wrong, log it and continue with other records.
            getLogger().error("Skipping failed analyzing event {} due to {}.", new Object[]{event, e}, e);
        }

    }

    private Referenceable prepareFlowPathWithTraits (ProvenanceEventRecord event) {
        String[] traits = null;

        // Add the Atlas classifications when they have been updated
        if (event.getAttributes().get("atlas.classifications") != null) {
            String classifications = event.getAttributes().get("atlas.classifications");
            traits = StringUtils.split(classifications, "\n");
        }

        return (traits == null ? new Referenceable(TYPE_NIFI_FLOW_PATH) : new Referenceable(TYPE_NIFI_FLOW_PATH, traits));
    }

    private void addProcessorToFlowPath (Referenceable flowPathRef, ProcessorStatus pr) {
        Collection<Referenceable> pids = new ArrayList<>();
        if (pr != null) {
            final Referenceable ref = new Referenceable(TYPE_NIFI_PROCESSOR);
            ref.set(ATTR_NAME, pr.getName());
            ref.set(ATTR_QUALIFIED_NAME, pr.getId());
            pids.add(ref);
        }
        flowPathRef.set(ATTR_PROCESSORS, pids);

    }

    private Collection<Referenceable> getNifiDataRefs (ProvenanceEventRecord event) {
        Collection<Referenceable> list = new ArrayList<>();
        if (event.getChildUuids() != null) {
            for (String uuid : event.getChildUuids()) {
                final Referenceable ref = new Referenceable(TYPE_NIFI_DATA);
                ref.set(ATTR_NAME, uuid);
                ref.set(ATTR_QUALIFIED_NAME, uuid);
                ref.set(ATTR_DESCRIPTION, "Clone of " + event.getFlowFileUuid());
                list.add(ref);
            }
        }
        return list;
    }
}
