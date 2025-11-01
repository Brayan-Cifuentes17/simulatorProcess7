package model;

public class Log {
    private String processName;
    private long remainingTime;
    private Status status;
    private long size;
    private Partition partition;
    private Filter filter;
    private int cycleCount;
    private long timestamp;
    private String partitionHistoryString;

    public Log(Process process, Filter filter) {
        this.processName = process.getName();
        this.remainingTime = process.getRemainingTime();
        this.status = process.getStatus();
        this.size = process.getSize();
        this.partition = process.getPartition();
        this.cycleCount = process.getCycleCount();
        this.filter = filter;
        this.timestamp = System.currentTimeMillis();
        this.partitionHistoryString = process.getPartitionHistoryString();
    }

    public String getProcessName() {
        return processName;
    }

    public long getRemainingTime() {
        return remainingTime;
    }

    public Status getStatus() {
        return status;
    }

    public String getStatusString() {
        return status == Status.BLOQUEADO ? "Bloqueado" : "No bloqueado";
    }

    public long getSize() {
        return size;
    }

    public Partition getPartition() {
        return partition;
    }

    // Partición actual (solo la última)
    public String getPartitionName() {
        return partition != null ? partition.getName() : "Sin partición";
    }

    // Historial completo de particiones
    public String getPartitionHistoryString() {
        return partitionHistoryString != null && !partitionHistoryString.isEmpty() 
               ? partitionHistoryString 
               : "Sin partición";
    }

    public Filter getFilter() {
        return filter;
    }

    public int getCycleCount() {
        return cycleCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Log{" +
                "processName='" + processName + '\'' +
                ", remainingTime=" + remainingTime +
                ", status=" + status +
                ", size=" + size +
                ", partition=" + (partition != null ? partition.getName() : "null") +
                ", partitionHistory=" + partitionHistoryString +
                ", filter=" + filter +
                ", cycleCount=" + cycleCount +
                '}';
    }
}