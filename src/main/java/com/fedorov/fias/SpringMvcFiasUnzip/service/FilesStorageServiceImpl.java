package com.fedorov.fias.SpringMvcFiasUnzip.service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class FilesStorageServiceImpl implements FilesStorageService {

    private final Path root = Paths.get("uploads");

    private final AmazonS3 s3Client;

    @Autowired
    public FilesStorageServiceImpl(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void init() {
        try {
            Files.createDirectory(root);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
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
    public void unzipToCloud(MultipartFile file) {

        try {
            ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream());
            ObjectMetadata objectMetadata = new ObjectMetadata();


            for (ZipEntry entry; (entry = zipInputStream.getNextEntry()) != null; ) {
                List<PartETag> partETags = new ArrayList<>();
                String resolvePath = "unzipped/" + entry.getName();
                InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest("fias-fedorov", resolvePath);
                InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);

                long partSize = 5 * 1024 * 1024; // Set part size to 5 MB.
//                long partSize = 4096;
                long filePosition = 0;
                long contentLength = entry.getSize();
                for (int i = 1; filePosition < contentLength; i++) {
                    // Because the last part could be less than 5 MB, adjust the part size as needed.
                    partSize = Math.min(partSize, (contentLength - filePosition));

                    // Create the request to upload a part.
                    UploadPartRequest uploadRequest = new UploadPartRequest()
                            .withBucketName("fias-fedorov")
                            .withKey(resolvePath)
                            .withUploadId(initResponse.getUploadId())
                            .withPartNumber(i)
                            .withFileOffset(filePosition)
//                            .withFile(file)
                            .withInputStream(zipInputStream)
                            .withPartSize(partSize);

                    // Upload the part and add the response's ETag to our list.
                    UploadPartResult uploadResult = s3Client.uploadPart(uploadRequest);
                    partETags.add(uploadResult.getPartETag());

                    filePosition += partSize;
                }

                // Complete the multipart upload.
                CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest("fias-fedorov", resolvePath,
                        initResponse.getUploadId(), partETags);
                s3Client.completeMultipartUpload(compRequest);


            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveUnzip(MultipartFile file) {
        try {
            ZipInputStream inputStream = new ZipInputStream(file.getInputStream());

            ZipEntry entry;
            while (inputStream.getNextEntry() != null){
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
    public Resource load(String filename) {
        try {
            Path file = root.resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(root.toFile());
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.root, 1).filter(path -> !path.equals(this.root)).map(this.root::relativize);
        } catch (IOException e) {
            throw new RuntimeException("Could not load the files!");
        }
    }
}
