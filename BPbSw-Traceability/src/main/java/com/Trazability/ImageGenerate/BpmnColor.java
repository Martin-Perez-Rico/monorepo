package com.Trazability.ImageGenerate;

import com.Trazability.Main;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.UserTask;

import java.io.*;
import java.util.*;

public class BpmnColor {

    private final String bpmnFilePath = Main.getBpmnFilePath();
    private final File archivoTemp = new File(System.getProperty("user.dir") + File.separator + "output", "ColorModel.bpmn");
    private final String stroke = "#0d4372";
    private final String fill = "#ffe0b2";
    private final String background = "#ffe0b2";
    private final String border = "#6b3c00";

    public void modifyActivityColors(List<String> activityNames) {
        BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(bpmnFilePath));
        List<String> activityIds = findActivityIds(modelInstance, activityNames);
        modifyColor(activityIds);
    }

    private List<String> findActivityIds(BpmnModelInstance modelInstance, List<String> activityNames) {
        List<String> activityIds = new ArrayList<>();
        for (String activityName : activityNames) {
            UserTask userTask = findUserTaskByName(modelInstance, activityName);
            if (userTask != null) {
                activityIds.add(userTask.getId());
            }
        }
        return activityIds;
    }

    private UserTask findUserTaskByName(BpmnModelInstance modelInstance, String activityName) {
        for (UserTask userTask : modelInstance.getModelElementsByType(UserTask.class)) {
            if (activityName.equals(userTask.getName())) {
                return userTask;
            }
        }
        return null;
    }

    private void modifyColor(List<String> activityIds) {
        Set<String> idsModificados = new HashSet<>(activityIds);
        List<String> lineasModificadas = new LinkedList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(bpmnFilePath))) {
            String linea;

            while ((linea = br.readLine()) != null) {
                if (linea.contains("<bpmn:definitions") && !linea.contains("xmlns:bioc=\"http://bpmn.io/schema/bpmn/biocolor/1.0\"") && !linea.contains("xmlns:color=\"http://www.omg.org/spec/BPMN/non-normative/color/1.0\"")) {
                    lineasModificadas.add(modifyLineDefinitions(linea));
                } else if (linea.contains("bpmndi:BPMNShape") && linea.contains("bpmnElement")) {
                    String bpmnElement = getAttributeValue(linea, "bpmnElement");
                    if (bpmnElement != null && idsModificados.contains(bpmnElement)) {
                        lineasModificadas.add(modifyLineColor(linea));
                        idsModificados.remove(bpmnElement);
                    } else {
                        lineasModificadas.add(linea);
                    }
                } else {
                    lineasModificadas.add(linea);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivoTemp))) {
            for (String lineaModificada : lineasModificadas) {
                bw.write(lineaModificada);
                bw.newLine();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private String getAttributeValue(String linea, String atributo) {
        String patron = atributo + "=\"";
        int startIndex = linea.indexOf(patron);
        if (startIndex != -1) {
            startIndex += patron.length();
            int endIndex = linea.indexOf("\"", startIndex);
            if (endIndex != -1) {
                return linea.substring(startIndex, endIndex);
            }
        }
        return null;
    }

    private String modifyLineColor(String linea) {
        return linea.replace(">", "")
                + " bioc:stroke=\"" + stroke + "\" bioc:fill=\"" + fill + "\" "
                + "color:background-color=\"" + background + "\" color:border-color=\"" + border + "\">";
    }

    private String modifyLineDefinitions(String linea) {
        return linea.replace(">", "")
                + " xmlns:bioc=\"http://bpmn.io/schema/bpmn/biocolor/1.0\" xmlns:color=\"http://www.omg.org/spec/BPMN/non-normative/color/1.0\" >";
    }

    public String findParticipantName() {
        BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(bpmnFilePath));
        for (Participant participant : modelInstance.getModelElementsByType(Participant.class)) {
            return participant.getName();
        }
        return null;
    }
}
