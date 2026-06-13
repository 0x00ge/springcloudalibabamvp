package com.mvp.common.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mvp.common.enums.ResultCode;
import com.mvp.common.vo.ResultVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.ResolvableType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.Serializable;

/**
 * 通用 CRUD Controller。
 *
 * <p>子类只需要传实体和 dto 两个泛型，例如 {@code BaseController<E, D>}。
 * Controller 对外收发 dto，内部通过 {@code IService<E>} 操作实体。</p>
 *
 * <p>这个基类只负责最常见的单表接口：按 id 查询、分页查询、新增、按 id 修改、按 id 删除。
 * 业务接口、组合查询、批量操作等更具体的能力，建议放在具体子类 Controller 中实现。</p>
 *
 * @param <E> 实体类型
 * @param <D> dto 类型
 */
@Slf4j
public abstract class BaseController<E, D> {

    /**
     * 父类持有的通用 Service。
     *
     * <p>这里使用 MyBatis-Plus 的 {@link IService}，是因为它已经提供了 getById、page、
     * save、updateById、removeById 等通用 CRUD 方法。子类构造方法里调用
     * {@code super(userService)} 后，父类就可以通过这个对象完成通用接口。</p>
     */
    private final IService<E> baseService;

    /**
     * 接收子类传入的具体业务 Service。
     *
     * <p>例如 UserController 传入 UserService。UserService 本身继承了
     * {@code IService<User>}，所以既能被父类当作通用 CRUD Service 使用，也能被子类当作
     * 用户模块自己的业务 Service 使用。</p>
     */
    protected BaseController(IService<E> baseService) {
        this.baseService = baseService;
    }

    /**
     * 根据主键查询单条数据。
     *
     * <p>返回给前端的是 Dto，而不是 Entity。这样后续如果 Entity 增加内部字段，也可以通过
     * Dto 控制哪些字段对外暴露。</p>
     */
    @GetMapping("/{id}")
    public ResultVO<D> getById(@PathVariable String id) {
        log.info("{} 查询{}详情，id={}", controllerName(), entityName(), id);
        E entity = baseService.getById(id);
        if (entity == null) {
            log.warn("{} 查询{}详情未命中，id={}", controllerName(), entityName(), id);
            return ResultVO.build(ResultCode.NOT_FOUND);
        }
        log.debug("{} 查询{}详情成功，id={}", controllerName(), entityName(), id);
        return ResultVO.ok(entity2Dto(entity));
    }

    /**
     * 分页查询。
     *
     * <p>MyBatis-Plus 的 {@link Page} 保存当前页和每页数量，{@code baseService.page(query)}
     * 返回实体分页结果。这里再通过 {@code convert(this::entity2Dto)} 把每一条 Entity 转成 Dto，
     * 保持 Controller 的对外数据结构一致。</p>
     */
    @GetMapping("/page")
    public ResultVO<IPage<D>> page(@RequestParam(defaultValue = "1") Long page,
                                   @RequestParam(defaultValue = "10") Long size) {
        log.info("{} 分页查询{}，page={}, size={}", controllerName(), entityName(), page, size);
        Page<E> query = new Page<>(page, size);
        IPage<D> result = baseService.page(query).convert(this::entity2Dto);
        log.debug("{} 分页查询{}完成，page={}, size={}, total={}",
                controllerName(), entityName(), page, size, result.getTotal());
        return ResultVO.ok(result);
    }

    /**
     * 新增数据。
     *
     * <p>{@link Valid} 会触发 Dto 上的 Jakarta Validation 注解，例如 {@code @NotBlank}、
     * {@code @Size}、{@code @Pattern} 等。校验通过后再把 Dto 拷贝成 Entity，交给
     * MyBatis-Plus 保存。</p>
     *
     * <p>保存成功后返回实体主键。对于使用 {@code IdType.ASSIGN_UUID} 的实体，主键会在
     * {@code baseService.save(entity)} 过程中由 MyBatis-Plus 生成并回填到实体对象里。</p>
     */
    @PostMapping
    public ResultVO<Serializable> save(@Valid @RequestBody D dto) {
        log.info("{} 新增{}", controllerName(), entityName());
        E entity = dto2Entity(dto);
        baseService.save(entity);
        Serializable id = getIdValue(entity);
        log.info("{} 新增{}成功，id={}", controllerName(), entityName(), id);
        return ResultVO.ok(id);
    }

    /**
     * 根据路径 id 修改数据。
     *
     * <p>这里先查一次是否存在，是为了把“不存在”明确返回成 NOT_FOUND，而不是让
     * {@code updateById} 静默返回更新失败。路径里的 id 优先级最高，Dto 里即使带了 id，
     * 也会被 {@link #setIdValue(Object, Serializable)} 覆盖，避免前端传错 id 时更新到别的记录。</p>
     */
    @PutMapping("/{id}")
    public ResultVO<Void> update(@PathVariable String id, @Valid @RequestBody D dto) {
        log.info("{} 修改{}，id={}", controllerName(), entityName(), id);
        E exist = baseService.getById(id);
        if (exist == null) {
            log.warn("{} 修改{}失败，数据不存在，id={}", controllerName(), entityName(), id);
            return ResultVO.build(ResultCode.NOT_FOUND);
        }

        E entity = dto2Entity(dto);
        setIdValue(entity, id);
        baseService.updateById(entity);
        log.info("{} 修改{}成功，id={}", controllerName(), entityName(), id);
        return ResultVO.ok();
    }

