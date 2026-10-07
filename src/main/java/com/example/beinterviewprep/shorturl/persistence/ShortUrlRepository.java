package com.example.beinterviewprep.shorturl.persistence;

import com.example.beinterviewprep.shorturl.domain.ShortUrl;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

  Optional<ShortUrl> findByCode(String code);

  boolean existsByCode(String code);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("update ShortUrl s set s.visitCount = s.visitCount + 1 where s.code = :code")
  int incrementVisitCount(@Param("code") String code);
}
