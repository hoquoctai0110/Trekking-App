package com.example.trekkingapp.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    @Query("""
            select p
            from CommunityPost p
            join fetch p.author
            where p.status <> :status
            order by p.createdAt desc
            """)
    List<CommunityPost> findActivePosts(String status);

    @Query("""
            select p
            from CommunityPost p
            join fetch p.author
            where p.postId = :postId and p.status <> :status
            """)
    Optional<CommunityPost> findActivePostById(Long postId, String status);
}
