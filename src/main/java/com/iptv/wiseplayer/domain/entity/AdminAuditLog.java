package com.iptv.wiseplayer.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;

    @Column(name = "target_email", nullable = false)
    private String targetEmail;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    public AdminAuditLog(UUID performedBy, String targetEmail, String action, String ipAddress) {
        this.performedBy = performedBy;
        this.targetEmail = targetEmail;
        this.action = action;
        this.ipAddress = ipAddress;
    }
}
