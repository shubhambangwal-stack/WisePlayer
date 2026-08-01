package com.iptv.wiseplayer.util;

import com.iptv.wiseplayer.domain.entity.RolePermission;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.repository.RolePermissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class BulkPermissionUtil {

    private final RolePermissionRepository rolePermissionRepository;

    public BulkPermissionUtil(RolePermissionRepository rolePermissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public Map<String, Object> getBulkPermissionMap(AdminRole role) {
        RolePermission rp = rolePermissionRepository.findByRole(role)
                .orElse(RolePermission.allTrue(role));

        Map<String, Object> bulkMap = new LinkedHashMap<>();
        bulkMap.put("role", role.name());
        bulkMap.put("canCreate", rp.isCanCreate());
        bulkMap.put("canRead", rp.isCanRead());
        bulkMap.put("canUpdate", rp.isCanUpdate());
        bulkMap.put("canDelete", rp.isCanDelete());
        if (rp.getUpdatedAt() != null) {
            bulkMap.put("updatedAt", rp.getUpdatedAt());
        }
        return bulkMap;
    }

    public <T> Map<String, Object> wrapPageWithBulkPermissions(AdminRole role, Page<T> page) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bulkPermissions", getBulkPermissionMap(role));
        response.put("content", page.getContent());
        response.put("pageable", page.getPageable());
        response.put("totalElements", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("last", page.isLast());
        response.put("size", page.getSize());
        response.put("number", page.getNumber());
        response.put("sort", page.getSort());
        response.put("numberOfElements", page.getNumberOfElements());
        response.put("first", page.isFirst());
        response.put("empty", page.isEmpty());
        return response;
    }

    public <T> Map<String, Object> wrapListWithBulkPermissions(AdminRole role, Iterable<T> list) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bulkPermissions", getBulkPermissionMap(role));
        response.put("content", list);
        return response;
    }
}
