package org.apache.nifi.atlas.provenance;

import org.apache.nifi.provenance.ProvenanceEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class NiFiProvenanceEventAnalyzerFactory {

    private static final Logger logger = LoggerFactory.getLogger(NiFiProvenanceEventAnalyzerFactory.class);
    private static final Map<Pattern, NiFiProvenanceEventAnalyzer> analyzersForComponentType = new ConcurrentHashMap<>();
    private static final Map<Pattern, NiFiProvenanceEventAnalyzer> analyzersForTransitUri = new ConcurrentHashMap<>();
    private static final Map<ProvenanceEventType, NiFiProvenanceEventAnalyzer> analyzersForProvenanceEventType = new ConcurrentHashMap<>();
    private static boolean loaded = false;

    private static void loadAnalyzers() {
        logger.debug("Loading NiFiProvenanceEventAnalyzer ...");
        final ServiceLoader<NiFiProvenanceEventAnalyzer> serviceLoader
                = ServiceLoader.load(NiFiProvenanceEventAnalyzer.class);
        serviceLoader.forEach(analyzer -> {
            addAnalyzer(analyzer.targetComponentTypePattern(), analyzersForComponentType, analyzer);
            addAnalyzer(analyzer.targetTransitUriPattern(), analyzersForTransitUri, analyzer);
            final ProvenanceEventType eventType = analyzer.targetProvenanceEventType();
            if (eventType != null) {
                if (analyzersForProvenanceEventType.containsKey(eventType)) {
                    logger.warn("Fo ProvenanceEventType {}, an Analyzer {} is already assigned." +
                            " Only one analyzer for a type can be registered. Ignoring {}",
                            eventType, analyzersForProvenanceEventType.get(eventType), analyzer);
                }
                analyzersForProvenanceEventType.put(eventType, analyzer);
            }
        });
        logger.info("Loaded NiFiProvenanceEventAnalyzers: componentTypes={}, transitUris={}", analyzersForComponentType, analyzersForTransitUri);
    }

    private static void addAnalyzer(String patternStr, Map<Pattern, NiFiProvenanceEventAnalyzer> toAdd,
                                    NiFiProvenanceEventAnalyzer analyzer) {
        if (patternStr != null && !patternStr.isEmpty()) {
            Pattern pattern = Pattern.compile(patternStr.trim());
            toAdd.put(pattern, analyzer);
        }
    }

    /**
     * Find and retrieve NiFiProvenanceEventAnalyzer implementation for the specified targets.
     * Pattern matching is performed by following order, and the one found at first is returned:
     * <ol>
     * <li>Component type name. Use an analyzer supporting the Component type with its {@link NiFiProvenanceEventAnalyzer#targetProvenanceEventType()}.
     * <li>TransitUri. Use an analyzer supporting the TransitUri with its {@link NiFiProvenanceEventAnalyzer#targetTransitUriPattern()}.
     * <li>Provenance Event Type. Use an analyzer supporting the Provenance Event Type with its {@link NiFiProvenanceEventAnalyzer#targetProvenanceEventType()}.
     * </ol>
     * @param typeName NiFi component type name.
     * @param transitUri Transit URI.
     * @param eventType Provenance event type.
     * @return Instance of NiFiProvenanceEventAnalyzer if one is found for the specified className, otherwise null.
     */
    public static NiFiProvenanceEventAnalyzer getAnalyzer(String typeName, String transitUri, ProvenanceEventType eventType) {

        if (!loaded) {
            synchronized (analyzersForComponentType) {
                if (!loaded) {
                    loadAnalyzers();
                    loaded = true;
                }
            }
        }

        // TODO: implement a simple limited size cache mechanism here? If performance becomes a problem.

        for (Map.Entry<Pattern, NiFiProvenanceEventAnalyzer> entry : analyzersForComponentType.entrySet()) {
            if (entry.getKey().matcher(typeName).matches()) {
                return entry.getValue();
            }
        }

        if (transitUri != null) {
            for (Map.Entry<Pattern, NiFiProvenanceEventAnalyzer> entry : analyzersForTransitUri.entrySet()) {
                if (entry.getKey().matcher(transitUri).matches()) {
                    return entry.getValue();
                }
            }
        }

        // If there's no specific implementation, just use generic analyzer.
        return analyzersForProvenanceEventType.get(eventType);
    }
}
