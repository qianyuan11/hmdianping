package com.hmdp.dto;

import lombok.Data;

@Data
public class UserDTO {
    /*DTO就是数据传输对象(Data Transfer Object)的缩写。 DTO模式，是指将数据封装成普通的JavaBeans，在J2EE多个层次之间传输。   DTO类似信使，是同步系统中的Message。  该JavaBeans可以是一个数据模型Model。*/
    private Long id;
    private String nickName;
    private String icon;
}
