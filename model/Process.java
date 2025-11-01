package model;

import java.util.ArrayList;

public class Process {
    private String name;
    private long originalTime;
    private long remainingTime;
    private Status status; 
    private long size;
    private Partition partition;
    private int cycleCount;
    private ArrayList<Partition> blockedPartitions;
    private int lastPartitionIndex;
    private ArrayList<Partition> partitionHistory;  // ← NUEVO: Historial de particiones asignadas

    // Constructor principal
    public Process(String name, long time, Status status, long size, Partition partition) {
        this.name = name;
        this.originalTime = time;
        this.remainingTime = time;
        this.status = status;
        this.size = size;
        this.partition = partition;
        this.cycleCount = 0;
        this.blockedPartitions = new ArrayList<>();
        this.lastPartitionIndex = -1;
        this.partitionHistory = new ArrayList<>();  // ← INICIALIZAR
    }

    // Constructor con todos los parámetros
    public Process(String name, long originalTime, long remainingTime, Status status, 
                   long size, Partition partition, int cycleCount) {
        this.name = name;
        this.originalTime = originalTime;
        this.remainingTime = remainingTime;
        this.status = status;
        this.size = size;
        this.partition = partition;
        this.cycleCount = cycleCount;
        this.blockedPartitions = new ArrayList<>();
        this.lastPartitionIndex = -1;
        this.partitionHistory = new ArrayList<>();  // ← INICIALIZAR
    }

    // Constructor sin partición
    public Process(String name, long time, Status status, long size) {
        this.name = name;
        this.originalTime = time;
        this.remainingTime = time;
        this.status = status;
        this.size = size;
        this.partition = null;
        this.cycleCount = 0;
        this.blockedPartitions = new ArrayList<>();
        this.lastPartitionIndex = -1;
        this.partitionHistory = new ArrayList<>();  // ← INICIALIZAR
    }

    public void subtractTime(long time) {
        this.remainingTime -= time;
        if (remainingTime < 0) {
            remainingTime = 0;
        }
    }

    public void incrementCycle() {
        this.cycleCount++;
    }

    public boolean isFinished() {
        return remainingTime <= 0;
    }

    public boolean isBlocked() {
        return status == Status.BLOQUEADO;
    }

    public boolean fitsInPartition() {
        return partition != null && size <= partition.getSize();
    }

    public void resetTime() {
        remainingTime = originalTime;
    }

    // Métodos para particiones bloqueadas
    public void addBlockedPartition(Partition partition) {
        if (!blockedPartitions.contains(partition)) {
            blockedPartitions.add(partition);
        }
    }

    public int getQuantityBlockedPartitions() {
        return blockedPartitions.size();
    }

    public int getLastPartitionIndex() {
        return lastPartitionIndex;
    }

    public void setLastPartitionIndex(int index) {
        this.lastPartitionIndex = index;
    }

    public void clearBlockedPartitions() {
        blockedPartitions.clear();
    }

    // ← NUEVO: Métodos para historial de particiones
    public void addToPartitionHistory(Partition partition) {
        if (partition != null && !partitionHistory.contains(partition)) {
            partitionHistory.add(partition);
        }
    }

    public ArrayList<Partition> getPartitionHistory() {
        return new ArrayList<>(partitionHistory);
    }

    public String getPartitionHistoryString() {
        if (partitionHistory.isEmpty()) {
            return "Sin partición";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partitionHistory.size(); i++) {
            sb.append(partitionHistory.get(i).getName());
            if (i < partitionHistory.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    // Getters existentes
    public String getName() {
        return name;
    }

    public long getOriginalTime() {
        return originalTime;
    }

    public long getRemainingTime() {
        return remainingTime;
    }

    public Status getStatus() {
        return status;
    }

    public long getSize() {
        return size;
    }

    public Partition getPartition() {
        return partition;
    }

    public int getCycleCount() {
        return cycleCount;
    }

    public String getStatusString() {
        return status == Status.BLOQUEADO ? "Bloqueado" : "No bloqueado";
    }

    // Setters existentes
    public void setName(String name) {
        this.name = name;
    }

    public void setOriginalTime(long originalTime) {
        this.originalTime = originalTime;
        this.remainingTime = originalTime;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
    }

    public void setCycleCount(int cycleCount) {
        this.cycleCount = cycleCount;
    }

    // Actualizar el método clone
    public Process clone() {
        Process cloned = new Process(name, originalTime, remainingTime, status, size, partition, cycleCount);
        cloned.blockedPartitions = new ArrayList<>(this.blockedPartitions);
        cloned.lastPartitionIndex = this.lastPartitionIndex;
        cloned.partitionHistory = new ArrayList<>(this.partitionHistory);  // ← Clonar historial
        return cloned;
    }

    @Override
    public String toString() {
        return "Process{" +
                "name='" + name + '\'' +
                ", originalTime=" + originalTime +
                ", remainingTime=" + remainingTime +
                ", status=" + status +
                ", size=" + size +
                ", partition=" + (partition != null ? partition.getName() : "null") +
                ", cycleCount=" + cycleCount +
                ", lastPartitionIndex=" + lastPartitionIndex +
                ", partitionHistory=" + getPartitionHistoryString() +
                '}';
    }
}