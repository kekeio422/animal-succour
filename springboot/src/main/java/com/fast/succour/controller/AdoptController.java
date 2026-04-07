package com.fast.succour.controller;

import com.fast.succour.domain.Adopt;
import com.fast.succour.service.IAdoptService;
import com.fast.system.general.core.controller.BaseController;
import com.fast.system.general.core.domain.AjaxResult;
import com.fast.system.general.core.page.TableDataInfo;
import com.fast.system.mapper.SysRoleMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 宠物Controller
 *
 * @author huacai
 * @date 2025-12-16
 */
@RestController
@RequestMapping("/succour/adopt")
public class AdoptController extends BaseController {
    @Resource
    private IAdoptService adoptService;

    @Resource
    private SysRoleMapper roleMapper;

    /**
     * 查询领养申请列表
     */
    @GetMapping("/list")
    public TableDataInfo list(Adopt adopt) {
        //根据用户ID查询当前的角色Id, 超级管理员可看所有申请, 救助站可看自己发布的萌宠的领养申请, 用户可看自己提交的申请
        Integer roleId = roleMapper.selectRoleIdByUserId(getUserId());

        if (roleId == 102) {
            adopt.setStationForUserId(getUserId());
        }

        if (roleId == 2) {
            adopt.setUserId(Math.toIntExact(getUserId()));
        }

        startPage();
        List<Adopt> list = adoptService.selectAdoptList(adopt);
        return getDataTable(list);
    }

    /**
     * 获取领养申请信息
     */
    @GetMapping(value = "/{adoptId}")
    public AjaxResult getInfo(@PathVariable("adoptId") String adoptId) {
        return success(adoptService.selectAdoptByAdoptId(adoptId));
    }

    /**
     * 新增领养申请
     */
    @PostMapping
    public AjaxResult add(@RequestBody Adopt adopt) {
        return toAjax(adoptService.insertAdopt(adopt));
    }

    /**
     * 修改领养申请
     */
    @PutMapping
    public AjaxResult edit(@RequestBody Adopt adopt) {
        return toAjax(adoptService.updateAdopt(adopt));
    }

    /**
     * 删除领养申请
     */
    @DeleteMapping("/{adoptIds}")
    public AjaxResult remove(@PathVariable String[] adoptIds) {
        return toAjax(adoptService.deleteAdoptByAdoptIds(adoptIds));
    }

    /**
     * 撤销申请
     */
    @PutMapping("/revoke/{adoptId}")
    public AjaxResult revoke(@PathVariable String adoptId) {
        return toAjax(adoptService.revoke(adoptId));
    }

}
