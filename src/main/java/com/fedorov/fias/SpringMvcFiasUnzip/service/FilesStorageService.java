package com.fedorov.fias.SpringMvcFiasUnzip.service;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FilesStorageService {
    void init();

    void save(MultipartFile file);

    void saveUnzip(MultipartFile file);

    void unzipToCloud(MultipartFile file);

    void deleteAll();

    Resource load(String filename);

    Stream<Path> loadAll();
}
