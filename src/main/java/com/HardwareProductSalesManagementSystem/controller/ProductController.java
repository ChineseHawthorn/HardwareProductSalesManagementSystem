package com.HardwareProductSalesManagementSystem.controller;

import com.HardwareProductSalesManagementSystem.commons.APIRes;
import com.HardwareProductSalesManagementSystem.pojo.*;
import com.HardwareProductSalesManagementSystem.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 商品管理控制器
 * 基础路径：/api/v1/product
 */
@RestController
@RequestMapping("/api/v1/product")
public class ProductController {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductSpecService productSpecService;
    @Autowired
    private ProductImageService productImageService;
    @Autowired
    private ProductCategoryService productCategoryService;
    @Autowired
    private PriceStrategyService priceStrategyService;
    @Autowired
    private com.HardwareProductSalesManagementSystem.service.OrderService orderService;
    @Autowired
    private com.HardwareProductSalesManagementSystem.service.OrderDetailService orderDetailService;

    @Value("${upload.product.path:D:/Project/毕业设计/HardwareProductSalesManagementSystem/upload/images/product/}")
    private String productImagePath;

    // ======================== 商品分类 ========================

    /** GET /api/v1/category/list - 获取分类树 */
    @GetMapping("/category/list")
    public APIRes categoryList() {
        List<ProductCategory> all = productCategoryService.list(
                new LambdaQueryWrapper<ProductCategory>().orderByAsc(ProductCategory::getSortOrder));
        return APIRes.ok("获取成功", buildCategoryTree(all, 0));
    }

    /** POST /api/v1/category/add - 新增分类（店主/店员） */
    @PostMapping("/category/add")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public APIRes categoryAdd(@RequestBody ProductCategory category) {
        if (StringUtils.isBlank(category.getCategoryName())) {
            return APIRes.fail(400, "分类名称不能为空");
        }
        category.setCreateTime(new Date());
        category.setUpdateTime(new Date());
        if (category.getParentId() == null) category.setParentId(0);
        if (category.getSortOrder() == null) category.setSortOrder(0);
        if (category.getStatus() == null) category.setStatus(1);
        productCategoryService.save(category);
        return APIRes.ok("新增分类成功", category);
    }

    /** PUT /api/v1/category/update/{id} - 修改分类（店主） */
    @PutMapping("/category/update/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes categoryUpdate(@PathVariable Integer id, @RequestBody ProductCategory category) {
        category.setCategoryId(id);
        category.setUpdateTime(new Date());
        productCategoryService.updateById(category);
        return APIRes.ok("修改成功");
    }

    /** PUT /api/v1/category/status/{id} - 启用/禁用分类（店主） */
    @PutMapping("/category/status/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes categoryStatus(@PathVariable Integer id, @RequestParam Integer status) {
        ProductCategory cat = new ProductCategory();
        cat.setCategoryId(id);
        cat.setStatus(status);
        cat.setUpdateTime(new Date());
        productCategoryService.updateById(cat);
        return APIRes.ok(status == 1 ? "已启用" : "已禁用");
    }

    // ======================== 商品CRUD ========================

