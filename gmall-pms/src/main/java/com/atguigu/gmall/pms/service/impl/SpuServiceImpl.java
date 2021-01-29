package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallPmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.fasterxml.jackson.databind.util.BeanUtil;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jcajce.provider.util.SecretKeyUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.print.attribute.SupportedValuesAttribute;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescMapper descMapper;

    @Autowired
    private SpuAttrValueService attrValueService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuImagesService imagesService;

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private SpuDescService descService;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuByCidAndPage(Long cid, PageParamVo paramVo) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        //如果用户选择了分类,并查询本类
        if (cid != 0) {
            wrapper.eq("category_id", cid);
        }

        String key = paramVo.getKey();
        // 判断关键字是否为空
        if (StringUtils.isNotBlank(key)) {
            wrapper.and(t -> t.eq("id", key).or().like("name", key));


        }

        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                wrapper
        );

        return new PageResultVo(page);
    }

    @GlobalTransactional
    @Override
    public void bigSave(SpuVo spu) throws FileNotFoundException {
        //1、保存spu相关信息
        //1.1 保存spu的基本信息:pms_spu
        Long spuId = saveSpu(spu);

        //1.2 保存spu的描述信息:pms_spu_desc
        //this.saveSpuDesc(spu, spuId);
        this.descService.saveSpuDesc(spu, spuId);

//        try {
//            TimeUnit.SECONDS.sleep(4);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }


        //int i = 1/0;
        //new FileInputStream("xxxx");
        //1.3 保存spu的基本属性信息:pms_spu_attr_value
        saveBaseAttr(spu, spuId);


        //2、保存sku相关信息
        saveSkuInfo(spu, spuId);


    }

    private void saveSkuInfo(SpuVo spu, Long spuId) {
        List<SkuVo> skus = spu.getSkus();
        if (CollectionUtils.isEmpty(skus)) {
            return;
        }
        //2.1 保存sku的基本信息:pms_sku
        skus.forEach(sku -> {
            sku.setSpuId(spuId);
            sku.setBrandId(spu.getBrandId());
            sku.setCatagoryId(spu.getCategoryId());
            //设置默认图片
            List<String> images = sku.getImages();
            if (!CollectionUtils.isEmpty(images)) {
                sku.setDefaultImage(StringUtils.isNotBlank(sku.getDefaultImage()) ? sku.getDefaultImage() : images.get(0));
            }
            this.skuMapper.insert(sku);
            Long skuId = sku.getId();
            //2.2 保存sku的图片信息:pms_sku_image
            if (!CollectionUtils.isEmpty(images)) {
                imagesService.saveBatch(images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(sku.getDefaultImage(), image) ? 1 : 0);
                    return skuImagesEntity;

                }).collect(Collectors.toList()));
            }


            //2.3 保存sku的销售属性:pms_sku_attr_value
            List<SkuAttrValueEntity> saleAttrs = sku.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)) {
                saleAttrs.forEach(skuAttrValueEntity -> skuAttrValueEntity.setSkuId(skuId));
                this.skuAttrValueService.saveBatch(saleAttrs);
            }


            //3、保存sku的营销信息
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(sku, skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.pmsClient.savaSales(skuSaleVo);
        });
    }

    private void saveBaseAttr(SpuVo spu, Long spuId) {
        List<SpuAttrValueVo> baseAttrs = spu.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {

        }
        this.attrValueService.saveBatch(baseAttrs.stream().map(spuAttrValueVo -> {
            SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
            BeanUtils.copyProperties(spuAttrValueVo, spuAttrValueEntity);
            spuAttrValueEntity.setSpuId(spuId);
            return spuAttrValueEntity;
        }).collect(Collectors.toList()));
    }


    private Long saveSpu(SpuVo spu) {
        spu.setCreateTime(new Date());
        spu.setUpdateTime(spu.getCreateTime());
        this.save(spu);
        return spu.getId();
    }
}