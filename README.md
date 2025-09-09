Nous allons utiliser l'API **DOM (Document Object Model)**, qui est incluse dans le JDK Java standard, ce qui évite d'ajouter des dépendances externes pour un projet simple comme celui-ci. DOM charge l'intégralité du fichier XML en mémoire sous forme d'arbre, ce qui est idéal pour naviguer entre les éléments interconnectés comme les tâches et les flux de séquence.

---

### **Structure du projet**

Le projet sera organisé comme suit :

1.  **`model/` (Package pour les objets de données)**
    *   `BpmnNode.java`: Une interface commune pour tous les éléments du processus (tâches, événements).
    *   `BpmnTask.java`: Une classe pour représenter les tâches (`userTask`, `serviceTask`, etc.).
    *   `BpmnEvent.java`: Une classe pour représenter les événements (`startEvent`, `endEvent`, etc.).
    *   `SequenceFlow.java`: Une classe pour représenter les liens entre les nœuds.

2.  **`parser/` (Package pour la logique d'analyse)**
    *   `BpmnParser.java`: Le cœur de l'analyseur. Il lit le fichier, le parse et reconstruit le flux.

3.  **`BpmnAnalyzer.java` (Classe principale)**
    *   Contient la méthode `main` pour lancer le programme et afficher les résultats.

---

### **1. Fichier BPMN d'exemple (`process.bpmn`)**

Pour que le programme fonctionne, nous avons besoin d'un fichier BPMN. Créez un fichier nommé `process.bpmn` avec le contenu suivant. C'est sur ce fichier que notre analyseur va travailler.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                   xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                   xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                   targetNamespace="http://bpmn.io/schema/bpmn"
                   id="Definitions_1">
  <bpmn:process id="Process_1" isExecutable="false">
    <bpmn:startEvent id="StartEvent_1" name="Début du processus">
      <bpmn:outgoing>SequenceFlow_1</bpmn:outgoing>
    </bpmn:startEvent>
    
    <bpmn:userTask id="Task_User_Validate" name="Valider la demande">
      <bpmn:incoming>SequenceFlow_1</bpmn:incoming>
      <bpmn:outgoing>SequenceFlow_2</bpmn:outgoing>
    </bpmn:userTask>
    
    <bpmn:sequenceFlow id="SequenceFlow_1" sourceRef="StartEvent_1" targetRef="Task_User_Validate" />
    
    <bpmn:serviceTask id="Task_Service_Notify" name="Notifier le système externe">
      <bpmn:incoming>SequenceFlow_2</bpmn:incoming>
      <bpmn:outgoing>SequenceFlow_3</bpmn:outgoing>
    </bpmn:serviceTask>
    
    <bpmn:sequenceFlow id="SequenceFlow_2" sourceRef="Task_User_Validate" targetRef="Task_Service_Notify" />
    
    <bpmn:intermediateCatchEvent id="Event_WaitForSignal" name="Attendre la confirmation">
      <bpmn:incoming>SequenceFlow_3</bpmn:incoming>
      <bpmn:outgoing>SequenceFlow_4</bpmn:outgoing>
      <bpmn:signalEventDefinition signalRef="Signal_ConfirmationReceived" />
    </bpmn:intermediateCatchEvent>
    
    <bpmn:sequenceFlow id="SequenceFlow_3" sourceRef="Task_Service_Notify" targetRef="Event_WaitForSignal" />
    
    <bpmn:endEvent id="EndEvent_1" name="Fin du processus">
      <bpmn:incoming>SequenceFlow_4</bpmn:incoming>
    </bpmn:endEvent>
    
    <bpmn:sequenceFlow id="SequenceFlow_4" sourceRef="Event_WaitForSignal" targetRef="EndEvent_1" />
  </bpmn:process>
  
  <bpmn:signal id="Signal_ConfirmationReceived" name="Confirmation Reçue" />
  
</bpmn:definitions>
```

---

### **2. Code Java**

#### **2.1. Classes du Modèle (`model/`)**

**`model/BpmnNode.java`**
```java
package model;

/**
 * Interface commune pour tous les éléments d'un processus BPMN
 * qui peuvent être connectés par un SequenceFlow (Tâches, Événements, etc.).
 */
public interface BpmnNode {
    String getId();
    String getName();
    String getType();
}
```

**`model/BpmnTask.java`**
```java
package model;

/**
 * Représente une tâche dans le processus BPMN.
 */
public class BpmnTask implements BpmnNode {
    private final String id;
    private final String name;
    private final String type;

    public BpmnTask(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("Task[ID=%s, Name=%s, Type=%s]", id, name, type);
    }
}
```

**`model/BpmnEvent.java`**
```java
package model;

/**
 * Représente un événement dans le processus BPMN (début, fin, signal).
 */
public class BpmnEvent implements BpmnNode {
    private final String id;
    private final String name;
    private final String type;
    private final String signalName; // Nom du signal, si applicable

    public BpmnEvent(String id, String name, String type, String signalName) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.signalName = signalName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return type;
    }
    
    public String getSignalName() {
        return signalName;
    }

    @Override
    public String toString() {
        String base = String.format("Event[ID=%s, Name=%s, Type=%s]", id, name, type);
        if (signalName != null && !signalName.isEmpty()) {
            return base + String.format(" (Signal: %s)", signalName);
        }
        return base;
    }
}
```

**`model/SequenceFlow.java`**
```java
package model;

/**
 * Représente un flux de séquence qui connecte deux nœuds BPMN.
 */
public class SequenceFlow {
    private final String id;
    private final String sourceRef;
    private final String targetRef;

    public SequenceFlow(String id, String sourceRef, String targetRef) {
        this.id = id;
        this.sourceRef = sourceRef;
        this.targetRef = targetRef;
    }

