package com.enterprise.api.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for FileMetadata entity validation and relationships.
 * Tests validation constraints, entity relationships, and business logic.
 */
@DisplayName("FileMetadata Entity Tests")
class FileMetadataTest {

    private Validator validator;
    private FileMetadata fileMetadata;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        fileMetadata = new FileMetadata();
        fileMetadata.setOriginalFileName("test-document.pdf");
        fileMetadata.setStoredFileName("uuid-generated-name.pdf");
        fileMetadata.setContentType("application/pdf");
        fileMetadata.setFileSize(1024L);
        fileMetadata.setFilePath("/uploads/documents/uuid-generated-name.pdf");
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with valid file metadata")
        void shouldPassValidationWithValidData() {
            Set<ConstraintViolation<FileMetadata>> violations = validator.validate(fileMetadata);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when original file name is blank")
        void shouldFailValidationWhenOriginalFileNameIsBlank() {
            fileMetadata.setOriginalFileName("");
            
            Set<ConstraintViolation<FileMetadata>> violations = validator.validate(fileMetadata);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Original file name is required");
        }

        @Test
        @DisplayName("Should fail validation when original file name is too long")
        void shouldFailValidationWhenOriginalFileNameIsTooLong() {
            fileMetadata.setOriginalFileName("a".repeat(256));
            
            Set<ConstraintViolation<FileMetadata>> violations = validator.validate(fileMetadata);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Original file name must not exceed 255 characters");
        }

        @Test
        @DisplayName("Should fail validation when stored file name is blank")
        void shouldFailValidationWhenStoredFileNameIsBlank() {
            fileMetadata.setStoredFileName("");
            
            Set<ConstraintViolation<FileMetadata>> violations = validator.validate(fileMetadata);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Stored file name is required");
        }

        @Test
        @DisplayName("Should fail validation when content type is blank")
        void shouldFailValidationWhenContentTypeIsBlank() {
            fileMetadata.setContentType("");
            
            Set<ConstraintViolation<FileMetadata>> violations = validator.validate(fileMetadata);
            
            assertThat(violations).hasSize(2);
            assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                    "Content type is required",
                    "Content type must be a valid MIME type"
                );
        }

        @Test
        @DisplayName("Should fail validation when content type is invalid")
        void shouldFailValidationWhenContentTypeIsInvalid() {
            fileMetadata.setContentType("invalid-content-type");
            
            Set<ConstraintViolation<FileMetadata>> violations = validator.validate(fileMetadata);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Content type must be a valid MIME type");
        }

        @Test
        @DisplayName("Should pass validation with valid content types")
        void shouldPassValidationWithValidContentTypes() {
            String[] validContentTypes = {
                "application/pdf",
                "image/jpeg",
                "text/plain",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            };
            
            for (String contentType : validContentTypes) {
                fileMetadata.setContentType(contentType);
                Set<ConstraintViolation<FileMetadata>> violations = validator.validate(fileMetadata);
                assertThat(violations).isEmpty();
            }
        }

        @Test
        @DisplayName("Should fail validation when file size is null")
        void shouldFailValidationWhenFileSizeIsNull() {
            fileMetadata.setFileSize(null);
            
            Set<ConstraintViolation<FileMetadata>> violations = validator.validate(fileMetadata);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("File size is required");
        }

        @Test
        @DisplayName("Should fail validation when file size is negative")
        void shouldFailValidationWhenFileSizeIsNegative() {
            fileMetadata.setFileSize(-1L);
            
            Set<ConstraintViolation<FileMetadata>> violations = validator.validate(fileMetadata);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("File size must be non-negative");
        }

        @Test
        @DisplayName("Should fail validation when file size exceeds limit")
        void shouldFailValidationWhenFileSizeExceedsLimit() {
            fileMetadata.setFileSize(104857601L); // 100MB + 1 byte
            
            Set<ConstraintViolation<FileMetadata>> violations = validator.validate(fileMetadata);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("File size must not exceed 100MB");
        }

        @Test
        @DisplayName("Should fail validation when file path is blank")
        void shouldFailValidationWhenFilePathIsBlank() {
            fileMetadata.setFilePath("");
            
            Set<ConstraintViolation<FileMetadata>> violations = validator.validate(fileMetadata);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("File path is required");
        }

