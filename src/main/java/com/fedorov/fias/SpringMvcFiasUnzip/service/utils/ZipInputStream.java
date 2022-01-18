package com.fedorov.fias.SpringMvcFiasUnzip.service.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
public class ZipInputStream extends InputStream {

  private final ZipArchiveInputStream zis;
  private boolean isUnzippedFull = false;

  public ZipInputStream(ZipArchiveInputStream zis) {
    this.zis = zis;
  }

  @Override
  public int read() throws IOException {
    return zis.read();
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return zis.read(b, off, len);
  }

  @Override
  public void close() throws IOException {
    if (isUnzippedFull) {
      zis.close();
    }
  }

  public void forEachEntry(Predicate<String> fileFilter, Consumer<ArchiveEntry> entryConsumer) throws IOException {
    ArchiveEntry entry;
    while ((entry = getNextEntry()) != null) {
      if (fileFilter.test(entry.getName()) && !entry.isDirectory()) {
        entryConsumer.accept(entry);
      }
    }
  }

  private ArchiveEntry getNextEntry() throws IOException {
    ArchiveEntry entry = zis.getNextEntry();
    isUnzippedFull = (entry == null);
    return entry;
  }
}