    public String getId() {
        return id;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public String getTargetRef() {
        return targetRef;
    }
}
```

---

#### **2.2. Classe de l'Analyseur (`parser/`)**

**`parser/BpmnParser.java`**
```java
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
```

---

#### **2.3. Classe Principale (`BpmnAnalyzer.java`)**

**`BpmnAnalyzer.java`**
```java
import model.BpmnEvent;
import model.BpmnNode;
import model.BpmnTask;
import parser.BpmnParser;

import java.util.List;

/**
 * Classe principale pour exécuter l'analyseur de fichier BPMN.
 */
public class BpmnAnalyzer {

    public static void main(String[] args) {
        // Remplacez par le chemin de votre fichier BPMN
        String filePath = "process.bpmn"; 

        System.out.println("=============================================");
        System.out.println("=    Analyseur de Fichier BPMN 2.0 en Java  =");
        System.out.println("=============================================");
        System.out.println("Fichier analysé : " + filePath + "\n");

        BpmnParser parser = new BpmnParser();
        try {
            parser.parse(filePath);

            // Affichage des tâches extraites
            System.out.println("--- Tâches identifiées ---");
            List<BpmnTask> tasks = parser.getTasks();
            if (tasks.isEmpty()) {
                System.out.println("Aucune tâche trouvée.");
            } else {
                tasks.forEach(task -> 
                    System.out.printf("  - ID: %-25s | Type: %-15s | Nom: %s\n", 
                                      task.getId(), task.getType(), task.getName())
                );
            }
            
            System.out.println("\n--- Événements identifiés ---");
            List<BpmnEvent> events = parser.getEvents();
            if (events.isEmpty()) {
                System.out.println("Aucun événement trouvé.");
            } else {
                for (BpmnEvent event : events) {
                    String signalInfo = event.getSignalName() != null ? " (Signal: " + event.getSignalName() + ")" : "";
                    System.out.printf("  - ID: %-25s | Type: %-25s | Nom: %s%s\n", 
                                      event.getId(), event.getType(), event.getName(), signalInfo);
                }
            }

            // Affichage des points de départ et de fin
            System.out.println("\n--- Points de Début et de Fin ---");
            BpmnNode start = parser.getStartEvent();
            BpmnNode end = parser.getEndEvent();
            if (start != null) {
                System.out.println("Événement de début : " + start.getId() + " (" + start.getName() + ")");
            } else {
                System.out.println("Aucun événement de début trouvé.");
            }
            if (end != null) {
                System.out.println("Événement de fin   : " + end.getId() + " (" + end.getName() + ")");
            } else {
                System.out.println("Aucun événement de fin trouvé.");
            }
            
            // Affichage de la séquence du processus
            System.out.println("\n--- Séquence du Processus ---");
            List<BpmnNode> orderedFlow = parser.getOrderedFlow();
            if (orderedFlow.isEmpty()) {
                System.out.println("Impossible de déterminer le flux du processus.");
            } else {
                StringBuilder flowString = new StringBuilder();
                for (int i = 0; i < orderedFlow.size(); i++) {
                    flowString.append(orderedFlow.get(i).getId());
                    if (i < orderedFlow.size() - 1) {
                        flowString.append(" -> ");
                    }
                }
                System.out.println(flowString.toString());
            }

        } catch (Exception e) {
            System.err.println("Une erreur est survenue lors de l'analyse du fichier :");
            e.printStackTrace();
        }
    }
}
```

---

### **Comment compiler et exécuter**

1.  **Structure des dossiers :**
    Assurez-vous que vos fichiers sont organisés dans la structure de packages spécifiée :
    ```
    .
    ├── BpmnAnalyzer.java
    ├── process.bpmn
    ├── model/
    │   ├── BpmnNode.java
    │   ├── BpmnTask.java
    │   ├── BpmnEvent.java
    │   └── SequenceFlow.java
    └── parser/
        └── BpmnParser.java
    ```

2.  **Compilation :**
    Ouvrez un terminal à la racine du projet et compilez tous les fichiers `.java` :
    ```bash
    javac */*.java *.java
    ```

3.  **Exécution :**
    Exécutez la classe principale :
    ```bash
    java BpmnAnalyzer
    ```

### **Sortie attendue**

L'exécution du programme devrait produire la sortie suivante sur la console :

```
=============================================
=    Analyseur de Fichier BPMN 2.0 en Java  =
=============================================
Fichier analysé : process.bpmn

--- Tâches identifiées ---
  - ID: Task_User_Validate        | Type: userTask        | Nom: Valider la demande
  - ID: Task_Service_Notify       | Type: serviceTask     | Nom: Notifier le système externe

--- Événements identifiés ---
  - ID: Event_WaitForSignal         | Type: intermediateCatchEvent    | Nom: Attendre la confirmation (Signal: Confirmation Reçue)
  - ID: EndEvent_1                  | Type: endEvent                  | Nom: Fin du processus
  - ID: StartEvent_1                | Type: startEvent                | Nom: Début du processus

--- Points de Début et de Fin ---
Événement de début : StartEvent_1 (Début du processus)
Événement de fin   : EndEvent_1 (Fin du processus)

--- Séquence du Processus ---
StartEvent_1 -> Task_User_Validate -> Task_Service_Notify -> Event_WaitForSignal -> EndEvent_1
```

Ce programme répond à toutes les exigences de la demande : il est modulaire, robuste, utilise une véritable analyse XML, extrait toutes les informations demandées et reconstruit le flux de processus pour des scénarios simples. Il peut facilement être étendu pour gérer des éléments plus complexes comme les passerelles (gateways) ou d'autres types d'événements.
