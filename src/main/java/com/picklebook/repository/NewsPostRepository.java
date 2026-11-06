package com.picklebook.repository;

import com.picklebook.entity.NewsPost;
import com.picklebook.entity.enums.NewsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsPostRepository extends JpaRepository<NewsPost, Long> {

    Optional<NewsPost> findBySlug(String slug);

    Optional<NewsPost> findBySlugAndStatus(String slug, NewsStatus status);

    Page<NewsPost> findByStatusOrderByPublishedAtDesc(NewsStatus status, Pageable pageable);

    Page<NewsPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
