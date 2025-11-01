package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Partition {
    private String name;
    private long size;
    private long initialLimit;  // ← NUEVO: Índice inicial
    private long finalLimit;    // ← NUEVO: Índice final
    private ArrayList<Process> assignedProcesses;
    private Set<String> processHistoryNames;
    private Map<String, Long> processExecutionTime;
    private boolean available;

    // Constructor original (sin límites)
    public Partition(String name, long size) {
        this.name = name;
        this.size = size;
        this.initialLimit = 0;  // ← INICIALIZAR
        this.finalLimit = 0;    // ← INICIALIZAR
        this.assignedProcesses = new ArrayList<>();
        this.processHistoryNames = new HashSet<>();
        this.processExecutionTime = new HashMap<>();
        this.available = false;  
    }

    // ← NUEVO: Constructor con límites
    public Partition(String name, long size, long initialLimit, long finalLimit) {
        this.name = name;
        this.size = size;
        this.initialLimit = initialLimit;
        this.finalLimit = finalLimit;
        this.assignedProcesses = new ArrayList<>();
        this.processHistoryNames = new HashSet<>();
        this.processExecutionTime = new HashMap<>();
        this.available = false;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }


    public long getInitialLimit() {
        return initialLimit;
    }

    public void setInitialLimit(long initialLimit) {
        this.initialLimit = initialLimit;
    }

    public long getFinalLimit() {
        return finalLimit;
    }

    public void setFinalLimit(long finalLimit) {
        this.finalLimit = finalLimit;
    }

    public ArrayList<Process> getAssignedProcesses() {
        return assignedProcesses;
    }

    public void addProcess(Process process) {
        if (!assignedProcesses.contains(process)) {
            assignedProcesses.add(process);
        }
        if (process != null) {
            processHistoryNames.add(process.getName());
        }
    }

    public void removeProcess(Process process) {
        assignedProcesses.remove(process);
    }

    public void removeProcessByName(String processName) {
        assignedProcesses.removeIf(p -> p.getName().equalsIgnoreCase(processName));
    }

    public void addExecutionTime(String processName, long timeExecuted) {
        long currentTime = processExecutionTime.getOrDefault(processName, 0L);
        processExecutionTime.put(processName, currentTime + timeExecuted);
    }

    public long getTotalExecutionTime() {
        long total = 0;
        for (Long time : processExecutionTime.values()) {
            total += time;
        }
        return total;
    }

    public String getProcessHistoryString() {
        if (processHistoryNames.isEmpty()) {
            return "Ninguno";
        }
        
        ArrayList<String> sortedNames = new ArrayList<>(processHistoryNames);
        sortedNames.sort(String::compareTo);
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sortedNames.size(); i++) {
            sb.append(sortedNames.get(i));
            if (i < sortedNames.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public Set<String> getProcessHistoryNames() {
        return new HashSet<>(processHistoryNames);
    }

    public int getProcessCount() {
        return assignedProcesses.size();
    }

    public boolean hasAssignedProcesses() {
        return !assignedProcesses.isEmpty();
    }

    public void clearExecutionData() {
        assignedProcesses.clear();
        processHistoryNames.clear();
        processExecutionTime.clear();
    }

    @Override
    public String toString() {
        return name;
    }

    
    public String toDetailedString() {
        return name + " [" + initialLimit + " - " + finalLimit + "] Tamaño: " + size;
    }
}