        @Test
        @DisplayName("Should fail validation when file path is too long")
        void shouldFailValidationWhenFilePathIsTooLong() {
            fileMetadata.setFilePath("a".repeat(501));
            
            Set<ConstraintViolation<FileMetadata>> violations = validator.validate(fileMetadata);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("File path must not exceed 500 characters");
        }

        @Test
        @DisplayName("Should fail validation when checksum is invalid")
        void shouldFailValidationWhenChecksumIsInvalid() {
            fileMetadata.setChecksum("invalid-checksum-with-special-chars!");
            
            Set<ConstraintViolation<FileMetadata>> violations = validator.validate(fileMetadata);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Checksum must contain only hexadecimal characters");
        }

        @Test
        @DisplayName("Should pass validation with valid checksum")
        void shouldPassValidationWithValidChecksum() {
            fileMetadata.setChecksum("a1b2c3d4e5f6789012345678901234567890abcdef");
            
            Set<ConstraintViolation<FileMetadata>> violations = validator.validate(fileMetadata);
            
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when description is too long")
        void shouldFailValidationWhenDescriptionIsTooLong() {
            fileMetadata.setDescription("a".repeat(256));
            
            Set<ConstraintViolation<FileMetadata>> violations = validator.validate(fileMetadata);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Description must not exceed 255 characters");
        }
    }

    @Nested
    @DisplayName("File Category Tests")
    class FileCategoryTests {

        @Test
        @DisplayName("Should categorize image files correctly")
        void shouldCategorizeImageFilesCorrectly() {
            assertThat(FileMetadata.FileCategory.fromContentType("image/jpeg"))
                .isEqualTo(FileMetadata.FileCategory.IMAGE);
            assertThat(FileMetadata.FileCategory.fromContentType("image/png"))
                .isEqualTo(FileMetadata.FileCategory.IMAGE);
            assertThat(FileMetadata.FileCategory.fromContentType("image/gif"))
                .isEqualTo(FileMetadata.FileCategory.IMAGE);
        }

        @Test
        @DisplayName("Should categorize video files correctly")
        void shouldCategorizeVideoFilesCorrectly() {
            assertThat(FileMetadata.FileCategory.fromContentType("video/mp4"))
                .isEqualTo(FileMetadata.FileCategory.VIDEO);
            assertThat(FileMetadata.FileCategory.fromContentType("video/avi"))
                .isEqualTo(FileMetadata.FileCategory.VIDEO);
        }

        @Test
        @DisplayName("Should categorize audio files correctly")
        void shouldCategorizeAudioFilesCorrectly() {
            assertThat(FileMetadata.FileCategory.fromContentType("audio/mp3"))
                .isEqualTo(FileMetadata.FileCategory.AUDIO);
            assertThat(FileMetadata.FileCategory.fromContentType("audio/wav"))
                .isEqualTo(FileMetadata.FileCategory.AUDIO);
        }

        @Test
        @DisplayName("Should categorize document files correctly")
        void shouldCategorizeDocumentFilesCorrectly() {
            assertThat(FileMetadata.FileCategory.fromContentType("application/pdf"))
                .isEqualTo(FileMetadata.FileCategory.DOCUMENT);
            assertThat(FileMetadata.FileCategory.fromContentType("application/msword"))
                .isEqualTo(FileMetadata.FileCategory.DOCUMENT);
            assertThat(FileMetadata.FileCategory.fromContentType("text/plain"))
                .isEqualTo(FileMetadata.FileCategory.DOCUMENT);
        }

        @Test
        @DisplayName("Should categorize archive files correctly")
        void shouldCategorizeArchiveFilesCorrectly() {
            assertThat(FileMetadata.FileCategory.fromContentType("application/zip"))
                .isEqualTo(FileMetadata.FileCategory.ARCHIVE);
            assertThat(FileMetadata.FileCategory.fromContentType("application/x-rar"))
                .isEqualTo(FileMetadata.FileCategory.ARCHIVE);
        }

