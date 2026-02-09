package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {
    java.util.List<Playlist> findByDeviceId(UUID deviceId);

    void deleteByDeviceIdAndName(UUID deviceId, String name);
}
