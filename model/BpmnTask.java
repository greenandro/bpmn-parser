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
