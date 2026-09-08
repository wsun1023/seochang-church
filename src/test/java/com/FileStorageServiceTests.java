package com;

import com.seochang.church.service.FileStorageService;
import com.seochang.church.entity.BoardAttachment;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class FileStorageServiceTests {
    @TempDir Path directory;
    FileStorageService storage;
    @BeforeEach void setup() {
        storage = new FileStorageService();
        ReflectionTestUtils.setField(storage, "uploadDir", directory.toString());
    }
    @AfterEach void cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }
    @Test void rejectsHtmlAndFakeImages() {
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", "bad.html", "text/html", "<script>alert(1)</script>".getBytes()), "board")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("image", "fake.jpg", "image/jpeg", new byte[]{1,2}), "editor")).isInstanceOf(ResponseStatusException.class);
    }
    @Test void preventsDeletionOutsideStorage() {
        assertThatThrownBy(() -> storage.deleteFile("../outside.txt")).isInstanceOf(ResponseStatusException.class);
    }
    @Test void validatesCountBeforeDeletingExistingFile() throws Exception {
        Path original = Files.writeString(directory.resolve("original.txt"), "keep");
        BoardAttachment attachment = new BoardAttachment();
        attachment.setId(1L); attachment.setStoredFileName("original.txt"); attachment.setImage(false);
        var upload = new MockMultipartFile("file", "new.txt", "text/plain", "new".getBytes());
        assertThatThrownBy(() -> storage.validatePlan(List.of(attachment), List.of(), null, List.of(upload), 0, 1)).isInstanceOf(ResponseStatusException.class);
        assertThat(original).hasContent("keep");
    }
    @Test void newFileIsRemovedOnRollback() {
        TransactionSynchronizationManager.initSynchronization();
        String name = storage.store(new MockMultipartFile("file", "new.txt", "text/plain", "new".getBytes()), "board");
        assertThat(directory.resolve(name)).exists();
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertThat(directory.resolve(name)).doesNotExist();
    }
    @Test void existingFileIsDeletedOnlyAfterCommit() throws Exception {
        Path original = Files.writeString(directory.resolve("original.txt"), "keep");
        TransactionSynchronizationManager.initSynchronization();
        storage.deleteFile("original.txt");
        assertThat(original).exists();
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(original).doesNotExist();
    }
    @Test void realImageIsReencodedAsJpeg() throws Exception {
        var buffer = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_ARGB), "png", buffer);
        String name = storage.store(new MockMultipartFile("image", "a.png", "image/png", buffer.toByteArray()), "editor");
        byte[] bytes = Files.readAllBytes(directory.resolve(name));
        assertThat(bytes[0]).isEqualTo((byte) 0xff);
        assertThat(bytes[1]).isEqualTo((byte) 0xd8);
    }
}
