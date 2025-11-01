package model;

public class Compactation {
    private String name;
    private long size;
    private Process process;
    private Partition partitionCreated;
    private boolean isForExpired;

    public Compactation(String name, long size, Process process, Partition partitionCreated, boolean isForExpired) {
        this.name = name;
        this.size = size;
        this.process = process;
        this.partitionCreated = partitionCreated;
        this.isForExpired = isForExpired;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public Process getProcess() {
        return process;
    }

    public Partition getPartitionCreated() {
        return partitionCreated;
    }

    public boolean isForExpired() {
        return isForExpired;
    }

    @Override
    public String toString() {
        String reason = isForExpired ? "Expiración de tiempo" : "Finalización de proceso";
        return name + " - Tamaño: " + size + " - Proceso: " + process.getName() + 
               " - Partición creada: " + partitionCreated.getName() + " - Razón: " + reason;
    }
}