package com.example.trekkingapp.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityPostImageRepository extends JpaRepository<CommunityPostImage, Long> {

    List<CommunityPostImage> findByPost_PostIdOrderByDisplayOrderAscImageIdAsc(Long postId);
}
