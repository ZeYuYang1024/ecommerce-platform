package com.ecommerce.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.auth.dto.request.AssignRolesRequest;
import com.ecommerce.auth.dto.response.AdminUserVO;
import com.ecommerce.auth.dto.response.PermissionVO;
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
    public Result<Page<Role>> listRoles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<Role> ipage = roleMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Role>().orderByAsc(Role::getCreatedAt));
        for (Role role : ipage.getRecords()) {
            List<RolePermission> rps = rolePermissionMapper.selectList(
                    new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, role.getId()));
            role.setPermissionIds(rps.stream().map(RolePermission::getPermissionId).collect(Collectors.toList()));
        }
        Page<Role> result = new Page<>(page, size);
        result.setTotal(ipage.getTotal());
        result.setRecords(ipage.getRecords());
        return Result.ok(result);
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
    public Result<?> listPermissions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && size != null) {
            IPage<Permission> ipage = permissionMapper.selectPage(
                    new Page<>(page, size),
                    new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSort));
            Page<PermissionVO> result = new Page<>(page, size);
            result.setTotal(ipage.getTotal());
            result.setRecords(ipage.getRecords().stream().map(p -> {
                PermissionVO vo = new PermissionVO();
                vo.setId(p.getId());
                vo.setName(p.getName());
                vo.setCode(p.getCode());
                vo.setType(p.getType());
                vo.setPath(p.getPath());
                vo.setParentId(p.getParentId());
                vo.setSort(p.getSort());
                return vo;
            }).collect(Collectors.toList()));
            return Result.ok(result);
        }
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
    public Result<Void> assignRoles(@PathVariable Long adminUserId, @RequestBody AssignRolesRequest body) {
        List<Long> roleIds = body.getRoleIds();
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
    public Result<List<AdminUserVO>> listAdminUsers() {
        List<AdminUser> admins = adminUserMapper.selectList(new LambdaQueryWrapper<>());
        List<AdminUserVO> result = new ArrayList<>();
        for (AdminUser admin : admins) {
            AdminUserVO vo = new AdminUserVO();
            vo.setId(admin.getId());
            vo.setUsername(admin.getUsername());
            vo.setType(admin.getType());
            vo.setStatus(admin.getStatus());
            List<AdminUserRole> roles = adminUserRoleMapper.selectList(
                    new LambdaQueryWrapper<AdminUserRole>().eq(AdminUserRole::getAdminUserId, admin.getId()));
            vo.setRoleIds(roles.stream().map(AdminUserRole::getRoleId).collect(Collectors.toList()));
            result.add(vo);
        }
        return Result.ok(result);
    }
}