    /**
     * 根据主键删除数据。
     *
     * <p>是否真正物理删除，取决于实体和 MyBatis-Plus 是否配置了逻辑删除字段。当前基类只调用
     * {@code removeById}，不在 Controller 层关心具体删除策略。</p>
     */
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable String id) {
        log.info("{} 删除{}，id={}", controllerName(), entityName(), id);
        E exist = baseService.getById(id);
        if (exist == null) {
            log.warn("{} 删除{}失败，数据不存在，id={}", controllerName(), entityName(), id);
            return ResultVO.build(ResultCode.NOT_FOUND);
        }

        baseService.removeById(id);
        log.info("{} 删除{}成功，id={}", controllerName(), entityName(), id);
        return ResultVO.ok();
    }

    /**
     * Entity 转 Dto，子类可以覆盖以处理复杂字段。
     *
     * <p>默认实现使用 Spring 的 {@link BeanUtils#copyProperties(Object, Object)}，
     * 只按同名属性做浅拷贝。字段名不同、枚举翻译、嵌套对象、脱敏等场景，可以在子类重写该方法。</p>
     */
    protected D entity2Dto(E entity) {
        D dto = newDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    /**
     * Dto 转 Entity，子类可以覆盖以处理复杂字段。
     *
     * <p>默认实现同样只做同名属性浅拷贝。适合字段结构基本一致的简单单表 CRUD；
     * 如果 Dto 中包含前端专用字段，或者 Entity 中有数据库自动维护字段，子类可以重写并定制拷贝逻辑。</p>
     */
    protected E dto2Entity(D dto) {
        E entity = newEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    /**
     * 创建实体对象。
     *
     * <p>{@code baseService.getEntityClass()} 由 MyBatis-Plus 根据具体 Service 解析实体类型。
     * 实体需要有无参构造方法；使用 Lombok {@code @Data} 不会破坏默认无参构造。</p>
     */
    private E newEntity() {
        return BeanUtils.instantiateClass(baseService.getEntityClass());
    }

    /**
     * 创建 Dto 对象。
     *
     * <p>Dto 类型不在 Service 中，所以需要从子类继承的泛型中解析，例如
     * {@code UserController extends BaseController<User, UserDto>} 的第二个泛型就是
     * {@code UserDto}。</p>
     */
    private D newDto() {
        return BeanUtils.instantiateClass(getDtoClass());
    }

    /**
     * 解析当前子类声明的 Dto 泛型类型。
     *
     * <p>{@link ResolvableType} 是 Spring 提供的泛型解析工具。这里固定读取
     * {@code BaseController<E, D>} 的第二个泛型参数，也就是 Dto 类型。</p>
     */
    @SuppressWarnings("unchecked")
    private Class<D> getDtoClass() {
        Class<?> dtoClass = ResolvableType.forClass(getClass())
                .as(BaseController.class)
                .getGeneric(1)
                .resolve();
        if (dtoClass == null) {
            throw new IllegalStateException("无法解析 BaseController 的 Dto 泛型");
        }
        return (Class<D>) dtoClass;
    }

    /**
     * 从保存后的实体中读取主键值。
     *
     * <p>MyBatis-Plus 会把表结构和主键信息缓存成 {@link TableInfo}。
     * 通过 {@code keyProperty} 可以知道实体里的主键属性名，再从实体对象上取出实际值。</p>
     */
    private Serializable getIdValue(E entity) {
        TableInfo tableInfo = getTableInfo();
        if (tableInfo == null || tableInfo.getKeyProperty() == null) {
            return null;
        }
        Object value = tableInfo.getPropertyValue(entity, tableInfo.getKeyProperty());
        if (value == null) {
            return null;
        }
        if (value instanceof Serializable serializable) {
            return serializable;
        }
        throw new IllegalStateException("实体主键没有实现 Serializable: " + value.getClass().getName());
    }

    /**
     * 把路径中的 id 写回到待更新实体。
     *
     * <p>{@code updateById} 依赖实体对象上的主键字段定位记录，所以 Dto 转 Entity 后必须确保
     * Entity 的主键有值。这里统一使用 URL 路径中的 id，避免请求体和路径 id 不一致。</p>
     */
    private void setIdValue(E entity, Serializable id) {
        TableInfo tableInfo = getTableInfo();
        if (tableInfo != null && tableInfo.getKeyProperty() != null) {
            tableInfo.setPropertyValue(entity, tableInfo.getKeyProperty(), id);
        }
    }

    /**
     * 获取当前实体对应的 MyBatis-Plus 表信息。
     *
     * <p>表名、主键字段、实体属性映射等元数据都在 {@link TableInfo} 里，主键读取和写入都会用到它。</p>
     */
    private TableInfo getTableInfo() {
        return TableInfoHelper.getTableInfo(baseService.getEntityClass());
    }

    private String controllerName() {
        return getClass().getSimpleName();
    }

    private String entityName() {
        return baseService.getEntityClass().getSimpleName();
    }
}
