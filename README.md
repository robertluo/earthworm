# earthworm

earthworm - 蚯蚓

默默地在地下工作，给植物——整个服务器程序松土让它更容易破土而出。

它包括一些经常可以重用的工具函数：在 `earthworm.util` 包内。

 * `(:require [earthworm.util.datomic])` 提供 datamic 数据库的常用工具函数
 * `(:require [earthworm.util.bean])` 提供 java-bean 与 clojure map 互转换的函数


以及用 stuartsierra 的 component 库所写成的可重用的基本服务集合，在 `earthworm.service` 包内。目前的内容有：

 * `earthworm.service.datomic` — 提供 datomic 数据库服务
 * `earthworm.service.http-kit` - 提供 http-kit 的 web 服务器

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


## 版本

### 0.1.0

包括 datomic 服务与 http-kit web 服务、datomic 工具函数集

### 0.2.0

重新组织命名空间。并加上 util 的单元测试。

### 0.2.1

增加了几个新的数据库内置函数。

## License

Copyright © 2015 @robortluo

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
