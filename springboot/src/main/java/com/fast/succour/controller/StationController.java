package com.fast.succour.controller;

import com.fast.succour.domain.Station;
import com.fast.succour.service.IStationService;
import com.fast.system.general.core.controller.BaseController;
import com.fast.system.general.core.domain.AjaxResult;
import com.fast.system.general.core.page.TableDataInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 救助站Controller
 *
 * @author huacai
 * @date 2025-12-15
 */
@RestController
@RequestMapping("/succour/station")
public class StationController extends BaseController {
    @Resource
    private IStationService stationService;

    /**
     * 查询救助站列表
     */
    @GetMapping("/list")
    public TableDataInfo list(Station station) {
        startPage();
        List<Station> list = stationService.selectStationList(station);
        return getDataTable(list);
    }

    /**
     * 获取救助站详细信息
     */
    @GetMapping(value = "/{stationId}")
    public AjaxResult getInfo(@PathVariable("stationId") String stationId) {
        return success(stationService.selectStationByStationId(stationId));
    }

    /**
     * 新增救助站
     */
    @PostMapping
    public AjaxResult add(@RequestBody Station station) {
        return toAjax(stationService.insertStation(station));
    }

    /**
     * 修改救助站
     */
    @PutMapping
    public AjaxResult edit(@RequestBody Station station) {
        return toAjax(stationService.updateStation(station));
    }

    /**
     * 删除救助站
     */
    @DeleteMapping("/{stationIds}")
    public AjaxResult remove(@PathVariable String[] stationIds) {
        return toAjax(stationService.deleteStationByStationIds(stationIds));
    }

    /**
     * 查询当前用户是否已经提交救助站认证
     */
    @GetMapping("/selectIsAuth")
    public AjaxResult selectIsAuth() {
        Boolean isAuth = stationService.selectIsAuth();
        return success(isAuth);
    }

    /**
     * 通过认证
     */
    @PutMapping("/toAuth/{userId}")
    public AjaxResult toAuth(@PathVariable Integer userId) {
        return toAjax(stationService.toAuth(userId));
    }

    /**
     * 查询状态为审核通过的救助站
     */
    @GetMapping("/selectStationListByIsAuth")
    public AjaxResult selectStationListByIsAuth() {
        List<Station> list = stationService.selectStationListByIsAuth();
        return success(list);
    }

    /**
     * 地址联想建议
     */
    @GetMapping("/addressTips")
    public AjaxResult addressTips(@RequestParam("keywords") String keywords) {
        List<Map<String, String>> list = stationService.selectAddressTips(keywords);
        return success(list);
    }

}
