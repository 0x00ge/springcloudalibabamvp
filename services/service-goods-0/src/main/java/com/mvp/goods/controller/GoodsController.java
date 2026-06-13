package com.mvp.goods.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mvp.goods.dto.GoodsDto;
import com.mvp.goods.entity.Goods;
import com.mvp.goods.service.GoodsService;
import com.mvp.common.controller.BaseController;
import com.mvp.common.vo.ResultVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;

/**
 * 商品控制器。
 *
 * <p>复用 {@code BaseController} 提供商品配置的最小单表 CRUD，供管理端维护
 * 秒杀价、总库存、限购数量和秒杀时间窗口。</p>
 */
@RestController
@RequestMapping("/goods")
public class GoodsController extends BaseController<Goods, GoodsDto> {

    public GoodsController(GoodsService goodsService) {
        super(goodsService);
    }

    /**
     * 根据主键查询商品。
     */
    @Override
    @GetMapping("/{id}")
    public ResultVO<GoodsDto> getById(@PathVariable String id) {
        return super.getById(id);
    }

    /**
     * 分页查询商品。
     */
    @Override
    @GetMapping("/page")
    public ResultVO<IPage<GoodsDto>> page(@RequestParam(defaultValue = "1") Long page,
                                          @RequestParam(defaultValue = "10") Long size) {
        return super.page(page, size);
    }

    /**
     * 新增商品配置。
     */
    @Override
    @PostMapping
    public ResultVO<Serializable> save(@Valid @RequestBody GoodsDto dto) {
        return super.save(dto);
    }

    /**
     * 更新商品配置。
     */
    @Override
    @PutMapping("/{id}")
    public ResultVO<Void> update(@PathVariable String id, @Valid @RequestBody GoodsDto dto) {
        return super.update(id, dto);
    }

    /**
     * 删除商品配置。
     */
    @Override
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable String id) {
        return super.delete(id);
    }
}
