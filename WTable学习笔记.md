**1 介绍**

WTable是58架构平台部研发的kv、klist分布式存储系统，数据会存储在集群内部的各个节点上，具有高可用、高性能、存储容量大、自动扩容和容灾等特点。相对于其它存在方案有较大的优势，如下图所示。

![](https://ws1.sinaimg.cn/large/006tNc79gy1fz1lqlphu5j313809ctbe.jpg)

表的结构示例，如下图所示。

![](https://ws4.sinaimg.cn/large/006tNc79gy1fz1lsbrcncj31cs0g640t.jpg)

关于WTable表结构几个要点：

   1）与MySQL不同，WTable中的表没有schema，也不需要预先定义；

   2）每条数据都包含rowKey、colKey、value和score等4个属性，rowKey和colKey两个参数才能唯一的确认一条数据；
   
   3）任何类型的数据，只要能够序列化成字节流，就都可以作为WTable的rowKey、colKey和value；
   
   4）对于rowKey相同colKey不同的数据，默认空间和Z空间提供了不同的排序方式。在默认空间内，数据是按照colKey升序排序的，而Z空间提供了两种排序方式，分别为按colKey升序排列和按score+colKey升序排列；

-------
**2 原理**

原理没有搞得太明白，贴一张文档上的图。
![](https://ws3.sinaimg.cn/large/006tNc79gy1fz1ltzprsmj30w00eo78l.jpg)
直接COPY，解释一下：

**Name Center：**提供一种“类似域名解析”的服务。Name Center会实时从etcd同步所有小集群Proxy服务器地址信息，剔除有问题的Proxy，并提供接口根据bid拉取Proxy地址列表。

**etcd：**系统内部的配置中心，支持实时推送配置变更，高可用。

**Proxy：**代理服务，它知道每个rowKey应该存储在哪个Data Server Group，并进行请求路由。如果是MGET/MSET这类批量请求，它会将请求进行分包，并将结果进行汇总返回给客户端。Proxy本身无状态，每个Proxy提供的功能都是对等的，支持线性无限扩展。

**Data Server：**数据存储服务，它是整个系统的核心。一个Data Server Group里面会包含几台Data Server，一般是2至3台，其中一台是Master，其余的是Slave。Master的选举是自动执行的，不用人工干预。所有写操作都先在Master上执行，并实时同步给所有的Slave。读取操作可以在Master或者Slave上执行。Master和Slave数据同步的延迟非常低，因此数据不一致的概率很低，大部分业务从Slave读取数据都是安全的。如果对数据一致性有特殊需要，可以设置CAS请求参数，可以指定只从Master读取数据，或者通过Cas.LOCK参数来加乐观锁，具体请参考CAS相关说明。Data Server数据存储采用了LSM树(Log-Structured Merge Tree)存储引擎，所有数据实时落地到文件，并且避免了随机写，因此有非常好的写性能；数据读取在本机有block cache，采用LRU淘汰策略，因此读取性能非常棒，并且能保证永远不会出现cache数据不一致的问题。



----------
   
**3 例子**

本Demo使用Java Client，有一个官方的example。

    示例代码下载地址为http://wiki.58corp.com/index.php?title=文件:Wtable-example.zip
另外WTable还支持C++和PHP，详解文档。

    文档连接地址为http://wiki.58corp.com/index.php?title=WTable#Java.E5.AE.A2.E6.88.B7.E7.AB.AF
    
首先指正example里的一个小坑，wtable.properties中的信息有误，需要改成如下图。

![](https://ws1.sinaimg.cn/large/006tNc79gy1fz1luhci5uj30cm03aq3a.jpg)





代码跑一跑  ↓  ↓   ↓

1 example中有两个Class，Example和UserInfo。

  1) Example.java
  
  常见接口操作的示例：
  
  ![](https://ws3.sinaimg.cn/large/006tNc79gy1fz1lv4jjgxj30x002maai.jpg)
  
  ![](https://ws2.sinaimg.cn/large/006tNc79gy1fz1lwa2xcbj30wg02oq3f.jpg)
  
  ![](https://ws3.sinaimg.cn/large/006tNc79gy1fz1lwsoy89j30wg030aah.jpg)

  ![](https://ws2.sinaimg.cn/large/006tNc79gy1fz1lxumko4j30wc02o0tb.jpg)

  ![](https://ws4.sinaimg.cn/large/006tNc79gy1fz1lya0qepj30we04oab1.jpg)
  
  ![](https://ws4.sinaimg.cn/large/006tNc79gy1fz1lynzxb7j30wy07uq5f.jpg)

  ![](https://ws1.sinaimg.cn/large/006tNc79gy1fz1lz05s1jj30wc0323z1.jpg)
  
  ![](https://ws2.sinaimg.cn/large/006tNc79gy1fz1lzmt2paj30w2034aai.jpg)
  
**get/zGet：**在默认空间/Z空间中获取某个table中rowKey+colKey对应的数据，可以指定是否使用cas，关于cas的概念和详情参加下文中cas的部分。

**set/zSet：**在默认空间/Z空间中设置某个table中rowKey+colKey对应的数据，如果对应的数据已经存在，则新数据会覆盖旧数据，可以指定是否使用cas。

**setEx/zSetEx：**在默认空间/Z空间中设置某个table中rowKey+colKey对应的带有TTL（生存时长，单位秒）的数据，如果对应的数据已经存在，则新数据会覆盖旧数据，可以指定是否使用cas。

**expire/zExpire：**在默认空间/Z空间中修改某个table中rowKey+colKey对应的数据的TTL（生存时长，单位秒），TTL为0表示清除数据原有的TTL，可以指定是否使用cas。

**del/zDel**：在默认空间/Z空间中删除某个table中rowKey+colKey指定的数据，可以指定是否使用cas。

**incr/zIncr：**在默认空间/Z空间中增加某个table中rowKey+colKey指定的数据对应的score值，增幅可以为负值，从而实现减少score的效果，可以指定是否使用cas。

**mGet/zmGet：**get/zGet的批量接口，单次操作上限为100，如果部分key失败依然可以正常返回，可以通过getErrCode()来判断这个key是否成功。

**mSet/zmSet：**set/zSet的批量接口，单次操作上限为100，如果部分key失败依然可以正常返回，可以通过getErrCode()来判断这个key是否成功。

**mSetEx/zmSetEx：**setEx/zSetEx的批量接口，单次操作上限为100，如果部分key失败依然可以正常返回，可以通过getErrCode()来判断这个key是否成功。

**mDel/zmDel：**del/zDel的批量接口，单次操作上限为100，如果部分key失败依然可以正常返回，可以通过getErrCode()来判断这个key是否成功。

**mIncr/zmIncr：**incr/zIncr的批量接口，单次操作上限为100，如果部分key失败依然可以正常返回，可以通过getErrCode()来判断这个key是否成功。

**scan/zScan：**在默认空间/Z空间中扫描某个table中相同rowKey的不同colKey的数据，单次最多返回10000条数据，建议一次扫描不要超过1000条。

**scanPivot/zScanPivot：**与scan/zScan类似，但是从指定的colKey或者score+colKey进行扫描，而不是像scan/zScan一样从头开始扫描。

**scanMore：**与scan/zScan和scanPivot/zScanPivot配合使用，从上次扫描位置继续扫描。

**dumpDB：**开始dump全量数据库，只能返回初始的若干条数据，需要与dumpMore配合使用返回后续的数据。

**dumpTable：**开始dump数据库中的某个table，只能返回初始的若干条数据，需要与dumpMore配合使用返回后续的数据。

**dumpMore：**与dumpDB/dumpTable配合使用，从上次dump位置继续返回。


  2） UserInfo.Java
  
  
  静态内部类User
  
  ![](https://ws4.sinaimg.cn/large/006tNc79gy1fz1m0bx398j30io0l6djf.jpg)




    
    