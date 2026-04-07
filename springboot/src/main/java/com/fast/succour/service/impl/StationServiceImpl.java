package com.fast.succour.service.impl;

import com.fast.succour.domain.Station;
import com.fast.succour.mapper.StationMapper;
import com.fast.succour.service.IStationService;
import com.fast.system.general.utils.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.fast.system.general.utils.SecurityUtils.getUserId;

/**
 * 救助站Service业务层处理
 *
 * @author huacai
 * @date 2025-12-15
 */
@Service
public class StationServiceImpl implements IStationService {
    private static final String GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${map.amap.web-key:}")
    private String amapWebKey;

    @Resource
    private StationMapper stationMapper;

    /**
     * 查询救助站
     *
     * @param stationId 救助站主键
     * @return 救助站
     */
    @Override
    public Station selectStationByStationId(String stationId) {
        return stationMapper.selectStationByStationId(stationId);
    }

    /**
     * 查询救助站列表
     *
     * @param station 救助站
     * @return 救助站
     */
    @Override
    public List<Station> selectStationList(Station station) {
        return stationMapper.selectStationList(station);
    }

    /**
     * 新增救助站
     *
     * @param station 救助站
     * @return 结果
     */
    @Override
    public int insertStation(Station station) {
        fillGeoLocation(station);
        station.setCreateTime(new Date());
        station.setStationId(String.valueOf(UUID.randomUUID()));
        station.setUserId(Math.toIntExact(getUserId()));
        return stationMapper.insertStation(station);
    }

    /**
     * 修改救助站
     *
     * @param station 救助站
     * @return 结果
     */
    @Override
    public int updateStation(Station station) {
        if (StringUtils.isNotEmpty(station.getAddress())) {
            fillGeoLocation(station);
        }
        return stationMapper.updateStation(station);
    }

    /**
     * 批量删除救助站
     *
     * @param stationIds 需要删除的救助站主键
     * @return 结果
     */
    @Override
    public int deleteStationByStationIds(String[] stationIds) {
        return stationMapper.deleteStationByStationIds(stationIds);
    }

    /**
     * 查询当前用户是否已经提交救助站认证
     * @return 是否已经提交救助站认证
     */
    @Override
    public Boolean selectIsAuth() {
        Long userId = getUserId();
        return stationMapper.selectIsAuth(userId);
    }

    /**
     * 通过认证
     * @param userId 要通过认证的用户ID
     * @return 是否通过认证
     */
    @Override
    @Transactional
    public int toAuth(Integer userId) {
        //用户和角色关联表中根据用户ID修改角色ID
        stationMapper.updateUserRoleByUserId(userId);

        //根据用户ID查询救助站ID
        String stationId = stationMapper.selectStationIdByUserId(userId);

        //将审核状态修改为已通过
        Station station = new Station();
        station.setStationId(stationId);
        station.setStatus("审核通过");

        return stationMapper.updateStation(station);
    }

    /**
     * 查询状态为审核通过的救助站
     * @return 救助站列表
     */
    @Override
    public List<Station> selectStationListByIsAuth() {
        return stationMapper.selectStationListByIsAuth();
    }

    /**
     * 通过地址转换经纬度，用于地图展示及地址真实性校验
     */
    private void fillGeoLocation(Station station) {
        if (StringUtils.isBlank(station.getAddress())) {
            throw new RuntimeException("请填写救助站真实地址后再提交认证");
        }
        if (StringUtils.isBlank(amapWebKey)) {
            throw new RuntimeException("地图服务未配置高德 Web Key，请联系管理员配置 map.amap.web-key");
        }
        try {
            String encodeAddress = URLEncoder.encode(station.getAddress(), StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEOCODE_URL + "?address=" + encodeAddress + "&key=" + amapWebKey))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            if (!"1".equals(root.path("status").asText())) {
                throw new RuntimeException("地址校验失败，请稍后重试");
            }
            JsonNode geocodes = root.path("geocodes");
            if (!geocodes.isArray() || geocodes.isEmpty()) {
                throw new RuntimeException("地址无法识别，请填写更完整、真实的救助站地址");
            }
            String location = geocodes.get(0).path("location").asText();
            String[] locationParts = location.split(",");
            if (locationParts.length != 2) {
                throw new RuntimeException("地址无法识别，请填写更完整、真实的救助站地址");
            }
            station.setLongitude(new BigDecimal(locationParts[0]));
            station.setLatitude(new BigDecimal(locationParts[1]));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("地址校验失败，请稍后重试或更换更准确的地址");
        }
    }
}
