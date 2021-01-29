package com.atguigu.gmall.wms.service;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.wms.entity.PurchaseDetailEntity;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author txl
 * @email txl@atguigu.com
 * @date 2021-01-20 09:31:30
 */
public interface PurchaseDetailService extends IService<PurchaseDetailEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

