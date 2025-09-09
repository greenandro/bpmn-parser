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
