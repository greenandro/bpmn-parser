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
