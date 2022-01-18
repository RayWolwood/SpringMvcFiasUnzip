package com.fedorov.fias.SpringMvcFiasUnzip.service;

import org.springframework.web.multipart.MultipartFile;

public interface FilesStorageService {

    void save(MultipartFile file);

    void saveUnzip(MultipartFile file);

    void unzipToCloud(MultipartFile file);

}
