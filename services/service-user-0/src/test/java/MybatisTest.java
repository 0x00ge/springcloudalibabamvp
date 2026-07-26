import com.mvp.user.UserApplication;
import com.mvp.user.entity.User;
import com.mvp.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

/**
 * MyBatis 动态 SQL / 映射示例测试。
 *
 * <p>依赖本地 MySQL（master: 3306，库名 mvp）。</p>
 *
 * @Author zt
 */
@SpringBootTest(classes = UserApplication.class)
public class MybatisTest {

    @Autowired
    private UserMapper userMapper;

    /**
     * resultType + include Base_Column_List
     */
    @Test
    public void userList() {
        List<User> users = userMapper.userList();
        System.out.println("userList 用户总数: " + users.size());
        for (User user : users) {
            System.out.println(user);
        }
    }

    /**
     * resultMap + include Base_Column_List
     */
    @Test
    public void userMapList() {
        List<User> users = userMapper.userMapList();
        System.out.println("userMapList 用户总数: " + users.size());
        for (User user : users) {
            System.out.println(user);
        }
    }

    /**
     * if + where：可选条件
     */
    @Test
    public void findUsers() {
        // 全条件为空 → 等价全表
        List<User> all = userMapper.findUsers(null, null, null);
        System.out.println("findUsers(全空) 总数: " + all.size());

        // 按 status
        List<User> byStatus = userMapper.findUsers(null, null, 1);
        System.out.println("findUsers(status=1) 总数: " + byStatus.size());
        for (User user : byStatus) {
            System.out.println(user);
        }

        // 按 name 模糊
        List<User> byName = userMapper.findUsers(null, "张", null);
        System.out.println("findUsers(name like 张) 总数: " + byName.size());
        for (User user : byName) {
            System.out.println(user);
        }
    }

    /**
     * choose / when / otherwise：按 type 分支
     */
    @Test
    public void findByKeyword() {
        List<User> byPhone = userMapper.findByKeyword("13800000000", null, null);
        System.out.println("findByKeyword(phone) 总数: " + byPhone.size());
        for (User user : byPhone) {
            System.out.println(user);
        }

        List<User> byName = userMapper.findByKeyword(null, "张三", null);
        System.out.println("findByKeyword(name) 总数: " + byName.size());
        for (User user : byName) {
            System.out.println(user);
        }

        // otherwise → 1=1，相当于全表
        List<User> otherwise = userMapper.findByKeyword(null, null, null);
        System.out.println("findByKeyword(otherwise) 总数: " + otherwise.size());
    }

    /**
     * set + if：动态更新（只更新非 null 字段）
     *
     * <p>先查一条再改 name，避免写死不存在的 id。</p>
     */
    @Test
    public void updateUser() {
        List<User> users = userMapper.userList();
        if (users.isEmpty()) {
            System.out.println("updateUser: 无用户数据，跳过");
            return;
        }

        User first = users.get(0);
        User patch = new User();
        patch.setId(first.getId());
        patch.setName(first.getName() == null ? "测试用户" : first.getName());
        // phone / status 保持 null，不会被 set 进 SQL

        int rows = userMapper.updateUser(patch);
        System.out.println("updateUser 影响行数: " + rows + ", id=" + first.getId());
    }

    /**
     * foreach：IN 查询
     */
    @Test
    public void listByIds() {
        List<User> users = userMapper.userList();
        if (users.isEmpty()) {
            System.out.println("listByIds: 无用户数据，跳过");
            return;
        }

        // 取前 2 个 id（不足 2 个就全取）
        List<String> ids = users.stream()
                .limit(2)
                .map(User::getId)
                .toList();
        // 再拼一个不存在的 id，验证 IN 语法
        ids = Arrays.asList(ids.get(0),
                ids.size() > 1 ? ids.get(1) : ids.get(0),
                "00000000000000000000000000000000");

        List<User> result = userMapper.listByIds(ids);
        System.out.println("listByIds 入参: " + ids);
        System.out.println("listByIds 命中: " + result.size());
        for (User user : result) {
            System.out.println(user);
        }
    }

    /**
     * bind：姓名模糊查询
     */
    @Test
    public void searchByName() {
        List<User> users = userMapper.searchByName("张");
        System.out.println("searchByName(张) 总数: " + users.size());
        for (User user : users) {
            System.out.println(user);
        }
    }

    /**
     * 综合：where + if + order by ${} + limit
     *
     * <p>orderBy 必须是白名单列名，这里写死 created_at desc。</p>
     */
    @Test
    public void userListPage() {
        List<User> page = userMapper.userListPage(1, "created_at desc", 0, 10);
        System.out.println("userListPage(status=1, offset=0, limit=10) 总数: " + page.size());
        for (User user : page) {
            System.out.println(user);
        }
    }
}
