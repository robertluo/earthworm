# earthworm

earthworm - 蚯蚓

默默地在地下工作，给植物——整个服务器程序松土让它更容易破土而出。

用 stuartsierra 的 component 库所写成的可重用的基本服务集合。目前的内容有：

 * datomic — 提供数据库服务和基本的 util 函数

## Usage

每个 datomic 数据库的定义被称为 db-def，包括：结构定义 schema, 种子数据 seed 和开发时使用的样例数据 sample。因此，使用者首先要建立一个 db-def：

```clojure
(require '[earthworm.datomic.util :as du])

(def my-database (du/def-db schema seed sample))
```

在构造系统时：

```clojure
(require '[earthworm.datomic :as datomic])
(component/system-map :datomic (datomic/service {:uri "datomic:mem:test" :db-def my-database :sample? true})
```


## License

Copyright © 2015 @robortluo

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