    /**
     * GET /api/v1/product/list - 分页查询商品列表
     * 支持筛选：categoryId, keyword, isOnSale, pageNum, pageSize
     * 支持排序：sortBy = default(最新) | price_asc | price_desc | sales_desc | stock_desc | material_asc
     */
    @GetMapping("/list")
    public APIRes list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) Integer categoryId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer isOnSale,
                       @RequestParam(defaultValue = "default") String sortBy) {
        // 加载所有分类，构建查找Map（用于分类名称填充和父分类判断）
        List<ProductCategory> allCats = productCategoryService.list();
        Map<Integer, ProductCategory> catMap = new HashMap<>();
        for (ProductCategory c : allCats) catMap.put(c.getCategoryId(), c);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            // 判断是否为父分类（有子分类）
            List<Integer> childIds = allCats.stream()
                    .filter(c -> categoryId.equals(c.getParentId()))
                    .map(ProductCategory::getCategoryId)
                    .collect(Collectors.toList());
            if (!childIds.isEmpty()) {
                wrapper.in(Product::getCategoryId, childIds);
            } else {
                wrapper.eq(Product::getCategoryId, categoryId);
            }
        }
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Product::getProductName, keyword)
                    .or().like(Product::getBrand, keyword)
                    .or().like(Product::getModel, keyword));
        }
        if (isOnSale != null) wrapper.eq(Product::getIsOnSale, isOnSale);
        wrapper.orderByDesc(Product::getCreateTime); // 默认时间倒序，作为次级排序

        // 非默认排序时需要全量加载再内存排序（小型项目数据量可控）
        boolean inMemorySort = !"default".equals(sortBy);

        List<Product> productList;
        long total;
        long pages;

        if (inMemorySort) {
            productList = productService.list(wrapper);
            total = productList.size();
            pages = (long) Math.ceil((double) total / pageSize);
        } else {
            Page<Product> dbPage = productService.page(new Page<>(pageNum, pageSize), wrapper);
            productList = dbPage.getRecords();
            total = dbPage.getTotal();
            pages = dbPage.getPages();
        }

        // 预计算近30天销量（sales_desc排序时使用）
        Map<Integer, Integer> salesMap = new HashMap<>();
        if ("sales_desc".equals(sortBy)) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -30);
            List<com.HardwareProductSalesManagementSystem.pojo.Order> recentOrders =
                    orderService.list(new LambdaQueryWrapper<com.HardwareProductSalesManagementSystem.pojo.Order>()
                            .ge(com.HardwareProductSalesManagementSystem.pojo.Order::getCreateTime, cal.getTime())
                            .in(com.HardwareProductSalesManagementSystem.pojo.Order::getPayStatus, 1, 4, 5));
            if (!recentOrders.isEmpty()) {
                List<String> orderIds = recentOrders.stream()
                        .map(com.HardwareProductSalesManagementSystem.pojo.Order::getOrderId)
                        .collect(Collectors.toList());
                List<OrderDetail> details = orderDetailService.list(
                        new LambdaQueryWrapper<OrderDetail>().in(OrderDetail::getOrderId, orderIds));
                for (OrderDetail d : details) {
                    ProductSpec sp = productSpecService.getById(d.getSpecId());
                    if (sp != null) {
                        salesMap.merge(sp.getProductId(), d.getSaleQuantity() != null ? d.getSaleQuantity() : 1, Integer::sum);
                    }
                }
            }
        }

        // 为每个商品附加首图、最低价、首个规格ID、分类名称、材质、规格数
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Product p : productList) {
            Map<String, Object> row = new HashMap<>();
            row.put("productId", p.getProductId());
            row.put("productName", p.getProductName());
            row.put("brand", p.getBrand());
            row.put("model", p.getModel());
            row.put("isOnSale", p.getIsOnSale());
            row.put("description", p.getDescription());
            row.put("createTime", p.getCreateTime());
            row.put("salesCount", salesMap.getOrDefault(p.getProductId(), 0));

            // 分类名称及父分类信息
            ProductCategory cat = catMap.get(p.getCategoryId());
            row.put("categoryId", p.getCategoryId());
            row.put("categoryName", cat != null ? cat.getCategoryName() : null);
            if (cat != null && cat.getParentId() != null && cat.getParentId() != 0) {
                ProductCategory parentCat = catMap.get(cat.getParentId());
                row.put("parentCategoryId", cat.getParentId());
                row.put("parentCategoryName", parentCat != null ? parentCat.getCategoryName() : null);
            } else {
                row.put("parentCategoryId", null);
                row.put("parentCategoryName", null);
            }

            // 主图
            ProductImage mainImg = productImageService.getOne(
                    new LambdaQueryWrapper<ProductImage>()
                            .eq(ProductImage::getProductId, p.getProductId())
                            .eq(ProductImage::getImageSort, 1)
                            .eq(ProductImage::getIsDeleted, 0)
                            .last("LIMIT 1"));
            row.put("mainImage", mainImg != null ? mainImg.getImageUrl() : null);

            // 规格：最低价 + 首个specId + 总库存 + 材质 + 规格数量
            List<ProductSpec> specs = productSpecService.list(
                    new LambdaQueryWrapper<ProductSpec>()
                            .eq(ProductSpec::getProductId, p.getProductId())
                            .orderByAsc(ProductSpec::getSpecId));
            if (!specs.isEmpty()) {
                ProductSpec first = specs.get(0);
                row.put("firstSpecId", first.getSpecId());
                row.put("unit", first.getUnit());
                row.put("material", first.getMaterial());           // 主材质（首规格）
                row.put("specCount", specs.size());                  // 规格数量
                BigDecimal minPrice = specs.stream()
                        .map(ProductSpec::getUnitPrice)
                        .filter(Objects::nonNull)
                        .min(BigDecimal::compareTo)
                        .orElse(first.getUnitPrice());
                BigDecimal maxPrice = specs.stream()
                        .map(ProductSpec::getUnitPrice)
                        .filter(Objects::nonNull)
                        .max(BigDecimal::compareTo)
                        .orElse(first.getUnitPrice());
                row.put("minPrice", minPrice);
                row.put("maxPrice", maxPrice);
                int totalStock = specs.stream().mapToInt(s -> s.getStockQuantity() != null ? s.getStockQuantity() : 0).sum();
                row.put("totalStock", totalStock);
            } else {
                row.put("firstSpecId", null);
                row.put("unit", null);
                row.put("material", null);
                row.put("specCount", 0);
                row.put("minPrice", null);
                row.put("maxPrice", null);
                row.put("totalStock", 0);
            }
            enriched.add(row);
        }

        // 内存排序
        if (inMemorySort) {
            switch (sortBy) {
                case "price_asc":
                    enriched.sort((a, b) -> {
                        BigDecimal pa = (BigDecimal) a.get("minPrice");
                        BigDecimal pb = (BigDecimal) b.get("minPrice");
                        if (pa == null && pb == null) return 0;
                        if (pa == null) return 1;
                        if (pb == null) return -1;
                        return pa.compareTo(pb);
                    });
                    break;
                case "price_desc":
                    enriched.sort((a, b) -> {
                        BigDecimal pa = (BigDecimal) a.get("minPrice");
                        BigDecimal pb = (BigDecimal) b.get("minPrice");
                        if (pa == null && pb == null) return 0;
                        if (pa == null) return 1;
                        if (pb == null) return -1;
                        return pb.compareTo(pa);
                    });
                    break;
                case "sales_desc":
                    enriched.sort((a, b) -> Integer.compare(
                            (Integer) b.getOrDefault("salesCount", 0),
                            (Integer) a.getOrDefault("salesCount", 0)));
                    break;
                case "stock_desc":
                    enriched.sort((a, b) -> Integer.compare(
                            (Integer) b.getOrDefault("totalStock", 0),
                            (Integer) a.getOrDefault("totalStock", 0)));
                    break;
                case "material_asc":
                    enriched.sort((a, b) -> {
                        String ma = a.get("material") != null ? a.get("material").toString() : "";
                        String mb = b.get("material") != null ? b.get("material").toString() : "";
                        return ma.compareTo(mb);
                    });
                    break;
                default:
                    break;
            }
            // 手动分页
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = (int) Math.min(fromIndex + pageSize, total);
            if (fromIndex >= total) {
                enriched = new ArrayList<>();
            } else {
                enriched = new ArrayList<>(enriched.subList(fromIndex, toIndex));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("pages", pages);
        result.put("current", (long) pageNum);
        result.put("records", enriched);
        return APIRes.ok("查询成功", result);
    }

    /** GET /api/v1/product/detail/{id} - 商品详情（含规格和图片） */
    @GetMapping("/detail/{id}")
    public APIRes detail(@PathVariable Integer id) {
        Product product = productService.getById(id);
        if (product == null) return APIRes.fail(404, "商品不存在");

        List<ProductSpec> specs = productSpecService.list(
                new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, id));
        List<ProductImage> images = productImageService.list(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, id)
                        .eq(ProductImage::getIsDeleted, 0)
                        .orderByAsc(ProductImage::getImageSort));

        Map<String, Object> result = new HashMap<>();
        result.put("product", product);
        result.put("specs", specs);
        result.put("images", images);
        return APIRes.ok("查询成功", result);
    }

    /** POST /api/v1/product/add - 新增商品（店主/店员） */
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public APIRes add(@RequestBody Product product) {
        if (StringUtils.isBlank(product.getProductName())) {
            return APIRes.fail(400, "商品名称不能为空");
        }
        product.setCreateTime(new Date());
        product.setUpdateTime(new Date());
        if (product.getIsOnSale() == null) product.setIsOnSale(1);
        productService.save(product);
        return APIRes.ok("新增商品成功", product);
    }

    /** PUT /api/v1/product/update/{id} - 修改商品信息（店主） */
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes update(@PathVariable Integer id, @RequestBody Product product) {
        product.setProductId(id);
        product.setUpdateTime(new Date());
        productService.updateById(product);
        return APIRes.ok("修改成功");
    }

    /** PUT /api/v1/product/status/{id} - 上架/下架商品（店主/店员） */
    @PutMapping("/status/{id}")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public APIRes status(@PathVariable Integer id, @RequestParam Integer isOnSale) {
        Product p = new Product();
        p.setProductId(id);
        p.setIsOnSale(isOnSale);
        p.setUpdateTime(new Date());
        productService.updateById(p);
        return APIRes.ok(isOnSale == 1 ? "上架成功" : "下架成功");
    }

    /**
     * GET /api/v1/product/hot - 热销商品（按近N天购买量降序，不足时补最新上架）
     */
    @GetMapping("/hot")
    public APIRes hotProducts(@RequestParam(defaultValue = "8") Integer limit,
                              @RequestParam(defaultValue = "30") Integer days) {
        // 1. 查询近N天已支付订单
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        Date startDate = cal.getTime();

        List<com.HardwareProductSalesManagementSystem.pojo.Order> recentOrders = orderService.list(
                new LambdaQueryWrapper<com.HardwareProductSalesManagementSystem.pojo.Order>()
                        .ge(com.HardwareProductSalesManagementSystem.pojo.Order::getCreateTime, startDate)
                        .in(com.HardwareProductSalesManagementSystem.pojo.Order::getPayStatus, 1, 4, 5));

        // 2. 统计每个商品的总销量
        Map<Integer, Integer> productSales = new HashMap<>();
        if (!recentOrders.isEmpty()) {
            List<String> orderIds = recentOrders.stream()
                    .map(com.HardwareProductSalesManagementSystem.pojo.Order::getOrderId)
                    .collect(Collectors.toList());

            List<com.HardwareProductSalesManagementSystem.pojo.OrderDetail> details = orderDetailService.list(
                    new LambdaQueryWrapper<com.HardwareProductSalesManagementSystem.pojo.OrderDetail>()
                            .in(com.HardwareProductSalesManagementSystem.pojo.OrderDetail::getOrderId, orderIds));

            Map<Integer, Integer> specSales = new HashMap<>();
            for (com.HardwareProductSalesManagementSystem.pojo.OrderDetail d : details) {
                if (d.getSpecId() != null)
                    specSales.merge(d.getSpecId(), d.getSaleQuantity() != null ? d.getSaleQuantity() : 0, Integer::sum);
            }

            if (!specSales.isEmpty()) {
                List<ProductSpec> specs = productSpecService.list(
                        new LambdaQueryWrapper<ProductSpec>()
                                .in(ProductSpec::getSpecId, new ArrayList<>(specSales.keySet())));
                for (ProductSpec spec : specs) {
                    productSales.merge(spec.getProductId(),
                            specSales.getOrDefault(spec.getSpecId(), 0), Integer::sum);
                }
            }
        }

        // 3. 按销量降序取前N个productId
        List<Integer> topIds = productSales.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 4. 不足时用最新上架商品补齐
        if (topIds.size() < limit) {
            LambdaQueryWrapper<Product> latestWrapper = new LambdaQueryWrapper<Product>()
                    .eq(Product::getIsOnSale, 1)
                    .orderByDesc(Product::getCreateTime)
                    .last("LIMIT " + limit);
            if (!topIds.isEmpty()) latestWrapper.notIn(Product::getProductId, topIds);
            List<Product> latest = productService.list(latestWrapper);
            for (Product p : latest) {
                if (topIds.size() >= limit) break;
                topIds.add(p.getProductId());
            }
        } else {
            topIds = topIds.subList(0, limit);
        }

        // 5. 构建返回结果
        List<ProductCategory> allCatsHot = productCategoryService.list();
        Map<Integer, ProductCategory> catMapHot = new HashMap<>();
        for (ProductCategory c : allCatsHot) catMapHot.put(c.getCategoryId(), c);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Integer pid : topIds) {
            Product p = productService.getById(pid);
            if (p == null) continue;

            Map<String, Object> row = new HashMap<>();
            row.put("productId", p.getProductId());
            row.put("productName", p.getProductName());
            row.put("brand", p.getBrand());
            row.put("model", p.getModel());
            row.put("isOnSale", p.getIsOnSale());
            row.put("salesCount", productSales.getOrDefault(pid, 0));

            ProductCategory cat = catMapHot.get(p.getCategoryId());
            row.put("categoryId", p.getCategoryId());
            row.put("categoryName", cat != null ? cat.getCategoryName() : null);
            if (cat != null && cat.getParentId() != null && cat.getParentId() != 0) {
                ProductCategory parentCat = catMapHot.get(cat.getParentId());
                row.put("parentCategoryId", cat.getParentId());
                row.put("parentCategoryName", parentCat != null ? parentCat.getCategoryName() : null);
            } else {
                row.put("parentCategoryId", null);
                row.put("parentCategoryName", null);
            }

            ProductImage mainImg = productImageService.getOne(
                    new LambdaQueryWrapper<ProductImage>()
                            .eq(ProductImage::getProductId, pid)
                            .eq(ProductImage::getImageSort, 1)
                            .eq(ProductImage::getIsDeleted, 0)
                            .last("LIMIT 1"));
            row.put("mainImage", mainImg != null ? mainImg.getImageUrl() : null);

            List<ProductSpec> specs = productSpecService.list(
                    new LambdaQueryWrapper<ProductSpec>()
                            .eq(ProductSpec::getProductId, pid)
                            .orderByAsc(ProductSpec::getSpecId));
            if (!specs.isEmpty()) {
                row.put("firstSpecId", specs.get(0).getSpecId());
                row.put("unit", specs.get(0).getUnit());
                BigDecimal minPrice = specs.stream().map(ProductSpec::getUnitPrice)
                        .filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(null);
                row.put("minPrice", minPrice);
                int totalStock = specs.stream()
                        .mapToInt(s -> s.getStockQuantity() != null ? s.getStockQuantity() : 0).sum();
                row.put("totalStock", totalStock);
            } else {
                row.put("firstSpecId", null);
                row.put("minPrice", null);
                row.put("totalStock", 0);
            }
            records.add(row);
        }
        return APIRes.ok("查询成功", records);
    }

    // ======================== 商品规格 ========================

    /** GET /api/v1/product/spec/list - 查询商品的规格列表 */
    @GetMapping("/spec/list")
    public APIRes specList(@RequestParam Integer productId) {
        List<ProductSpec> specs = productSpecService.list(
                new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, productId));
        return APIRes.ok("查询成功", specs);
    }

    /** POST /api/v1/product/spec/add - 新增规格（店主） */
    @PostMapping("/spec/add")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes specAdd(@RequestBody ProductSpec spec) {
        if (spec.getProductId() == null) return APIRes.fail(400, "商品ID不能为空");
        if (spec.getUnitPrice() == null) return APIRes.fail(400, "单价不能为空");
        if (spec.getWarningThreshold() == null) spec.setWarningThreshold(10);
        if (spec.getStockQuantity() == null) spec.setStockQuantity(0);
        // 自动生成规格编码
        if (StringUtils.isBlank(spec.getSpecCode())) {
            spec.setSpecCode(generateSpecCode());
        }
        spec.setUpdateTime(new Date());
        productSpecService.save(spec);
        return APIRes.ok("新增规格成功", spec);
    }

    /** PUT /api/v1/product/spec/update/{specId} - 修改规格（店主） */
    @PutMapping("/spec/update/{specId}")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes specUpdate(@PathVariable Integer specId, @RequestBody ProductSpec spec) {
        spec.setSpecId(specId);
        spec.setUpdateTime(new Date());
        productSpecService.updateById(spec);
        return APIRes.ok("修改成功");
    }

    // ======================== 价格策略 ========================

    /** GET /api/v1/product/price/strategy/list - 查询价格策略（店主） */
    @GetMapping("/price/strategy/list")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes priceStrategyList(@RequestParam(defaultValue = "1") Integer pageNum,
                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<PriceStrategy> page = priceStrategyService.page(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<PriceStrategy>().orderByDesc(PriceStrategy::getCreateTime));
        return APIRes.ok("查询成功", page);
    }

    /** POST /api/v1/product/price/strategy/add - 新增价格策略（店主） */
    @PostMapping("/price/strategy/add")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes priceStrategyAdd(@RequestBody PriceStrategy strategy) {
        if (StringUtils.isBlank(strategy.getStrategyName())) {
            return APIRes.fail(400, "策略名称不能为空");
        }
        strategy.setCreateTime(new Date());
        strategy.setUpdateTime(new Date());
        if (strategy.getStatus() == null) strategy.setStatus(1);
        priceStrategyService.save(strategy);
        return APIRes.ok("新增价格策略成功", strategy);
    }

    /** PUT /api/v1/product/price/strategy/status/{id} - 启用/禁用价格策略（店主） */
    @PutMapping("/price/strategy/status/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes priceStrategyStatus(@PathVariable Integer id, @RequestParam Integer status) {
        PriceStrategy ps = new PriceStrategy();
        ps.setStrategyId(id);
        ps.setStatus(status);
        ps.setUpdateTime(new Date());
        priceStrategyService.updateById(ps);
        return APIRes.ok(status == 1 ? "已启用" : "已禁用");
    }

    // ======================== 图片上传 ========================

    /** POST /api/v1/product/image/upload - 上传商品图片（店主/店员） */
    @PostMapping("/image/upload")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public APIRes imageUpload(@RequestParam Integer productId,
                              @RequestParam(defaultValue = "2") Integer imageSort,
                              @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return APIRes.fail(400, "文件不能为空");
        try {
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            File dest = new File(productImagePath + filename);
            dest.getParentFile().mkdirs();
            file.transferTo(dest);

            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setImageUrl("/images/product/" + filename);
            image.setImageSort(imageSort);
            image.setUploadTime(new Date());
            image.setIsDeleted(0);
            productImageService.save(image);
            return APIRes.ok("上传成功", image);
        } catch (Exception e) {
            return APIRes.fail(500, "上传失败：" + e.getMessage());
        }
    }

    /** DELETE /api/v1/product/image/{imageId} - 删除商品图片（店主） */
    @DeleteMapping("/image/{imageId}")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes imageDelete(@PathVariable Integer imageId) {
        ProductImage image = new ProductImage();
        image.setImageId(imageId);
        image.setIsDeleted(1);
        productImageService.updateById(image);
        return APIRes.ok("删除成功");
    }

    // ======================== 工具方法 ========================

    /** 生成规格编码（6位随机） */
    private String generateSpecCode() {
        return "SP-" + String.format("%06d", new Random().nextInt(999999));
    }

    /** 递归构建分类树（同级去重，防止数据库重复行） */
    private List<Map<String, Object>> buildCategoryTree(List<ProductCategory> all, int parentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (ProductCategory cat : all) {
            if (cat.getParentId() != null && cat.getParentId() == parentId
                    && seen.add(cat.getCategoryId())) {
                Map<String, Object> node = new HashMap<>();
                node.put("categoryId", cat.getCategoryId());
                node.put("categoryName", cat.getCategoryName());
                node.put("parentId", cat.getParentId());
                node.put("status", cat.getStatus());
                node.put("sortOrder", cat.getSortOrder());
                node.put("children", buildCategoryTree(all, cat.getCategoryId()));
                result.add(node);
            }
        }
        return result;
    }
}
