# 分布式id生成器<br>

## 简介
这是一个spring-boot-starter,如果你还不了解spring-boot以及它的starter如何使用，请先学习spring-boot。

基于snowflake（雪花算法）id生成方案（需要spring-data-redis支持）

## 使用

- step1：添加依赖
```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <dependency>
        <groupId>cn.ocoop.framework</groupId>
        <artifactId>id-spring-boot-starter</artifactId>
        <version>1.1.0</version>
    </dependency>
```

- step2：配置yml
```yml
spring:
  redis:
    url: redis://localhost:6379 #redis配置
id:
  key: 'worker_id_sequence'     #该id服务所属的集群key，将所有生成不重复id的key设置为相同               
```

- step3：java
```java
        long id = Id.next();        
```