        @Test
        @DisplayName("Should categorize unknown files as OTHER")
        void shouldCategorizeUnknownFilesAsOther() {
            assertThat(FileMetadata.FileCategory.fromContentType("application/unknown"))
                .isEqualTo(FileMetadata.FileCategory.OTHER);
            assertThat(FileMetadata.FileCategory.fromContentType(null))
                .isEqualTo(FileMetadata.FileCategory.OTHER);
        }

        @Test
        @DisplayName("Should update category when content type changes")
        void shouldUpdateCategoryWhenContentTypeChanges() {
            fileMetadata.setContentType("image/jpeg");
            assertThat(fileMetadata.getCategory()).isEqualTo(FileMetadata.FileCategory.IMAGE);
            
            fileMetadata.setContentType("application/pdf");
            assertThat(fileMetadata.getCategory()).isEqualTo(FileMetadata.FileCategory.DOCUMENT);
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should increment download count")
        void shouldIncrementDownloadCount() {
            fileMetadata.setDownloadCount(5L);
            fileMetadata.incrementDownloadCount();
            
            assertThat(fileMetadata.getDownloadCount()).isEqualTo(6L);
        }

        @Test
        @DisplayName("Should handle null download count when incrementing")
        void shouldHandleNullDownloadCountWhenIncrementing() {
            fileMetadata.setDownloadCount(null);
            fileMetadata.incrementDownloadCount();
            
            assertThat(fileMetadata.getDownloadCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should extract file extension correctly")
        void shouldExtractFileExtensionCorrectly() {
            fileMetadata.setOriginalFileName("document.pdf");
            assertThat(fileMetadata.getFileExtension()).isEqualTo("pdf");
            
            fileMetadata.setOriginalFileName("image.jpeg");
            assertThat(fileMetadata.getFileExtension()).isEqualTo("jpeg");
            
            fileMetadata.setOriginalFileName("archive.tar.gz");
            assertThat(fileMetadata.getFileExtension()).isEqualTo("gz");
        }

        @Test
        @DisplayName("Should return empty extension for files without extension")
        void shouldReturnEmptyExtensionForFilesWithoutExtension() {
            fileMetadata.setOriginalFileName("README");
            assertThat(fileMetadata.getFileExtension()).isEmpty();
            
            fileMetadata.setOriginalFileName(null);
            assertThat(fileMetadata.getFileExtension()).isEmpty();
        }

        @Test
        @DisplayName("Should format file size correctly")
        void shouldFormatFileSizeCorrectly() {
            fileMetadata.setFileSize(512L);
            assertThat(fileMetadata.getFileSizeFormatted()).isEqualTo("512 B");
            
            fileMetadata.setFileSize(1536L); // 1.5 KB
            assertThat(fileMetadata.getFileSizeFormatted()).isEqualTo("1 KB");
            
            fileMetadata.setFileSize(1572864L); // 1.5 MB
            assertThat(fileMetadata.getFileSizeFormatted()).isEqualTo("1 MB");
        }

        @Test
        @DisplayName("Should handle null file size in formatting")
        void shouldHandleNullFileSizeInFormatting() {
            fileMetadata.setFileSize(null);
            assertThat(fileMetadata.getFileSizeFormatted()).isEqualTo("0 B");
        }

        @Test
        @DisplayName("Should check file type correctly")
        void shouldCheckFileTypeCorrectly() {
            fileMetadata.setContentType("image/jpeg");
            assertThat(fileMetadata.isImage()).isTrue();
            assertThat(fileMetadata.isDocument()).isFalse();
            
            fileMetadata.setContentType("application/pdf");
            assertThat(fileMetadata.isDocument()).isTrue();
            assertThat(fileMetadata.isImage()).isFalse();
        }

        @Test
        @DisplayName("Should have default values")
        void shouldHaveDefaultValues() {
            FileMetadata newFile = new FileMetadata();
            
            assertThat(newFile.getDownloadCount()).isEqualTo(0L);
            assertThat(newFile.isPublicAccess()).isFalse();
            assertThat(newFile.getCategory()).isEqualTo(FileMetadata.FileCategory.OTHER);
            assertThat(newFile.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("User Relationship Tests")
    class UserRelationshipTests {

        @Test
        @DisplayName("Should set uploaded by user")
        void shouldSetUploadedByUser() {
            User user = new User("testuser", "password123", "test@example.com");
            fileMetadata.setUploadedBy(user);
            
            assertThat(fileMetadata.getUploadedBy()).isEqualTo(user);
        }

        @Test
        @DisplayName("Should handle null uploaded by user")
        void shouldHandleNullUploadedByUser() {
            fileMetadata.setUploadedBy(null);
            
            assertThat(fileMetadata.getUploadedBy()).isNull();
        }
    }

    @Nested
    @DisplayName("Equality and Hash Code Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when IDs are the same")
        void shouldBeEqualWhenIdsAreTheSame() {
            FileMetadata file1 = new FileMetadata();
            file1.setId(1L);
            file1.setStoredFileName("file1.pdf");
            
            FileMetadata file2 = new FileMetadata();
            file2.setId(1L);
            file2.setStoredFileName("file2.pdf");
            
            assertThat(file1).isEqualTo(file2);
            assertThat(file1.hashCode()).isEqualTo(file2.hashCode());
        }

        @Test
        @DisplayName("Should be equal when stored file names are the same and IDs are null")
        void shouldBeEqualWhenStoredFileNamesAreTheSameAndIdsAreNull() {
            FileMetadata file1 = new FileMetadata();
            file1.setStoredFileName("same-file.pdf");
            
            FileMetadata file2 = new FileMetadata();
            file2.setStoredFileName("same-file.pdf");
            
            assertThat(file1).isEqualTo(file2);
            assertThat(file1.hashCode()).isEqualTo(file2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when stored file names are different")
        void shouldNotBeEqualWhenStoredFileNamesAreDifferent() {
            FileMetadata file1 = new FileMetadata();
            file1.setStoredFileName("file1.pdf");
            
            FileMetadata file2 = new FileMetadata();
            file2.setStoredFileName("file2.pdf");
            
            assertThat(file1).isNotEqualTo(file2);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create file metadata with default constructor")
        void shouldCreateFileMetadataWithDefaultConstructor() {
            FileMetadata newFile = new FileMetadata();
            
            assertThat(newFile.getOriginalFileName()).isNull();
            assertThat(newFile.getStoredFileName()).isNull();
            assertThat(newFile.getContentType()).isNull();
            assertThat(newFile.getFileSize()).isNull();
            assertThat(newFile.getFilePath()).isNull();
            assertThat(newFile.getCategory()).isEqualTo(FileMetadata.FileCategory.OTHER);
        }

        @Test
        @DisplayName("Should create file metadata with parameterized constructor")
        void shouldCreateFileMetadataWithParameterizedConstructor() {
            FileMetadata newFile = new FileMetadata(
                "test.pdf", "uuid.pdf", "application/pdf", 1024L, "/uploads/uuid.pdf"
            );
            
            assertThat(newFile.getOriginalFileName()).isEqualTo("test.pdf");
            assertThat(newFile.getStoredFileName()).isEqualTo("uuid.pdf");
            assertThat(newFile.getContentType()).isEqualTo("application/pdf");
            assertThat(newFile.getFileSize()).isEqualTo(1024L);
            assertThat(newFile.getFilePath()).isEqualTo("/uploads/uuid.pdf");
            assertThat(newFile.getCategory()).isEqualTo(FileMetadata.FileCategory.DOCUMENT);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should generate meaningful toString")
        void shouldGenerateMeaningfulToString() {
            fileMetadata.setId(1L);
            User user = new User("testuser", "password123", "test@example.com");
            fileMetadata.setUploadedBy(user);
            
            String toString = fileMetadata.toString();
            
            assertThat(toString).contains("FileMetadata{");
            assertThat(toString).contains("id=1");
            assertThat(toString).contains("originalFileName='test-document.pdf'");
            assertThat(toString).contains("storedFileName='uuid-generated-name.pdf'");
            assertThat(toString).contains("contentType='application/pdf'");
            assertThat(toString).contains("uploadedBy=testuser");
        }
    }
}