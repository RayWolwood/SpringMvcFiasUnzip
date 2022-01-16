package com.fedorov.fias.SpringMvcFiasUnzip.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fedorov.fias.SpringMvcFiasUnzip.message.ResponseMessage;
import com.fedorov.fias.SpringMvcFiasUnzip.model.FileInfo;
import com.fedorov.fias.SpringMvcFiasUnzip.service.FilesStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Controller
@RequestMapping("/service")
public class FilesController {

    private final FilesStorageService storageService;

    @Autowired
    public FilesController(FilesStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ResponseMessage> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        String message;
        try {
            List<String> fileNames = new ArrayList<>();

            Arrays.stream(files).forEach(file -> {
                log.info("Начата загрузка файла {}", file.getOriginalFilename());
                storageService.save(file);
                fileNames.add(file.getOriginalFilename());
            });

            message = "Файлы были загружены: " + fileNames;
            log.info(message);
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
        } catch (Exception e) {
            message = "Ошибка загрузки файлов!";
            log.info(message);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
        }
    }

    @PostMapping("/unzip")
    public ResponseEntity<ResponseMessage> unzip(@RequestParam("file") MultipartFile file) {
        long startTime = System.currentTimeMillis();
        String message;
        try {
            log.info("Начата загрузка файла {}", file.getOriginalFilename());
            storageService.saveUnzip(file);
            long duration = System.currentTimeMillis() - startTime;
            message = "Файл был загружен и распакован: " + file.getOriginalFilename() + " за " + duration + " мс";
            log.info(message);
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
        } catch (Exception e) {
            message = "Ошибка загрузки файла!";
            log.info(message);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
        }
    }

    @GetMapping("/files")
    public ResponseEntity<List<FileInfo>> getListFiles() {
        List<FileInfo> fileInfos = storageService.loadAll().map(path -> {
            String filename = path.getFileName().toString();
            return new FileInfo(filename);
        }).collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(fileInfos);
    }

}
