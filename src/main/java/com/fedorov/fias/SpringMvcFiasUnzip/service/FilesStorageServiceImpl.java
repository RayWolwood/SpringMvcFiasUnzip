package com.fedorov.fias.SpringMvcFiasUnzip.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.fedorov.fias.SpringMvcFiasUnzip.service.utils.ZipInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class FilesStorageServiceImpl implements FilesStorageService {

    private final Path root = Paths.get("uploads");

    @Value("${cloud.bucket}")
    private String bucket;
    private final AmazonS3 s3Client;

    private int countRetry = 10;

    private final Predicate<String> fileFilter;

    @Autowired
    public FilesStorageServiceImpl(AmazonS3 s3Client,
                                   @Value("${unzip.unzip-ignore-patterns}") List<String> ignorePatterns) {
        this.s3Client = s3Client;
        this.fileFilter = getFilter(ignorePatterns);
    }

    @Override
    public void save(MultipartFile file) {
        try {
            Files.copy(file.getInputStream(), this.root.resolve(Objects.requireNonNull(file.getOriginalFilename())));
        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    @Override
    public void saveUnzip(MultipartFile file) {
        try {
            java.util.zip.ZipInputStream inputStream = new java.util.zip.ZipInputStream(file.getInputStream());

            ZipEntry entry;
            while (inputStream.getNextEntry() != null) {
                entry = inputStream.getNextEntry();
                Path resolvedPath = this.root.resolve(entry.getName());
                if (!entry.isDirectory()) {
                    Files.createDirectories(resolvedPath.getParent());
                    Files.copy(inputStream, resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unzipToCloud(MultipartFile file) {

        try (ZipInputStream zipInputStream = new ZipInputStream(new ZipArchiveInputStream(file.getInputStream()))) {
            zipInputStream.forEachEntry(fileFilter, entry -> lowLevelMultipartUpload(entry, zipInputStream));
        } catch (Exception e) {
            log.error("Unzip error: ", e);
        }

//        try {
//            java.util.zip.ZipInputStream zipInputStream = new java.util.zip.ZipInputStream(file.getInputStream());
//            for (ZipEntry entry; (entry = zipInputStream.getNextEntry()) != null; ) {
//                if (entry.getSize() != 0) {
//                    lowLevelMultipartUpload(entry, zipInputStream);
//                }
//            }
//            zipInputStream.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void lowLevelMultipartUpload(ArchiveEntry entry, ZipInputStream zipInputStream) {
        long partSize = 5L * 1024 * 1024; // Set part size to -> x MB.
        long contentLength = entry.getSize();
        long partCount = calculatePartCount(partSize, contentLength);

        List<PartETag> partETags = new ArrayList<>();
        String resolvePath = "unzipped2/" + entry.getName();
        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucket, resolvePath);
        InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);

        long filePosition = 0;

        for (int i = 1; filePosition < contentLength; i++) {
            partSize = Math.min(partSize, (contentLength - filePosition));

            UploadPartRequest uploadRequest = new UploadPartRequest()
                    .withBucketName(bucket)
                    .withKey(resolvePath)
                    .withUploadId(initResponse.getUploadId())
                    .withPartNumber(i)
                    .withInputStream(zipInputStream)
                    .withPartSize(partSize);

            uploadRequest.getRequestClientOptions().setReadLimit(10_000_000);

            while (countRetry != 0) {
                try {
                    UploadPartResult uploadResult = s3Client.uploadPart(uploadRequest);
                    partETags.add(uploadResult.getPartETag());
                    log.info("Загрузка файла {}, часть ({} из {})", entry.getName(), i, partCount);
                    filePosition += partSize;
                    break;
                } catch (Exception e) {
                    countRetry--;
                    log.error("Ошибка отправки части файла, осталось попыток: {}", countRetry);
                    e.printStackTrace();
                }
            }
        }

        CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucket, resolvePath,
                initResponse.getUploadId(), partETags);

        s3Client.completeMultipartUpload(compRequest);
    }

    private long calculatePartCount(long partSize, long contentLength) {
        double d = ((double) contentLength) / ((double) partSize);
        double result = Math.ceil(d);
        return (long) result < 0 ? 1 : (long) result;
    }

    private Predicate<String> getFilter(List<String> ignorePatterns) {
        List<Pattern> ignorePatternsList = ignorePatterns.stream()
                .map(fileName -> Pattern.compile(".*" + fileName + ".*"))
                .collect(Collectors.toList());
        return fileName -> ignorePatternsList.stream().noneMatch(pattern -> pattern.matcher(fileName).matches());
    }
}
