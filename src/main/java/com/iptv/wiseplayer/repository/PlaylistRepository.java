package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {
    java.util.List<Playlist> findByDeviceId(UUID deviceId);

    java.util.List<Playlist> findByDeviceIdOrderByPinnedDescCreatedAtDesc(UUID deviceId);

    Optional<Playlist> findByDeviceIdAndPinnedTrue(UUID deviceId);

    void deleteAllByDeviceId(UUID deviceId);

    void deleteByDeviceIdAndName(UUID deviceId, String name);

    Optional<Playlist> findByIdAndDeviceId(UUID id, UUID deviceId);

    // Owner-based queries
    java.util.List<Playlist> findByOwnerIdAndOwnerType(UUID ownerId, com.iptv.wiseplayer.domain.enums.OwnerType ownerType);

    java.util.List<Playlist> findByOwnerIdAndOwnerTypeOrderByCreatedAtDesc(UUID ownerId, com.iptv.wiseplayer.domain.enums.OwnerType ownerType);

    Optional<Playlist> findByIdAndOwnerIdAndOwnerType(UUID id, UUID ownerId, com.iptv.wiseplayer.domain.enums.OwnerType ownerType);
}
