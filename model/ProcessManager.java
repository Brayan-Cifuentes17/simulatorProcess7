package model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessManager {
    private ArrayList<Process> initialProcesses;
    private ArrayList<Partition> partitions;
    private ArrayList<Log> executionLogs;
    private ArrayList<Partition> internalPartitions;
    private ArrayList<Condensation> condensations;
    private ArrayList<Compactation> compactations;
    private boolean isFirstCondensation;

    public ProcessManager() {
        initialProcesses = new ArrayList<>();
        partitions = new ArrayList<>();
        executionLogs = new ArrayList<>();
        internalPartitions = new ArrayList<>();
        condensations = new ArrayList<>();
        compactations = new ArrayList<>();
        isFirstCondensation = true;
    }

    // ========== GESTIÓN DE PARTICIONES ==========
    
    public void addPartition(String name, long size) {
        Partition partition = new Partition(name, size);
        partitions.add(partition);
    }
    
    public void addPartition(Partition partition) {
        partitions.add(partition);
    }

    public void editPartition(String partitionName, long newSize) {
        Partition partition = findPartitionByName(partitionName);
        if (partition != null) {
            partition.setSize(newSize);
        }
    }

    public boolean partitionExists(String name) {
        return partitions.stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(name.trim()));
    }

    public void removePartition(String name) {
        partitions.removeIf(p -> p.getName().equalsIgnoreCase(name.trim()));
    }

    public Partition findPartitionByName(String name) {
        return partitions.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(null);
    }
    
    public Partition searchPartition(String name) {
        for (int i = 0; i < partitions.size(); i++) {
            if (partitions.get(i).getName().equalsIgnoreCase(name)) {
                return partitions.get(i);
            }
        }
        return null;
    }

    public boolean hasPartitionAssignedProcesses(String partitionName) {
        return initialProcesses.stream()
                .anyMatch(p -> p.getPartition() != null && 
                         p.getPartition().getName().equalsIgnoreCase(partitionName));
    }

    public ArrayList<Partition> getPartitions() {
        return new ArrayList<>(partitions);
    }

    // ========== GESTIÓN DE PROCESOS ==========
    
    public void addProcess(String name, long time, Status status, long size) {
        Process process = new Process(name, time, status, size);  
        initialProcesses.add(process);
    }

    public boolean processExists(String name) {
        return initialProcesses.stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(name.trim()));
    }

    public void removeProcess(String name) {
        Process process = findProcessByName(name);
        if (process != null && process.getPartition() != null) {
            process.getPartition().removeProcess(process);
        }
        initialProcesses.removeIf(p -> p.getName().equalsIgnoreCase(name.trim()));
    }

    public void editProcess(int position, String processName, long newTime, 
                        Status newStatus, long newSize) {
        if (position >= 0 && position < initialProcesses.size()) {
            Process existingProcess = initialProcesses.get(position);
            if (existingProcess.getName().equalsIgnoreCase(processName)) {
                existingProcess.setOriginalTime(newTime);
                existingProcess.setStatus(newStatus);
                existingProcess.setSize(newSize);
            }
        }
    }

    private Process findProcessByName(String name) {
        return initialProcesses.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(null);
    }

    public boolean isEmpty() {
        return initialProcesses.isEmpty();
    }

    public ArrayList<Process> getInitialProcesses() {
        return new ArrayList<>(initialProcesses);
    }

    // ========== SIMULACIÓN CON CONDENSACIÓN Y COMPACTACIÓN ==========
    
    public void runSimulation() {
        executionLogs.clear();
        condensations.clear();
        compactations.clear();
        isFirstCondensation = true;
        
        // Limpiar particiones
        for (Partition p : partitions) {
            p.clearExecutionData();
            p.setAvailable(false);
        }
        
        
        assignInitialPartitions();
        
        
        internalPartitions = new ArrayList<>(partitions);
        
        
        initialValues();
        
        // Registrar particiones iniciales
        for (Partition part : partitions) {
            Process dummyProcess = new Process("", 0, Status.NO_BLOQUEADO, part.getSize());
            dummyProcess.setPartition(part);
            addLog(dummyProcess, Filter.PARTICIONES);
        }
        
        // Clonar y ordenar procesos por tiempo
        ArrayList<Process> processQueue = new ArrayList<>();
        for (Process p : initialProcesses) {
            processQueue.add(p.clone());
        }
        processQueue.sort((p1, p2) -> Long.compare(p1.getOriginalTime(), p2.getOriginalTime()));
        
        // Ejecutar ciclo de simulación
        while (!processQueue.isEmpty()) {
            Process currentProcess = processQueue.remove(0);
            startCycle(currentProcess, processQueue);
        }
        
        // Resetear tiempos para segunda fase
        resetTimes();
        
        // Segunda fase: registrar logs reales
        processQueue = new ArrayList<>();
        for (Process p : initialProcesses) {
            processQueue.add(p.clone());
        }
        processQueue.sort((p1, p2) -> Long.compare(p1.getOriginalTime(), p2.getOriginalTime()));
        
        int index = 0;
        while (!processQueue.isEmpty()) {
            Process currentProcess = processQueue.remove(0);
            startRealCycle(currentProcess, processQueue, index);
            
            if (currentProcess.getName().equalsIgnoreCase(initialProcesses.get(initialProcesses.size() - 1).getName())) {
                index++;
            }
        }
        
        resetTimes();
    }

    // ← Asignar particiones iniciales con límites
    public void assignInitialPartitions() {
        for (Process process : initialProcesses) {
            Partition partition = new Partition(
                partitionName(), 
                process.getSize(),
                partitions.size() == 0 ? 0 : partitions.get(partitions.size() - 1).getFinalLimit(),
                partitions.size() == 0 ? process.getSize() : partitions.get(partitions.size() - 1).getFinalLimit() + process.getSize()
            );
            
            addPartition(partition);
            process.setPartition(partition);
            process.addToPartitionHistory(partition);
            partition.addProcess(process);
        }
    }

    public void initialValues() {
        for (Process process : initialProcesses) {
            addLog(process, Filter.INICIAL);
        }
    }

    // ← Ciclo de simulación (primera fase)
    private void startCycle(Process currentProcess, ArrayList<Process> remainingProcesses) {
        // Registrar en estado listo
        ready(currentProcess);
        
        // Ejecutar quantum
        long timeToExecute = Math.min(Constants.QUANTUM_TIME, currentProcess.getRemainingTime());
        
        if (currentProcess.getPartition() != null) {
            currentProcess.getPartition().addExecutionTime(
                currentProcess.getName(), 
                timeToExecute
            );
        }
        
        currentProcess.subtractTime(Constants.QUANTUM_TIME);
        currentProcess.incrementCycle();
        
        // ¿Terminó?
        if (currentProcess.getRemainingTime() > 0) {
            if (!currentProcess.isBlocked()) {
                // No terminó, volver a la cola
                remainingProcesses.add(currentProcess);
            } else {
                // Proceso bloqueado
                remainingProcesses.add(currentProcess);
            }
        } else {
            // Proceso terminado - liberar partición y condensar
            exitStage(currentProcess);
            substractTimeToOthers(remainingProcesses, currentProcess);
            reviewForCondensations(remainingProcesses, currentProcess, false);
        }
    }

    public void ready(Process process) {
        try {
            searchPartition(process.getPartition().getName())
                    .addExecutionTime(
                        process.getName(),
                        process.getRemainingTime() >= Constants.QUANTUM_TIME ? Constants.QUANTUM_TIME : process.getRemainingTime()
                    );
        } catch (Exception e) {
            System.out.println(process.getPartition().getName() + "," + internalPartitions.toString());
        }
    }

    public void exitStage(Process process) {
        addLog(process, Filter.FINALIZADO);
    }

    public void substractTimeToOthers(ArrayList<Process> remainingProcesses, Process currentProcess) {
        long lastTime = currentProcess.getRemainingTime() < 0 ? 
                       currentProcess.getOriginalTime() % Constants.QUANTUM_TIME : 
                       Constants.QUANTUM_TIME;
        
        for (int index = 0; index < remainingProcesses.size(); index++) {
            if (remainingProcesses.get(index).getRemainingTime() <= Constants.QUANTUM_TIME) {
                remainingProcesses.get(index).subtractTime(lastTime);
                if (remainingProcesses.get(index).getPartition() != null) {
                    searchPartition(remainingProcesses.get(index).getPartition().getName())
                            .addExecutionTime(remainingProcesses.get(index).getName(), lastTime);
                }
            }
        }
    }


    public void reviewForCondensations(ArrayList<Process> processesForSearch, Process process, boolean isForExpired) {
        int position = partitionPosition(process.getPartition());
        if (position == -1) return;
        
        boolean isPenultimate = position == internalPartitions.size() - 2;
        Partition removedPartition = null;
        
        try {
            removedPartition = internalPartitions.remove(position);
        } catch (Exception e) {
            System.out.println(process.getName() + " " + process.getPartition().getName() + " " + internalPartitions.toString());
            return;
        }
        
        Condensation condensation = null;
        
        // Mover particiones y crear condensaciones
        for (int i = position; i < internalPartitions.size(); i++) {
            if (i == internalPartitions.size() - 1) {
                // Es la última partición
                if (isFirstCondensation) {
                    // Primera condensación: solo mover
                    movePartition(processesForSearch, i);
                } else {
                    // Condensaciones posteriores: fusionar con la última
                    Partition lastPartition = internalPartitions.get(i);
                    
                    long newSize = removedPartition.getSize() + lastPartition.getSize();
                    long newInitialLimit = lastPartition.getFinalLimit() - newSize;
                    long newFinalLimit = lastPartition.getFinalLimit();
                    
                    Partition finalPartition = new Partition(
                        partitionName(),
                        newSize,
                        newInitialLimit,
                        newFinalLimit
                    );
                    
                    condensation = new Condensation(
                        "Cond" + (condensations.size() + 1),
                        removedPartition, 
                        lastPartition
                    );
                    
                    internalPartitions.set(internalPartitions.size() - 1, finalPartition);
                    
                    if (!isPenultimate) {
                        Compactation compactation = new Compactation(
                            "Compactación " + (compactations.size() + 1),
                            condensation.getSize(), 
                            process, 
                            finalPartition, 
                            isForExpired
                        );
                        compactations.add(compactation);
                    }
                    
                    partitions.add(finalPartition);
                }
            } else {
                // No es la última: mover la partición
                movePartition(processesForSearch, i);
            }
        }
        
        // Si es la primera condensación, crear partición libre al final
        if (isFirstCondensation) {
            isFirstCondensation = false;
            
            Partition lastPartition = internalPartitions.get(internalPartitions.size() - 1);
            
            Partition finalPartition = new Partition(
                partitionName(), 
                removedPartition.getSize(),
                lastPartition.getFinalLimit(),
                lastPartition.getFinalLimit() + removedPartition.getSize()
            );
            
            partitions.add(finalPartition);
            internalPartitions.add(finalPartition);
            
            Compactation compactation = new Compactation(
                "Compactación " + (compactations.size() + 1),
                finalPartition.getSize(), 
                process, 
                finalPartition, 
                isForExpired
            );
            compactations.add(compactation);
        }
        
        if (condensation != null) {
            condensations.add(condensation);
        }
    }

    // ← Mover partición recalculando límites
    public void movePartition(ArrayList<Process> processesForSearch, int i) {
        long initialLimit = i == 0 ? 0 : internalPartitions.get(i - 1).getFinalLimit();
        long finalLimit = i == 0 ? internalPartitions.get(i).getSize()
                : internalPartitions.get(i - 1).getFinalLimit() + internalPartitions.get(i).getSize();
        
        Partition partitionCreated = new Partition(
            partitionName(), 
            internalPartitions.get(i).getSize(),
            initialLimit, 
            finalLimit
        );
        
        if (internalPartitions.get(i).getAssignedProcesses().size() > 0) {
            partitionCreated.addProcess(internalPartitions.get(i).getAssignedProcesses().get(0));
            
            Process processToUpdate = searchProcess(
                processesForSearch,
                internalPartitions.get(i).getAssignedProcesses().get(0).getName()
            );
            
            if (processToUpdate != null) {
                processToUpdate.setPartition(partitionCreated);
                processToUpdate.addToPartitionHistory(partitionCreated);
            }
        }
        
        partitions.add(partitionCreated);
        internalPartitions.set(i, partitionCreated);
    }

    public Process searchProcess(ArrayList<Process> processesForSearch, String name) {
        for (int i = 0; i < processesForSearch.size(); i++) {
            if (processesForSearch.get(i).getName().equalsIgnoreCase(name)) {
                return processesForSearch.get(i);
            }
        }
        return null;
    }

    public int partitionPosition(Partition partition) {
        for (int i = 0; i < internalPartitions.size(); i++) {
            if (internalPartitions.get(i).getName().equals(partition.getName())) {
                return i;
            }
        }
        return -1;
    }

    public String partitionName() {
        return "Part" + (partitions.size() + 1);
    }

    // ← Segunda fase: registrar logs reales
    private void startRealCycle(Process currentProcess, ArrayList<Process> remainingProcesses, int index) {
        try {
            currentProcess.setPartition(currentProcess.getPartitionHistory().get(index));
        } catch (Exception e) {
            System.out.println(currentProcess.getName());
        }
        
        readyLog(currentProcess);
        dispatch(currentProcess);
        inExecution(currentProcess);
        
        if (currentProcess.getRemainingTime() > 0) {
            if (!currentProcess.isBlocked()) {
                expirationTime(currentProcess);
                remainingProcesses.add(currentProcess);
            } else {
                blockedTransition(currentProcess);
                blockedStage(currentProcess);
                wakeUp(currentProcess);
                remainingProcesses.add(currentProcess);
            }
        }
    }

    public void readyLog(Process process) {
        addLog(process, Filter.LISTO);
    }

    public void dispatch(Process process) {
        addLog(process, Filter.DESPACHAR);
    }

    public void inExecution(Process process) {
        addLog(process, Filter.EN_EJECUCION);
        process.subtractTime(Constants.QUANTUM_TIME);
    }

    public void expirationTime(Process process) {
        addLog(process, Filter.TIEMPO_EXPIRADO);
    }

    public void blockedTransition(Process process) {
        addLog(process, Filter.TRANSICION_BLOQUEO);
    }

    public void blockedStage(Process process) {
        addLog(process, Filter.BLOQUEADO);
    }

    public void wakeUp(Process process) {
        addLog(process, Filter.DESPERTAR);
    }

    public void resetTimes() {
        for (Process process : initialProcesses) {
            process.resetTime();
        }
    }

    public ArrayList<Compactation> getCompactations() {
        return new ArrayList<>(compactations);
    }

    public ArrayList<Condensation> getCondensations() {
        return new ArrayList<>(condensations);
    }

    // ========== LOGS ==========
    
    private void addLog(Process process, Filter filter) {
        Log log = new Log(process, filter);
        executionLogs.add(log);
    }

    public List<Log> getLogsByFilter(Filter filter) {
        return executionLogs.stream()
                .filter(log -> log.getFilter() == filter)
                .collect(Collectors.toList());
    }

    public List<Log> getLogsByFilterAndPartition(Filter filter, String partitionName) {
        return executionLogs.stream()
                .filter(log -> log.getFilter() == filter && 
                       log.getPartition() != null &&
                       log.getPartition().getName().equalsIgnoreCase(partitionName))
                .collect(Collectors.toList());
    }

    public ArrayList<Log> getAllLogs() {
        return new ArrayList<>(executionLogs);
    }

    // ========== INFORMES ==========
    
    public List<PartitionFinalizationInfo> getPartitionFinalizationReport() {
        List<PartitionFinalizationInfo> report = new ArrayList<>();
        
        for (Partition partition : partitions) {
            String processNames = partition.getProcessHistoryString();
            long totalTime = partition.getTotalExecutionTime();
            
            PartitionFinalizationInfo info = new PartitionFinalizationInfo(
                partition.getName(),
                partition.getSize(),
                processNames,
                totalTime
            );
            report.add(info);
        }
        
        report.sort((p1, p2) -> Long.compare(p1.getTotalTime(), p2.getTotalTime()));
        
        return report;
    }

    public static class PartitionFinalizationInfo {
        private String name;
        private long size;
        private String processNames;
        private long totalTime;

        public PartitionFinalizationInfo(String name, long size, String processNames, long totalTime) {
            this.name = name;
            this.size = size;
            this.processNames = processNames;
            this.totalTime = totalTime;
        }

        public String getName() { return name; }
        public long getSize() { return size; }
        public String getProcessNames() { return processNames; }
        public long getTotalTime() { return totalTime; }
    }

    // ========== LIMPIEZA ==========
    
    public void clearAll() {
        initialProcesses.clear();
        
        for (Partition p : partitions) {
            p.clearExecutionData();
        }
        
        partitions.clear();
        executionLogs.clear();
        internalPartitions.clear();
        condensations.clear();
        compactations.clear();
        isFirstCondensation = true;
    }

    public void clearLogs() {
        executionLogs.clear();
    }
}