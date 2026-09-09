package com.seochang.church;

import com.seochang.church.entity.*;
import com.seochang.church.repository.*;
import com.seochang.church.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = SeochangChurchApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:gallery;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS seochang_church_db",
        "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@Transactional
class GalleryIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired GalleryRepository galleries;
    @Autowired GalleryPhotoService photos;
    @Autowired EntityManager em;
    @MockBean FileStorageService storage;

    @BeforeEach void configureStorage() {
        when(storage.store(any(), eq("gallery"))).thenAnswer(call -> "gallery/img/" + UUID.randomUUID() + ".jpg");
        doAnswer(call -> {
            List<Long> ids = call.getArgument(0);
            java.util.Collection<GalleryAttachment> attachments = call.getArgument(1);
            if (ids != null) attachments.removeIf(photo -> ids.contains(photo.getId()));
            return null;
        }).when(storage).deleteAttachments(any(), anyCollection());
    }
    private User user(String name, String role) {
        User user = new User(name, "unused", name, null, null, role); user.setApproved(true);
        return users.saveAndFlush(user);
    }
    private MockHttpSession session(User user) {
        MockHttpSession session = new MockHttpSession(); session.setAttribute("loginUser", user);
        session.setAttribute("csrfToken", "gallery-token"); return session;
    }
    private Gallery gallery(User owner, int count) {
        Gallery gallery = new Gallery("행사 사진", "사진 이야기", owner.getName(), owner.getId());
        for (int i = 0; i < count; i++) {
            GalleryAttachment photo = new GalleryAttachment(); photo.setGallery(gallery); photo.setImage(true);
            photo.setOriginalFileName("photo-" + i + ".jpg"); photo.setStoredFileName(UUID.randomUUID() + ".jpg");
            photo.setFilePath("/uploads/" + photo.getStoredFileName()); photo.setFileSize(1L);
            gallery.getAttachments().add(photo);
        }
        return galleries.saveAndFlush(gallery);
    }
    private MockMultipartFile upload(String name) { return new MockMultipartFile("imageFiles", name, "image/jpeg", new byte[]{1}); }
    private String key(GalleryAttachment photo) { return "existing:" + photo.getId(); }

    @Test void creatingGalleryPersistsChosenCoverAndPhotoOrder() throws Exception {
        User admin = user("admin", "ADMIN");
        mvc.perform(multipart("/gallery/new").file(upload("a.jpg")).file(upload("b.jpg"))
                .session(session(admin)).param("_csrf", "gallery-token").param("title", "새 사진첩").param("content", "내용")
                .param("photoOrder", "new:1", "new:0").param("coverPhoto", "new:0"))
                .andExpect(status().is3xxRedirection());
        em.flush(); em.clear();
        Gallery gallery = galleries.findAll().get(0);
        assertThat(gallery.getPhotos()).extracting(GalleryAttachment::getOriginalFileName).containsExactly("b.jpg", "a.jpg");
        assertThat(gallery.getCoverPhoto().getOriginalFileName()).isEqualTo("a.jpg");
        verify(storage).validatePlan(anyCollection(), isNull(), anyList(), isNull(), eq(50), eq(3));
    }

    @Test void editMixesExistingAndNewPhotosAndRemovesOnlySelectedAttachment() throws Exception {
        User admin = user("admin", "ADMIN"); Gallery gallery = gallery(admin, 3);
        GalleryAttachment first = gallery.getPhotos().get(0), removed = gallery.getPhotos().get(1), last = gallery.getPhotos().get(2);
        mvc.perform(multipart("/gallery/" + gallery.getId() + "/edit").file(upload("new.jpg"))
                .session(session(admin)).param("_csrf", "gallery-token").param("title", "수정").param("content", "내용")
                .param("deleteFileIds", removed.getId().toString())
                .param("photoOrder", key(last), "new:0", key(first)).param("coverPhoto", "new:0"))
                .andExpect(redirectedUrl("/gallery/" + gallery.getId()));
        verify(storage).deleteAttachments(eq(List.of(removed.getId())), same(gallery.getAttachments()));
        Long galleryId = gallery.getId();
        em.flush(); em.clear();
        Gallery saved = galleries.findById(galleryId).orElseThrow();
        assertThat(saved.getPhotos()).extracting(GalleryAttachment::getOriginalFileName)
                .containsExactly(last.getOriginalFileName(), "new.jpg", first.getOriginalFileName());
        assertThat(em.find(GalleryAttachment.class, removed.getId())).isNull();
        assertThat(saved.getPhotos().stream().filter(GalleryAttachment::isCoverPhoto)).singleElement()
                .extracting(GalleryAttachment::getOriginalFileName).isEqualTo("new.jpg");
    }

    @Test void reorderingSurvivesReloadAndCoverIsIndependent() {
        User admin = user("admin", "ADMIN"); Gallery gallery = gallery(admin, 3);
        List<GalleryAttachment> original = gallery.getPhotos();
        photos.update(gallery, null, null, List.of(key(original.get(2)), key(original.get(0)), key(original.get(1))), key(original.get(1)));
        Long id = gallery.getId(); em.flush(); em.clear();
        Gallery loaded = galleries.findById(id).orElseThrow();
        assertThat(loaded.getPhotos()).extracting(GalleryAttachment::getId)
                .containsExactly(original.get(2).getId(), original.get(0).getId(), original.get(1).getId());
        assertThat(loaded.getCoverPhoto().getId()).isEqualTo(original.get(1).getId());
    }

    @Test void foreignPhotoReferencesAreRejectedBeforeFileOperations() {
        User admin = user("admin", "ADMIN"); Gallery gallery = gallery(admin, 1); Gallery other = gallery(admin, 1);
        assertThatThrownBy(() -> photos.update(gallery, null, List.of(other.getPhotos().get(0).getId()), null, null))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> photos.update(gallery, null, null, List.of(key(other.getPhotos().get(0))), null))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> photos.update(gallery, null, null, null, key(other.getPhotos().get(0))))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(storage);
    }

    @Test void missingAndDuplicateOrderEntriesAreRejected() {
        Gallery gallery = gallery(user("admin", "ADMIN"), 2); String key = key(gallery.getPhotos().get(0));
        assertThatThrownBy(() -> photos.update(gallery, null, null, List.of(key), null)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> photos.update(gallery, null, null, List.of(key, key), null)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(storage);
    }

    @Test void cannotDeleteLastPhotoOrExceedFifty() {
        User admin = user("admin", "ADMIN"); Gallery one = gallery(admin, 1); Gallery full = gallery(admin, 50);
        assertThatThrownBy(() -> photos.update(one, null, List.of(one.getPhotos().get(0).getId()), null, null))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> photos.update(full, List.of(upload("extra.jpg")), null, null, null))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(storage);
    }

    @Test void deletingCoverChoosesFirstRemainingPhoto() {
        Gallery gallery = gallery(user("admin", "ADMIN"), 2);
        GalleryAttachment removed = gallery.getPhotos().get(0), remaining = gallery.getPhotos().get(1);
        removed.setCoverPhoto(true);
        photos.update(gallery, null, List.of(removed.getId()), null, null);
        assertThat(remaining.isCoverPhoto()).isTrue();
    }

    @Test void existingGalleriesWithoutCoverStillShowFirstPhoto() {
        Gallery gallery = gallery(user("admin", "ADMIN"), 2);
        assertThat(gallery.getCoverPhoto().getId()).isEqualTo(gallery.getPhotos().get(0).getId());
    }

    @Test void ordinaryMemberCannotManagePhotos() throws Exception {
        User member = user("member", "USER"); Gallery gallery = gallery(member, 2);
        mvc.perform(get("/gallery/" + gallery.getId() + "/edit").session(session(member)))
                .andExpect(redirectedUrl("/?error=admin-only"));
        mvc.perform(multipart("/gallery/" + gallery.getId() + "/edit").session(session(member))
                .param("_csrf", "gallery-token").param("title", "조작").param("content", "내용"))
                .andExpect(redirectedUrl("/?error=admin-only"));
        verifyNoInteractions(storage);
    }

    @Test void createWithoutPhotosReturnsUsefulValidationError() throws Exception {
        User admin = user("admin", "ADMIN");
        mvc.perform(multipart("/gallery/new").session(session(admin)).param("_csrf", "gallery-token")
                .param("title", "빈 사진첩").param("content", "내용"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("사진은 최소 1장, 최대 50장까지 등록할 수 있습니다."));
        assertThat(galleries.count()).isZero();
        verifyNoInteractions(storage);
    }

    @Test void selectedCoverAppearsOnPublicAndAdminListsAndViewerHasOrderedPhotos() throws Exception {
        User admin = user("admin", "ADMIN"); Gallery gallery = gallery(admin, 2);
        GalleryAttachment cover = gallery.getPhotos().get(1); cover.setCoverPhoto(true);
        for (String path : List.of("/gallery", "/", "/admin/galleries")) {
            mvc.perform(get(path).session(session(admin))).andExpect(status().isOk())
                    .andExpect(content().string(containsString(cover.getFilePath())));
        }
        mvc.perform(get("/gallery/" + gallery.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("gallery-viewer.js")))
                .andExpect(content().string(containsString("toastui-editor-viewer.min.js")))
                .andExpect(content().string(containsString("galleryStoryViewer")))
                .andExpect(content().string(containsString("lightboxCounter")));
        mvc.perform(get("/gallery/" + gallery.getId() + "/edit").session(session(admin))).andExpect(status().isOk())
                .andExpect(content().string(containsString("toastui-editor-all.min.js")))
                .andExpect(content().string(containsString("galleryStoryEditor")))
                .andExpect(content().string(containsString("data-cover-key=\"" + key(cover) + "\"")));
    }
}
