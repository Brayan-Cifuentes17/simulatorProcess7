package model;

public enum Filter {
    INICIAL("Procesos Iniciales", "Inicial"),
    LISTO("Informe de procesos listos", "Listo"),
    DESPACHAR("Informe de procesos despachados", "Despachar"),
    EN_EJECUCION("Informe de procesos en ejecución", "En Ejecución"),
    TIEMPO_EXPIRADO("Informe de procesos con tiempo expirado", "Expiración de Tiempo"),
    TRANSICION_BLOQUEO("Informe de transición de bloqueo", "Espera de E/S"),
    BLOQUEADO("Informe de procesos bloqueados", "Bloqueado"),  
    DESPERTAR("Informe de procesos despertados", "Terminación de operación E/S"),
    FINALIZADO("Informe de procesos terminados", "Salidas"),
    PARTICIONES("Informe de particiones", "Particiones"),
    FINALIZACION_PARTICIONES("Informe de finalización de particiones", "Finalización de Particiones"),
    NO_EJECUTADO("Informe de procesos no ejecutados", "No Ejecutados"),
    EJECUCION_PROCESOS("Informe de ejecución de procesos", "Ejecución de Procesos"),
    CONDENSACIONES("Informe de condensaciones", "Condensaciones"),
    COMPACTACIONES("Informe de compactaciones", "Compactaciones"); 
        
    private String description;
    private String name;

    Filter(String description, String name) {
        this.description = description;
        this.name = name;
    }

    public String getDescription() {
        return description;
    }    

    public String getName() {
        return name;
    }  
}