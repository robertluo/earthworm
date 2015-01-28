# earthworm

earthworm - 蚯蚓

默默地在地下工作，给植物——整个服务器程序松土让它更容易破土而出。

用 stuartsierra 的 component 库所写成的可重用的基本服务集合。目前的内容有：

 * `earthworm.datomic` — 提供数据库服务和基本的 util 函数
 * `earthworm.http-kit` - 提供 http-kit 的 web 服务器

以上每个命名空间都有一个 `service` 函数，接受一个 map 作为参数。

## Usage

### Datomic 服务

每个 datomic 数据库的定义被称为 db-def，包括：结构定义 schema, 种子数据 seed 和开发时使用的样例数据 sample。因此，使用者首先要建立一个 db-def：

```clojure
(require '[earthworm.datomic.util :as du])

(def my-database (du/def-db schema seed sample))
```

在构造系统时：

```clojure
(require '[earthworm.datomic :as datomic])
(component/system-map :datomic (datomic/service {:uri "datomic:mem:test"
                                                 :db-def my-database :sample? true})
```

### Http-kit 服务

```clojure
(require '[earthworm.http-kit :as http])
(component/system-map :http (component/using (http/service {:port 8080}) [web-app]))
```

Http 服务依赖于一个 web-app，web-app 是一个含有 :handler 字段的 component 组件。

## License

Copyright © 2015 @robortluo

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
