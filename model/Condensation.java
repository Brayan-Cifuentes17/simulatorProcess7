package model;

import java.util.ArrayList;

public class Condensation {
    private String name;
    private long size;
    private ArrayList<Partition> partitions;

    public Condensation(String name, Partition... partitionsToAdd){
        this.name = name;
        size = 0;
        addPartitions(partitionsToAdd);
    }
    
    private void addPartitions(Partition[] partitionsToAdd){
        partitions = new ArrayList<>();
        for (int i = 0; i < partitionsToAdd.length; i++) {
            size += partitionsToAdd[i].getSize();
            partitions.add(partitionsToAdd[i]);
        }
    }

    public long getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public ArrayList<Partition> getPartitions() {
        return partitions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" ").append(size).append(" ");
        for (int i = 0; i < partitions.size(); i++) {
            sb.append(partitions.get(i).getName());
            if (i < partitions.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
    
    // Para el reporte detallado
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" = ");
        for (int i = 0; i < partitions.size(); i++) {
            sb.append(partitions.get(i).getName());
            sb.append("(").append(partitions.get(i).getSize()).append(")");
            if (i < partitions.size() - 1) {
                sb.append(" + ");
            }
        }
        sb.append(" = Tamaño total: ").append(size);
        return sb.toString();
    }
}