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
