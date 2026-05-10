package com.ecommerce.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.auth.entity.*;
import com.ecommerce.auth.mapper.*;
import com.ecommerce.common.result.Result;
import com.ecommerce.common.util.SnowflakeUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
public class RbacController {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final AdminUserRoleMapper adminUserRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final AdminUserMapper adminUserMapper;

    public RbacController(RoleMapper roleMapper, PermissionMapper permissionMapper,
                           AdminUserRoleMapper adminUserRoleMapper,
                           RolePermissionMapper rolePermissionMapper,
                           AdminUserMapper adminUserMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.adminUserRoleMapper = adminUserRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.adminUserMapper = adminUserMapper;
    }

    // ==================== 角色管理 ====================

    @GetMapping("/roles")
    public Result<List<Role>> listRoles() {
        List<Role> roles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>().orderByAsc(Role::getCreatedAt));
        for (Role role : roles) {
            List<RolePermission> rps = rolePermissionMapper.selectList(
                    new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, role.getId()));
            role.setPermissionIds(rps.stream().map(RolePermission::getPermissionId).collect(Collectors.toList()));
        }
        return Result.ok(roles);
    }

    @PostMapping("/roles")
    public Result<Role> createRole(@RequestBody Role role) {
        role.setId(SnowflakeUtils.nextId());
        roleMapper.insert(role);
        saveRolePermissions(role);
        return Result.ok(role);
    }

    @PutMapping("/roles/{id}")
    public Result<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        roleMapper.updateById(role);
        // 重建权限关联
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        saveRolePermissions(role);
        return Result.ok(role);
    }

    @DeleteMapping("/roles/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        adminUserRoleMapper.delete(
                new LambdaQueryWrapper<AdminUserRole>().eq(AdminUserRole::getRoleId, id));
        roleMapper.deleteById(id);
        return Result.ok();
    }

    private void saveRolePermissions(Role role) {
        if (role.getPermissionIds() != null) {
            for (Long permId : role.getPermissionIds()) {
                RolePermission rp = new RolePermission();
                rp.setId(SnowflakeUtils.nextId());
                rp.setRoleId(role.getId());
                rp.setPermissionId(permId);
                rolePermissionMapper.insert(rp);
            }
        }
    }

    // ==================== 权限管理 ====================

    @GetMapping("/permissions")
    public Result<List<Permission>> listPermissions() {
        List<Permission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSort));
        return Result.ok(buildPermTree(all));
    }

    @PostMapping("/permissions")
    public Result<Permission> createPermission(@RequestBody Permission perm) {
        perm.setId(SnowflakeUtils.nextId());
        permissionMapper.insert(perm);
        return Result.ok(perm);
    }

    @PutMapping("/permissions/{id}")
    public Result<Permission> updatePermission(@PathVariable Long id, @RequestBody Permission perm) {
        perm.setId(id);
        permissionMapper.updateById(perm);
        return Result.ok(perm);
    }

    @DeleteMapping("/permissions/{id}")
    public Result<Void> deletePermission(@PathVariable Long id) {
        permissionMapper.deleteById(id);
        return Result.ok();
    }

    private List<Permission> buildPermTree(List<Permission> all) {
        Map<Long, Permission> map = new HashMap<>();
        List<Permission> roots = new ArrayList<>();
        for (Permission p : all) {
            map.put(p.getId(), p);
            p.setChildren(new ArrayList<>());
        }
        for (Permission p : all) {
            if (p.getParentId() != null && p.getParentId() > 0 && map.containsKey(p.getParentId())) {
                map.get(p.getParentId()).getChildren().add(p);
            } else {
                roots.add(p);
            }
        }
        return roots;
    }

    // ==================== 管理员角色分配 ====================

    @GetMapping("/users/{adminUserId}/roles")
    public Result<List<Long>> getUserRoles(@PathVariable Long adminUserId) {
        List<AdminUserRole> list = adminUserRoleMapper.selectList(
                new LambdaQueryWrapper<AdminUserRole>().eq(AdminUserRole::getAdminUserId, adminUserId));
        return Result.ok(list.stream().map(AdminUserRole::getRoleId).collect(Collectors.toList()));
    }

    @PutMapping("/users/{adminUserId}/roles")
    public Result<Void> assignRoles(@PathVariable Long adminUserId, @RequestBody Map<String, List<Long>> body) {
        List<Long> roleIds = body.get("roleIds");
        // 删除旧关联
        adminUserRoleMapper.delete(
                new LambdaQueryWrapper<AdminUserRole>().eq(AdminUserRole::getAdminUserId, adminUserId));
        // 创建新关联
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                AdminUserRole aur = new AdminUserRole();
                aur.setId(SnowflakeUtils.nextId());
                aur.setAdminUserId(adminUserId);
                aur.setRoleId(roleId);
                adminUserRoleMapper.insert(aur);
            }
        }
        return Result.ok();
    }

    // ==================== 管理员列表（含角色） ====================

    @GetMapping("/admin-users")
    public Result<List<Map<String, Object>>> listAdminUsers() {
        List<AdminUser> admins = adminUserMapper.selectList(new LambdaQueryWrapper<>());
        List<Map<String, Object>> result = new ArrayList<>();
        for (AdminUser admin : admins) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", admin.getId());
            m.put("username", admin.getUsername());
            m.put("type", admin.getType());
            m.put("status", admin.getStatus());
            List<AdminUserRole> roles = adminUserRoleMapper.selectList(
                    new LambdaQueryWrapper<AdminUserRole>().eq(AdminUserRole::getAdminUserId, admin.getId()));
            m.put("roleIds", roles.stream().map(AdminUserRole::getRoleId).collect(Collectors.toList()));
            result.add(m);
        }
        return Result.ok(result);
    }
}
