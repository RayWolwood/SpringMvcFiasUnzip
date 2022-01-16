package com.fedorov.fias.SpringMvcFiasUnzip.model;

public class FileInfo {
    private String name;

    public FileInfo(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
