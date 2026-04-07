package com.fast.succour.domain;

import lombok.Data;

/**
 * 轮播图对象 banner
 *
 * @author huacai
 * @date 2025-12-16
 */

@Data
public class Banner
{

    /** 轮播图ID */
    private String bannerId;

    /** 图片 */
    private String image;

    /** 排序 */
    private Long sort;


}
