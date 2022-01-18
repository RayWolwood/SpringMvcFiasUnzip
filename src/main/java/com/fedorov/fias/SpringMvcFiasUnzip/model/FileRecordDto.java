package com.fedorov.fias.SpringMvcFiasUnzip.model;

import lombok.Data;

@Data
public class FileRecordDto {

  private String fileName;
  private long fileSize;

  public FileRecordDto(String fileName, long fileSize) {
    this.fileName = fileName;
    this.fileSize = fileSize;
  }

}
