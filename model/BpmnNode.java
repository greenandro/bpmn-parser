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
