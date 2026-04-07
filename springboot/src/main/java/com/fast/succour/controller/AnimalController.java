package com.fast.succour.controller;

import com.fast.succour.domain.Animal;
import com.fast.succour.service.IAnimalService;
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
@RequestMapping("/succour/animal")
public class AnimalController extends BaseController {
    @Resource
    private IAnimalService animalService;

    @Resource
    private SysRoleMapper roleMapper;

    /**
     * 查询宠物列表
     */
    @GetMapping("/list")
    public TableDataInfo list(Animal animal) {
        //根据用户ID查询当前的角色, 如果是救助站角色就只能查看自己发布的萌宠
        Integer roleId = roleMapper.selectRoleIdByUserId(getUserId());
        if (roleId == 102) {
            animal.setStationForUserId(getUserId());
        }

        startPage();
        List<Animal> list = animalService.selectAnimalList(animal);
        return getDataTable(list);
    }

    /**
     * 获取宠物详细信息
     */
    @GetMapping(value = "/{animalId}")
    public AjaxResult getInfo(@PathVariable("animalId") String animalId) {
        return success(animalService.selectAnimalByAnimalId(animalId));
    }

    /**
     * 新增宠物
     */
    @PostMapping
    public AjaxResult add(@RequestBody Animal animal) {
        return toAjax(animalService.insertAnimal(animal));
    }

    /**
     * 修改宠物
     */
    @PutMapping
    public AjaxResult edit(@RequestBody Animal animal) {
        return toAjax(animalService.updateAnimal(animal));
    }

    /**
     * 删除宠物
     */
    @DeleteMapping("/{animalIds}")
    public AjaxResult remove(@PathVariable String[] animalIds) {
        return toAjax(animalService.deleteAnimalByAnimalIds(animalIds));
    }

    /**
     * 根据救助站ID查询该救助站的可领养动物
     */
    @GetMapping("/selectAnimalListByStationId/{stationId}")
    public AjaxResult selectAnimalListByStationId(@PathVariable String stationId) {
        List<Animal> list = animalService.selectAnimalListByStationId(stationId);
        return success(list);
    }

}
