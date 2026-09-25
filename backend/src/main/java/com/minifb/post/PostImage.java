package com.minifb.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "post_images")
public class PostImage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "post_id", nullable = false)
  private Long postId;

  @Column(name = "storage_key", nullable = false)
  private String storageKey;

  @Column(name = "thumb_key", nullable = false)
  private String thumbKey;

  @Column(nullable = false, columnDefinition = "smallint unsigned")
  private int width;

  @Column(nullable = false, columnDefinition = "smallint unsigned")
  private int height;

  @Column(nullable = false, columnDefinition = "tinyint")
  private int position;

  protected PostImage() {}

  public PostImage(
      long postId, String storageKey, String thumbKey, int width, int height, int position) {
    this.postId = postId;
    this.storageKey = storageKey;
    this.thumbKey = thumbKey;
    this.width = width;
    this.height = height;
    this.position = position;
  }

  public Long getPostId() {
    return postId;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getThumbKey() {
    return thumbKey;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int getPosition() {
    return position;
  }
}
