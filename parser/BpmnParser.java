package parser;

import model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Classe principale pour l'analyse d'un fichier BPMN.
 * Elle lit le fichier, extrait les éléments et reconstruit le flux.
 */
public class BpmnParser {

    private final Map<String, BpmnNode> nodes = new HashMap<>();
    private final List<SequenceFlow> sequenceFlows = new ArrayList<>();
    private final Map<String, String> signalDefinitions = new HashMap<>();
    private BpmnNode startEvent;
    private BpmnNode endEvent;

    /**
     * Méthode principale qui analyse le fichier BPMN.
     * @param filePath Chemin d'accès au fichier .bpmn
     * @throws Exception Si une erreur de parsing ou de lecture survient.
     */
    public void parse(String filePath) throws Exception {
        File bpmnFile = new File(filePath);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(bpmnFile);
        doc.getDocumentElement().normalize();

        // D'abord, trouver les définitions de signaux globaux
        extractSignalDefinitions(doc);

        // Ensuite, trouver l'élément <process>
        NodeList processList = doc.getElementsByTagName("bpmn:process");
        if (processList.getLength() == 0) {
            processList = doc.getElementsByTagName("process"); // Fallback sans namespace
        }
        if (processList.getLength() > 0) {
            Node processNode = processList.item(0);
            extractElements(processNode);
        } else {
            throw new Exception("Aucun élément <process> trouvé dans le fichier BPMN.");
        }
    }

    /**
     * Extrait les définitions de signaux (<bpmn:signal>)
     */
    private void extractSignalDefinitions(Document doc) {
        NodeList signalList = doc.getElementsByTagName("bpmn:signal");
        if (signalList.getLength() == 0) {
            signalList = doc.getElementsByTagName("signal");
        }
        for (int i = 0; i < signalList.getLength(); i++) {
            Node node = signalList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String id = element.getAttribute("id");
                String name = element.getAttribute("name");
                if (id != null && name != null) {
                    signalDefinitions.put(id, name);
                }
            }
        }
    }

    /**
     * Extrait tous les éléments (tâches, événements, flux) à l'intérieur d'un nœud <process>.
     */
    private void extractElements(Node processNode) {
        NodeList childNodes = processNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String id = element.getAttribute("id");
                String name = element.getAttribute("name");

                switch (element.getTagName()) {
                    // --- Événements ---
                    case "bpmn:startEvent":
                    case "startEvent":
                        startEvent = new BpmnEvent(id, name, "startEvent", null);
                        nodes.put(id, startEvent);
                        break;
                    case "bpmn:endEvent":
                    case "endEvent":
                        endEvent = new BpmnEvent(id, name, "endEvent", null);
                        nodes.put(id, endEvent);
                        break;
                    case "bpmn:intermediateCatchEvent":
                    case "intermediateCatchEvent":
                        String signalName = getSignalNameFromEvent(element);
                        nodes.put(id, new BpmnEvent(id, name, "intermediateCatchEvent", signalName));
                        break;

                    // --- Tâches ---
                    case "bpmn:userTask":
                    case "userTask":
                        nodes.put(id, new BpmnTask(id, name, "userTask"));
                        break;
                    case "bpmn:serviceTask":
                    case "serviceTask":
                        nodes.put(id, new BpmnTask(id, name, "serviceTask"));
                        break;
                    // Ajoutez d'autres types de tâches ici (sendTask, receiveTask, etc.)

                    // --- Flux de séquence ---
                    case "bpmn:sequenceFlow":
                    case "sequenceFlow":
                        String sourceRef = element.getAttribute("sourceRef");
                        String targetRef = element.getAttribute("targetRef");
                        sequenceFlows.add(new SequenceFlow(id, sourceRef, targetRef));
                        break;
                }
            }
        }
    }

    /**
     * Recherche un signalEventDefinition dans un événement et retourne son nom.
     */
    private String getSignalNameFromEvent(Element eventElement) {
        NodeList signalDefs = eventElement.getElementsByTagName("bpmn:signalEventDefinition");
        if (signalDefs.getLength() == 0) {
            signalDefs = eventElement.getElementsByTagName("signalEventDefinition");
        }
        if (signalDefs.getLength() > 0) {
            Element signalDef = (Element) signalDefs.item(0);
            String signalRef = signalDef.getAttribute("signalRef");
            return signalDefinitions.getOrDefault(signalRef, "Référence de signal inconnue: " + signalRef);
        }
        return null;
    }

    /**
     * Reconstruit le chemin d'exécution principal à partir du startEvent.
     * Note : Ceci fonctionne pour des processus simples sans passerelles (gateways).
     * @return Une liste ordonnée des nœuds du processus.
     */
    public List<BpmnNode> getOrderedFlow() {
        if (startEvent == null) {
            return Collections.emptyList();
        }

        List<BpmnNode> orderedFlow = new ArrayList<>();
        BpmnNode currentNode = startEvent;
        Set<String> visitedFlows = new HashSet<>();

        while (currentNode != null) {
            orderedFlow.add(currentNode);
            if (currentNode.equals(endEvent)) {
                break; // Fin du processus
            }

            // Trouver le prochain flux de séquence non visité partant du nœud actuel
            final String currentId = currentNode.getId();
            SequenceFlow nextFlow = sequenceFlows.stream()
                    .filter(sf -> sf.getSourceRef().equals(currentId) && !visitedFlows.contains(sf.getId()))
                    .findFirst()
                    .orElse(null);

            if (nextFlow != null) {
                visitedFlows.add(nextFlow.getId());
                currentNode = nodes.get(nextFlow.getTargetRef());
            } else {
                currentNode = null; // Pas de flux sortant, fin du chemin
            }
        }
        return orderedFlow;
    }

    // --- Getters pour l'affichage des résultats ---

    public Collection<BpmnNode> getAllNodes() {
        return nodes.values();
    }

    public List<BpmnTask> getTasks() {
        return nodes.values().stream()
                .filter(n -> n instanceof BpmnTask)
                .map(n -> (BpmnTask) n)
                .collect(Collectors.toList());
    }

    public List<BpmnEvent> getEvents() {
        return nodes.values().stream()
                .filter(n -> n instanceof BpmnEvent)
                .map(n -> (BpmnEvent) n)
                .collect(Collectors.toList());
    }

    public BpmnNode getStartEvent() {
        return startEvent;
    }

    public BpmnNode getEndEvent() {
        return endEvent;
    }
}
