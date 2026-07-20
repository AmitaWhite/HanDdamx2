// 경로: src/test/java/com/white/handdam/storage/S3ObjectStorageTest.java
package com.white.handdam.storage;

import com.white.handdam.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3ObjectStorageTest {

    @Mock private S3Client s3Client;
    @Mock private S3Presigner s3Presigner;

    private S3ObjectStorage storage;

    @BeforeEach
    void setUp() {
        AwsProperties props = new AwsProperties();
        props.setRegion("ap-northeast-2");
        AwsProperties.S3 s3 = new AwsProperties.S3();
        s3.setBucket("handdam-test");
        s3.setEndpoint("http://localhost:4566");  // LocalStack 모드
        props.setS3(s3);

        storage = new S3ObjectStorage(s3Client, props, s3Presigner);
    }

    // ─── 이미지 업로드 ────────────────────────────────────────────────

    @Test
    @DisplayName("[LYJ-031] JPEG 이미지를 업로드하면 StoredObject를 반환한다")
    void upload_jpeg_success() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "photo.jpg", "image/jpeg", "fake-image".getBytes()
        );
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .willReturn(null);

        StoredObject result = storage.upload("feeds/1/attachments", file);

        assertThat(result.storageKey()).matches("feeds/1/attachments/[0-9a-f\\-]{36}\\.jpg");
        assertThat(result.originalName()).isEqualTo("photo.jpg");
        assertThat(result.url()).contains("handdam-test");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    // ─── PDF 업로드 ───────────────────────────────────────────────────

    @Test
    @DisplayName("[LYJ-031] PDF 파일을 업로드하면 StoredObject를 반환한다")
    void upload_pdf_success() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "manual.pdf", "application/pdf", "fake-pdf".getBytes()
        );
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .willReturn(null);

        StoredObject result = storage.upload("feeds/1/attachments", file);

        assertThat(result.storageKey()).matches("feeds/1/attachments/[0-9a-f\\-]{36}\\.pdf");
        assertThat(result.originalName()).isEqualTo("manual.pdf");
    }

    // ─── 영상 업로드 ──────────────────────────────────────────────────

    @Test
    @DisplayName("[LYJ-031] MP4 영상 파일을 업로드하면 StoredObject를 반환한다")
    void upload_mp4_success() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "tutorial.mp4", "video/mp4", "fake-video".getBytes()
        );
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .willReturn(null);

        StoredObject result = storage.upload("feeds/1/attachments", file);

        assertThat(result.storageKey()).matches("feeds/1/attachments/[0-9a-f\\-]{36}\\.mp4");
    }

    // ─── 허용되지 않는 타입 ───────────────────────────────────────────

    @Test
    @DisplayName("[LYJ-031] 허용되지 않는 파일 형식 업로드 시 CustomException 발생")
    void upload_unsupportedType_throws() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "script.exe", "application/octet-stream", "malware".getBytes()
        );

        assertThatThrownBy(() -> storage.upload("feeds/1/attachments", file))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("[LYJ-031] 빈 파일 업로드 시 CustomException 발생")
    void upload_emptyFile_throws() {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", "empty.jpg", "image/jpeg", new byte[0]
        );

        assertThatThrownBy(() -> storage.upload("feeds/1/attachments", emptyFile))
            .isInstanceOf(CustomException.class);
    }

    // ─── 삭제 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("[LYJ-031] 유효한 storageKey로 삭제하면 S3 deleteObject가 호출된다")
    void delete_success() {
        given(s3Client.deleteObject(any(DeleteObjectRequest.class))).willReturn(null);

        storage.delete("feeds/1/attachments/uuid_photo.jpg");

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("[LYJ-031] storageKey가 null이면 삭제 시 CustomException 발생")
    void delete_nullKey_throws() {
        assertThatThrownBy(() -> storage.delete(null))
            .isInstanceOf(CustomException.class);
    }

    // ─── Presigned URL 생성 ───────────────────────────────────────────

    @Test
    @DisplayName("[LYJ-031] generatePresignedUrl은 S3Presigner를 호출하고 URL 문자열을 반환한다")
    void generatePresignedUrl_success() throws Exception {
        PresignedGetObjectRequest presignedRequest =
            org.mockito.Mockito.mock(PresignedGetObjectRequest.class);
        given(presignedRequest.url())
            .willReturn(new URL("https://handdam-test.s3.ap-northeast-2.amazonaws.com/feeds/1/uuid_manual.pdf?X-Amz-Signature=abc"));
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
            .willReturn(presignedRequest);

        String url = storage.generatePresignedUrl("feeds/1/uuid_manual.pdf", 10);

        assertThat(url).contains("X-Amz-Signature");
        assertThat(url).contains("feeds/1/uuid_manual.pdf");
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }
}
