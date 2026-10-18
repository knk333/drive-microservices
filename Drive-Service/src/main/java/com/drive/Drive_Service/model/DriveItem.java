package com.drive.Drive_Service.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
public class DriveItem {
    public enum Kind{FILE,FOLDER}

   public enum Status{PENDING,READY,DELETING}
    @Id
    private UUID id;
    @Column(nullable = false)
    private String name;
    private UUID parentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    private long size;
    private String mediaType;
    @Column(nullable = false)
    private Instant createdAt;
    @Version
    private long version;
    public DriveItem(){}

 public Instant getCreatedAt() {
  return createdAt;
 }

 public void setCreatedAt(Instant createdAt) {
  this.createdAt = createdAt;
 }

 public UUID getId() {
  return id;
 }

 public void setId(UUID id) {
  this.id = id;
 }

 public Kind getKind() {
  return kind;
 }

 public void setKind(Kind kind) {
  this.kind = kind;
 }

 public String getMediaType() {
  return mediaType;
 }

 public void setMediaType(String mediaType) {
  this.mediaType = mediaType;
 }

 public String getName() {
  return name;
 }

 public void setName(String name) {
  this.name = name;
 }

 public UUID getParentId() {
  return parentId;
 }

 public void setParentId(UUID parentId) {
  this.parentId = parentId;
 }

 public long getSize() {
  return size;
 }

 public void setSize(long size) {
  this.size = size;
 }

 public Status getStatus() {
  return status;
 }

 public void setStatus(Status status) {
  this.status = status;
 }

 public long getVersion() {
  return version;
 }

 public void setVersion(long version) {
  this.version = version;
 }

 public DriveItem(String name, UUID parentId, Kind kind, long size, String mediaType){
     this.id=UUID.randomUUID();
     this.name=name;
     this.parentId=parentId;
     this.kind=kind;
     this.size=size;
     this.mediaType=mediaType;
     this.status=kind==Kind.FOLDER?Status.READY:Status.PENDING;
     this.createdAt=Instant.now();

    }


}
