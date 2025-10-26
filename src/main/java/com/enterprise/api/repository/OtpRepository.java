package com.enterprise.api.repository;

import com.enterprise.api.entity.Otp;
import com.enterprise.api.entity.OtpPurpose;
import com.enterprise.api.entity.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OTP entity operations.
 * Provides methods for OTP lifecycle management including creation,
 * validation, and cleanup of expired OTPs.
 */
@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

       /**
        * Find the most recent valid OTP for a given identifier and purpose.
        * 
        * @param identifier the identifier (email, phone, etc.)
        * @param purpose    the purpose of the OTP
        * @return the most recent valid OTP if found
        */
       @Query("SELECT o FROM Otp o WHERE o.identifier = :identifier " +
                     "AND o.purpose = :purpose " +
                     "AND o.used = false " +
                     "AND o.deleted = false " +
                     "AND o.expiresAt > :now " +
                     "AND o.attempts < o.maxAttempts " +
                     "ORDER BY o.createdDate DESC")
       Optional<Otp> findValidOtpByIdentifierAndPurpose(
                     @Param("identifier") String identifier,
                     @Param("purpose") OtpPurpose purpose,
                     @Param("now") LocalDateTime now);

       /**
        * Find all valid OTPs for a given identifier.
        * 
        * @param identifier the identifier
        * @param now        current timestamp
        * @return list of valid OTPs
        */
       @Query("SELECT o FROM Otp o WHERE o.identifier = :identifier " +
                     "AND o.used = false " +
                     "AND o.deleted = false " +
                     "AND o.expiresAt > :now " +
                     "AND o.attempts < o.maxAttempts " +
                     "ORDER BY o.createdDate DESC")
       List<Otp> findValidOtpsByIdentifier(
                     @Param("identifier") String identifier,
                     @Param("now") LocalDateTime now);

       /**
        * Find all OTPs for a given identifier and purpose (including expired/used).
        * 
        * @param identifier the identifier
        * @param purpose    the purpose
        * @return list of OTPs
        */
       List<Otp> findByIdentifierAndPurposeOrderByCreatedDateDesc(String identifier, OtpPurpose purpose);

       /**
        * Find all expired OTPs that haven't been cleaned up.
        * 
        * @param now current timestamp
        * @return list of expired OTPs
        */
       @Query("SELECT o FROM Otp o WHERE o.expiresAt < :now AND o.deleted = false")
       List<Otp> findExpiredOtps(@Param("now") LocalDateTime now);

       /**
        * Count valid OTPs for an identifier within a time window (for rate limiting).
        * 
        * @param identifier the identifier
        * @param since      timestamp to count from
        * @param now        current timestamp
        * @return count of OTPs generated since the given time
        */
       @Query("SELECT COUNT(o) FROM Otp o WHERE o.identifier = :identifier " +
                     "AND o.createdDate >= :since " +
                     "AND o.deleted = false")
       long countOtpsGeneratedSince(
                     @Param("identifier") String identifier,
                     @Param("since") LocalDateTime since);

       /**
        * Count failed attempts for an identifier within a time window.
        * 
        * @param identifier the identifier
        * @param since      timestamp to count from
        * @return count of failed attempts
        */
       @Query("SELECT COALESCE(SUM(o.attempts), 0) FROM Otp o WHERE o.identifier = :identifier " +
                     "AND o.lastModifiedDate >= :since " +
                     "AND o.deleted = false")
       long countFailedAttemptsSince(
                     @Param("identifier") String identifier,
                     @Param("since") LocalDateTime since);

       /**
        * Soft delete expired OTPs.
        * 
        * @param now current timestamp
        * @return number of OTPs marked as deleted
        */
       @Modifying
       @Transactional
       @Query("UPDATE Otp o SET o.deleted = true, o.lastModifiedDate = :now " +
                     "WHERE o.expiresAt < :now AND o.deleted = false")
       int softDeleteExpiredOtps(@Param("now") LocalDateTime now);

       /**
        * Invalidate all valid OTPs for a given identifier and purpose.
        * 
        * @param identifier the identifier
        * @param purpose    the purpose
        * @param now        current timestamp
        * @return number of OTPs invalidated
        */
       @Modifying
       @Transactional
       @Query("UPDATE Otp o SET o.used = true, o.lastModifiedDate = :now " +
                     "WHERE o.identifier = :identifier " +
                     "AND o.purpose = :purpose " +
                     "AND o.used = false " +
                     "AND o.deleted = false " +
                     "AND o.expiresAt > :now " +
                     "AND o.attempts < o.maxAttempts")
       int invalidateOtpsForIdentifierAndPurpose(
                     @Param("identifier") String identifier,
                     @Param("purpose") OtpPurpose purpose,
                     @Param("now") LocalDateTime now);

       /**
        * Find OTPs by type and creation date range.
        * 
        * @param type the OTP type
        * @param from start date
        * @param to   end date
        * @return list of OTPs
        */
       @Query("SELECT o FROM Otp o WHERE o.type = :type " +
                     "AND o.createdDate >= :from " +
                     "AND o.createdDate <= :to " +
                     "AND o.deleted = false " +
                     "ORDER BY o.createdDate DESC")
       List<Otp> findByTypeAndCreatedDateBetween(
                     @Param("type") OtpType type,
                     @Param("from") LocalDateTime from,
                     @Param("to") LocalDateTime to);

       /**
        * Check if an identifier has any valid OTPs.
        * 
        * @param identifier the identifier
        * @param now        current timestamp
        * @return true if valid OTPs exist
        */
       @Query("SELECT COUNT(o) > 0 FROM Otp o WHERE o.identifier = :identifier " +
                     "AND o.used = false " +
                     "AND o.deleted = false " +
                     "AND o.expiresAt > :now " +
                     "AND o.attempts < o.maxAttempts")
       boolean hasValidOtps(@Param("identifier") String identifier, @Param("now") LocalDateTime now);
}