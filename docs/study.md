# Java 学习笔记

## final 和 static final 的区别

### final

`final` 表示变量只能赋值一次。

```java
final int age = 18;
// age = 20; // 不允许再次赋值
```

如果 `final` 修饰的是对象引用，不能改变的是引用地址，不代表对象内容完全不能变。

```java
final List<String> names = new ArrayList<>();
names.add("Tom"); // 可以修改对象内容

// names = new ArrayList<>(); // 不允许重新指向另一个对象
```

在成员变量上使用 `final` 时，每个对象都有自己的一份值。

```java
class User {
    private final Long id;

    public User(Long id) {
        this.id = id;
    }
}
```

这里的 `id` 属于具体的 `User` 对象，不同对象可以有不同的 `id`，但创建后不能再改。

### static final

`static final` 表示这个变量属于类本身，并且只能赋值一次，通常用来定义常量。

```java
public class RedisConstant {
    public static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";
}
```

它的特点是：

| 特点 | 说明 |
| --- | --- |
| `static` | 属于类，不属于某个对象 |
| `final` | 只能赋值一次 |
| `static final` | 类级别常量，所有对象共享同一份 |

使用时通常通过类名访问：

```java
String key = RedisConstant.BLACKLIST_KEY_PREFIX + jti;
```

### 对比

| 写法 | 所属范围 | 是否共享 | 是否能再次赋值 | 常见用途 |
| --- | --- | --- | --- | --- |
| `final` | 对象实例 | 不共享，每个对象一份 | 不能 | 构造后不希望改变的成员变量 |
| `static final` | 类 | 共享，全类一份 | 不能 | 常量 |

### 示例

```java
public class User {
    private final Long id;
    public static final String TYPE = "USER";

    public User(Long id) {
        this.id = id;
    }
}
```

说明：

- `id` 是 `final`，每个 `User` 对象都有自己的 `id`。
- `TYPE` 是 `static final`，属于 `User` 类本身，所有 `User` 对象共享。

### 注意

`static final` 修饰可变对象时，引用不能变，但对象内容仍然可能被修改。

```java
public static final List<String> NAMES = new ArrayList<>();

NAMES.add("Tom"); // 可以
// NAMES = new ArrayList<>(); // 不可以
```

如果希望集合内容也不可变，可以使用不可变集合。

```java
public static final List<String> NAMES = List.of("Tom", "Jerry");
```

一句话总结：

```text
final：这个变量只能赋值一次。
static final：这个类级别变量只能赋值一次，通常用来定义常量。
```

## 构造器注入和注解注入的区别

在 Spring 中，常见的依赖注入方式有两种：

```text
构造器注入：通过构造方法传入依赖。
注解注入：通常指在字段上使用 @Autowired 注入依赖。
```

### 注解注入

注解注入通常写法如下：

```java
@Service
public class UserService {

    @Autowired
    private OrderService orderService;
}
```

优点：

- 写法简单。
- 代码看起来少。
- 适合快速 demo。

缺点：

- 依赖关系不明显，不看字段不容易知道这个类依赖谁。
- 不能配合 `final` 使用。
- 单元测试不方便，手动 `new UserService()` 时 `orderService` 是 `null`。
- 容易隐藏循环依赖问题。

### 构造器注入

构造器注入写法如下：

```java
@Service
public class UserService {

    private final OrderService orderService;

    public UserService(OrderService orderService) {
        this.orderService = orderService;
    }
}
```

如果一个类只有一个构造方法，Spring 可以自动识别，不需要额外写 `@Autowired`。

优点：

- 依赖关系清晰，看构造方法就知道这个类需要什么。
- 可以配合 `final` 使用，依赖创建后不能被改掉。
- 单元测试方便，可以直接传入 mock 对象。
- 必需依赖在对象创建时就必须提供，更安全。
- 循环依赖更容易提前暴露。

缺点：

- 代码比字段注入稍微多一点。
- 依赖太多时，构造方法会变长，也说明这个类可能职责太重。

### 对比

| 注入方式 | 写法 | 是否推荐 | 适合场景 |
| --- | --- | --- | --- |
| 注解注入 | 字段上写 `@Autowired` | 不推荐长期使用 | 简单 demo、临时代码 |
| 构造器注入 | 通过构造方法传入依赖 | 推荐 | 业务代码、核心代码 |

### 示例对比

不推荐：

```java
@Service
public class AuthService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
}
```

推荐：

```java
@Service
public class AuthService {

    private final RedisTemplate<String, String> redisTemplate;

    public AuthService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
}
```

一句话总结：

```text
注解注入：写起来省事，但依赖藏起来了。
构造器注入：写起来规矩，但更清晰、更安全、更好测试。
```
