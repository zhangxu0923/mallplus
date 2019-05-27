package com.zscat.mallplus.single;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zscat.mallplus.annotation.IgnoreAuth;
import com.zscat.mallplus.annotation.SysLog;
import com.zscat.mallplus.cms.service.ICmsSubjectCategoryService;
import com.zscat.mallplus.cms.service.ICmsSubjectCommentService;
import com.zscat.mallplus.cms.service.ICmsSubjectService;
import com.zscat.mallplus.pms.entity.PmsProduct;
import com.zscat.mallplus.pms.entity.PmsProductCategory;
import com.zscat.mallplus.pms.service.IPmsProductAttributeCategoryService;
import com.zscat.mallplus.pms.service.IPmsProductCategoryService;
import com.zscat.mallplus.pms.service.IPmsProductService;
import com.zscat.mallplus.pms.vo.PmsProductParam;
import com.zscat.mallplus.sms.service.ISmsGroupService;
import com.zscat.mallplus.sms.service.ISmsHomeAdvertiseService;
import com.zscat.mallplus.ums.entity.UmsMember;
import com.zscat.mallplus.ums.entity.UmsMemberLevel;
import com.zscat.mallplus.ums.service.IUmsMemberLevelService;
import com.zscat.mallplus.ums.service.RedisService;
import com.zscat.mallplus.util.JsonUtil;
import com.zscat.mallplus.utils.CommonResult;
import com.zscat.mallplus.vo.Rediskey;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Auther: shenzhuan
 * @Date: 2019/4/2 15:02
 * @Description:
 */
@RestController
@Api(tags = "SingePmsController", description = "商品关系管理")
@RequestMapping("/api/single/pms")
public class SingePmsController extends ApiBaseAction {

    @Resource
    private ISmsGroupService groupService;
    @Resource
    private IUmsMemberLevelService memberLevelService;
    @Resource
    private IPmsProductService pmsProductService;
    @Resource
    private IPmsProductAttributeCategoryService productAttributeCategoryService;
    @Resource
    private IPmsProductCategoryService productCategoryService;


    @Resource
    private ICmsSubjectCategoryService subjectCategoryService;
    @Resource
    private ICmsSubjectService subjectService;
    @Resource
    private ICmsSubjectCommentService commentService;
    @Autowired
    private ISmsHomeAdvertiseService advertiseService;

    @Resource
    private RedisService redisService;

    @SysLog(MODULE = "pms", REMARK = "查询商品详情信息")
    @IgnoreAuth
    @GetMapping(value = "/goods/detail")
    @ApiOperation(value = "查询商品详情信息")
    public Object queryProductDetail(@RequestParam(value = "id", required = false, defaultValue = "0") Long id) {
        PmsProductParam goods = null;
        try {
            goods = JsonUtil.jsonToPojo(redisService.get(String.format(Rediskey.GOODSDETAIL, id)), PmsProductParam.class);
        } catch (Exception e) {
            goods = pmsProductService.getGoodsRedisById(id);
        }
        Map<String, Object> map = new HashMap<>();


        map.put("goods", goods);
        return new CommonResult().success(map);
    }

    @SysLog(MODULE = "pms", REMARK = "查询商品列表")
    @IgnoreAuth
    @ApiOperation(value = "查询商品列表")
    @GetMapping(value = "/goods/list")
    public Object goodsList(PmsProduct product,
                            @RequestParam(value = "pageSize", required = false, defaultValue = "5") Integer pageSize,
                            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum) {
        product.setPublishStatus(1);
        product.setVerifyStatus(1);
        return new CommonResult().success(pmsProductService.page(new Page<PmsProduct>(pageNum, pageSize), new QueryWrapper<>(product)));
    }

    @SysLog(MODULE = "pms", REMARK = "查询商品分类列表")
    @IgnoreAuth
    @ApiOperation(value = "查询商品分类列表")
    @GetMapping(value = "/productCategory/list")
    public Object productCategoryList(PmsProductCategory productCategory,
                                      @RequestParam(value = "pageSize", required = false, defaultValue = "5") Integer pageSize,
                                      @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum) {
        return new CommonResult().success(productCategoryService.page(new Page<PmsProductCategory>(pageNum, pageSize), new QueryWrapper<>(productCategory)));
    }


    @ApiOperation("创建商品")
    @SysLog(MODULE = "pms", REMARK = "创建商品")
    @PostMapping(value = "/createGoods")
    public Object createGoods(PmsProduct productParam) {
        CommonResult commonResult;
        UmsMember member = this.getCurrentMember();
        if (member.getMemberLevelId() > 0) {
            UmsMemberLevel memberLevel = memberLevelService.getById(member.getMemberLevelId());
            PmsProduct newSubject = new PmsProduct();
            newSubject.setSupplyId(member.getId());
            newSubject.setPublishStatus(1);
            newSubject.setVerifyStatus(1);
            List<PmsProduct> subjects = pmsProductService.list(new QueryWrapper<>(newSubject));
            if (subjects != null && subjects.size() > memberLevel.getGoodscount()) {
                commonResult = new CommonResult().failed("你今天已经有发" + memberLevel.getGoodscount() + "个商品");
                return commonResult;
            }
        }
        productParam.setSupplyId(member.getId());
        productParam.setCreateTime(new Date());
        boolean count = pmsProductService.save(productParam);
        if (count) {
            return new CommonResult().success(count);
        } else {
            return new CommonResult().failed();
        }
    }


    @SysLog(MODULE = "pms", REMARK = "查询商品列表")
    @IgnoreAuth
    @ApiOperation(value = "查询首页推荐商品")
    @GetMapping(value = "/initGoodsRedis")
    public Object initGoodsRedis() {

        return pmsProductService.initGoodsRedis();

    }

}
