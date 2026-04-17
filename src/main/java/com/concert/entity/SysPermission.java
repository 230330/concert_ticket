package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:    系统权限表实体类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Data
@TableName("sys_permission")
public class SysPermission {

    /**
     * 权限ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 权限编码（如：concert:add, order:refund）
     */
    @TableField("permission_code")
    private String permissionCode;

    /**
     * 权限名称（如：新增演唱会，订单退款）
     */
    @TableField("permission_name")
    private String permissionName;

    /**
     * 资源类型：menu-菜单，button-按钮
     */
    @TableField("resource_type")
    private String resourceType;

    /**
     * 父级ID（0表示顶级）
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 排序号
     */
    @TableField("sort")
    private Integer sort;

    /**
     * 状态：0-禁用，1-正常
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
